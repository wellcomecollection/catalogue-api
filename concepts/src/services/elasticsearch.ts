import { Client, ClientOptions } from "@elastic/elasticsearch";
import { getSecret } from "./aws";

type ClientParameters = {
  pipelineDate: string;
};

const isProduction = process.env.NODE_ENV === "production";

const getElasticClientConfig = async ({
  pipelineDate,
}: ClientParameters): Promise<ClientOptions> => {
  const secretPrefix = `elasticsearch/concepts-${pipelineDate}`;
  const [host, password] = await Promise.all([
    getSecret(`${secretPrefix}/${isProduction ? "private" : "public"}_host`),
    getSecret(`${secretPrefix}/api/password`),
  ]);
  return {
    node: `https://${host}:9243`,
    auth: {
      username: "api",
      password: password!,
    },
  };
};

export const getElasticClient = async (
  params: ClientParameters
): Promise<Client> => {
  const config = await getElasticClientConfig(params);
  return new Client(config);
};
