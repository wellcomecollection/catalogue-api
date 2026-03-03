import {
  Client,
  ClientOptions,
  errors as elasticErrors,
} from "@elastic/elasticsearch";
import { getSecret } from "./aws";
import log from "./logging";

type ClientParameters = {
  pipelineDate: string;
};

const isProduction = process.env.NODE_ENV === "production";

const getElasticClientConfig = async ({
  pipelineDate,
}: ClientParameters): Promise<ClientOptions> => {
  const secretPrefix = `elasticsearch/pipeline_storage_${pipelineDate}`;
  const [host, apiKey] = await Promise.all([
    getSecret(`${secretPrefix}/${isProduction ? "private" : "public"}_host`),
    getSecret(`${secretPrefix}/concepts_api/api_key`),
  ]);

  if (apiKey == null || host == null) {
    throw Error("Could not retrieve values from Secrets Manager.");
  }

  return {
    node: `https://${host}:9243`,
    auth: {
      apiKey: apiKey,
    },
  };
};

const isAuthError = (error: unknown): boolean => {
  if (error instanceof elasticErrors.ResponseError) {
    return error.statusCode === 401 || error.statusCode === 403;
  }
  return false;
};

export class ResilientElasticClient {
  private client: Client;
  private readonly params: ClientParameters;
  private readonly MAX_RETRIES = 3;

  public constructor(client: Client, params: ClientParameters) {
    this.client = client;
    this.params = params;
  }

  static async create(
    params: ClientParameters
  ): Promise<ResilientElasticClient> {
    const config = await getElasticClientConfig(params);
    const client = new Client(config);
    return new ResilientElasticClient(client, params);
  }

  private async refreshClient(): Promise<void> {
    log.info("Refreshing Elasticsearch client due to auth error");
    const config = await getElasticClientConfig(this.params);
    this.client = new Client(config);
  }

  async execute<T>(operation: (client: Client) => Promise<T>): Promise<T> {
    for (let attempt = 0; attempt < this.MAX_RETRIES; attempt++) {
      try {
        return await operation(this.client);
      } catch (error) {
        if (isAuthError(error) && attempt < this.MAX_RETRIES - 1) {
          log.warn(
            `Elasticsearch auth error on attempt ${attempt + 1}/${
              this.MAX_RETRIES
            }, refreshing client...`
          );
          await this.refreshClient();
          continue;
        }
        throw error;
      }
    }
    throw new Error("Unexpected: max retries exceeded");
  }
}
