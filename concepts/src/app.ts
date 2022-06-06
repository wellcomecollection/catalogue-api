import express from "express";
import type { Client as ElasticClient } from "@elastic/elasticsearch";
import {
  conceptController,
  conceptsController,
  errorHandler,
} from "./controllers";

type Clients = {
  elastic: ElasticClient;
};

type Context = {
  pipelineDate: string;
};

const createApp = (clients: Clients, context: Context) => {
  const app = express();

  const elasticIndex = `works-indexed-${context.pipelineDate}`;
  app.get(
    "/concepts",
    conceptsController({
      elasticClient: clients.elastic,
      index: elasticIndex,
    })
  );

  app.get(
    "/concepts/:id",
    conceptController({
      elasticClient: clients.elastic,
      index: elasticIndex,
    })
  );

  app.use(errorHandler);

  return app;
};

export default createApp;
