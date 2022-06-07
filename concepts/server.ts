import createApp from "./src/app";
import { getElasticClient } from "./src/services/elasticsearch";

const pipelineDate = "2022-05-30";

getElasticClient({
  serviceName: "catalogue_api", // TODO an elasticsearch user for the concepts API
  pipelineDate,
}).then((elastic) => {
  const app = createApp({ elastic }, { pipelineDate });
  const port = process.env.PORT ?? 3000;
  app.listen(port, () => {
    console.log(`Concepts API listening on port ${port}`);
  });
});
