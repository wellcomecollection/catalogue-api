import { Client, ClientOptions } from "@elastic/elasticsearch";
import { getSecret } from "./aws";
import {
  ResilientElasticClient,
  createResilientClient,
} from "./resilient-elastic-client";

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
    getSecret(`${secretPrefix}/catalogue_api/api_key`),
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

/**
 * Create a basic Elasticsearch client (for testing or simple use cases).
 */
export const getElasticClient = async (
  params: ClientParameters
): Promise<Client> => {
  const config = await getElasticClientConfig(params);
  return new Client(config);
};

/**
 * Create a resilient Elasticsearch client that automatically refreshes
 * credentials on 401/403 errors and retries the failed request.
 */
export const getResilientElasticClient = async (
  params: ClientParameters
): Promise<ResilientElasticClient> => {
  const clientFactory = async () => {
    const config = await getElasticClientConfig(params);
    return new Client(config);
  };
  return createResilientClient(clientFactory);
};

export { ResilientElasticClient };
