import { Client, ClientOptions } from "@elastic/elasticsearch";
import { getSecret } from "./aws";

type ClientParameters = {
  pipelineDate: string;
  serviceName: string;
};

const getElasticClientConfig = async ({
  pipelineDate,
  serviceName,
}: ClientParameters): Promise<ClientOptions> => {
  const secretPrefix = `elasticsearch/pipeline_storage_${pipelineDate}`;
  const [host, port, protocol, username, password] = await Promise.all([
    getSecret(`${secretPrefix}/private_host`),
    getSecret(`${secretPrefix}/port`),
    getSecret(`${secretPrefix}/protocol`),
    getSecret(`${secretPrefix}/${serviceName}/es_username`),
    getSecret(`${secretPrefix}/${serviceName}/es_password`),
  ]);
  return {
    node: `${protocol}://${host}:${port}`,
    auth: {
      username: username!,
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
