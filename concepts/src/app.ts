import express from "express";
import type { Client as ElasticClient } from "@elastic/elasticsearch";
import { conceptController, errorHandler } from "./controllers";

type Clients = {
  elastic: ElasticClient;
};

type Context = {
  pipelineDate: string;
};

const createApp = (clients: Clients, context: Context) => {
  const app = express();

  const elasticIndex = `works-indexed-${context.pipelineDate}`;
  app.get("/concepts", (req, res) => {
    res.status(200).json({
      type: "ResultList",
      results: Object.values(concepts),
    });
  });

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

const concepts: Record<string, any> = {
  azxzhnuh: {
    id: "azxzhnuh",
    identifiers: [
      {
        identifierType: "lc-names",
        value: "n12345678",
        type: "Identifier",
      },
    ],
    label: "Florence Nightingale",
    alternativeLabels: ["The Lady with the Lamp"],
    type: "Person",
  },
};

export default createApp;
