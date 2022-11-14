// This must be the first import in the app!
import "./src/services/init-apm";

import createApp from "./src/app";
import { getElasticClient } from "./src/services/elasticsearch";
import { getConfig } from "./config";

const config = getConfig();

getElasticClient({ pipelineDate: config.pipelineDate }).then((elastic) => {
  const app = createApp({ elastic }, config);
  const port = process.env.PORT ?? 3000;
  app.listen(port, () => {
    console.log(`Concepts API listening on port ${port}`);
  });
});
