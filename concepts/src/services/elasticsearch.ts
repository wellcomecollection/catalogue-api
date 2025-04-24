import { Client, ClientOptions } from "@elastic/elasticsearch";
import { getSecret } from "./aws";

type ClientParameters = {
  pipelineDate: string;
};

const isProduction = process.env.NODE_ENV === "production";

const getElasticClientConfig = async ({
  pipelineDate,
}: ClientParameters): Promise<ClientOptions> => {
  // TODO: The 'concepts_api_new' key is temporary. Switch back to 'concepts_api' once it has been updated with the
  // necessary permissions.
  const secretPrefix = `elasticsearch/pipeline_storage_${pipelineDate}`;
  const [host, apiKey] = await Promise.all([
    getSecret(`${secretPrefix}/${isProduction ? "private" : "public"}_host`),
    getSecret(`${secretPrefix}/concepts_api_new/api_key`),
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

export const getElasticClient = async (
  params: ClientParameters
): Promise<Client> => {
  const config = await getElasticClientConfig(params);
  return new Client(config);
};
