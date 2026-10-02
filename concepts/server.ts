// This must be the first import in the app!
import "./src/services/init-apm";

import createApp from "./src/app";
import { ResilientElasticClient } from "./src/services/elasticsearch";
import { getConfig } from "./config";
import log from "./src/services/logging";

const config = getConfig();

// Like the search API, an additional cluster that fails to build is dropped (so 404s) rather than stopping startup
const createAdditionalClients = async (): Promise<
  Record<string, ResilientElasticClient>
> => {
  const entries = await Promise.all(
    Object.entries(config.additionalClusters).map(
      async ([name, { pipelineDate }]) => {
        try {
          const client = await ResilientElasticClient.create({ pipelineDate });
          return [[name, client] as const];
        } catch (error) {
          log.error(`Dropping Elasticsearch cluster ${name}: ${error}`);
          return [];
        }
      }
    )
  );
  return Object.fromEntries(entries.flat());
};

Promise.all([
  ResilientElasticClient.create({ pipelineDate: config.pipelineDate }),
  createAdditionalClients(),
]).then(([elastic, additionalElastic]) => {
  const app = createApp({ elastic, additionalElastic }, config);
  const port = process.env.PORT ?? 3000;
  app.listen(port, () => {
    log.info(`Concepts API listening on port ${port}`);
  });
});
