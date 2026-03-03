// This must be the first import in the app!
import "./src/services/init-apm";

import createApp from "./src/app";
import { ResilientElasticClient } from "./src/services/elasticsearch";
import { getConfig } from "./config";
import log from "./src/services/logging";

const config = getConfig();

ResilientElasticClient.create({ pipelineDate: config.pipelineDate }).then(
  (elastic) => {
    const app = createApp({ elastic }, config);
    const port = process.env.PORT ?? 3000;
    app.listen(port, () => {
      log.info(`Concepts API listening on port ${port}`);
    });
  }
);
