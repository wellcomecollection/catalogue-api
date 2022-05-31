import createApp from "./src/app";
import { getElasticClient } from "./src/services/elasticsearch";

const pipelineDate = "2022-05-30";

getElasticClient({
  serviceName: "concepts_api",
  pipelineDate,
}).then((elastic) => {
  const app = createApp({ elastic }, { pipelineDate });
  const port = process.env.PORT ?? 3000;
  app.listen(port, () => {
    console.log(`Concepts API listening on port ${port}`);
  });
});
