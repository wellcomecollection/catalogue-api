import createApp from "./src/app";
import { getElasticClient } from "./src/services/elasticsearch";

getElasticClient({
  serviceName: "concepts_api",
  pipelineDate: "2022-05-30",
}).then((elastic) => {
  const app = createApp({ elastic });
  const port = process.env.PORT ?? 3000;
  app.listen(port, () => {
    console.log(`Concepts API listening on port ${port}`);
  });
});
