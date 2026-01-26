import express from "express";
import morgan from "morgan";
import { logStream } from "./services/logging";
import {
  errorHandler,
  healthcheckController,
  worksController,
  workController,
  imagesController,
  imageController,
  clusterHealthController,
  workTypesTallyController,
  searchTemplatesController,
  elasticConfigController,
} from "./controllers";
import { Config } from "../config";
import { Clients } from "./types";

const createApp = (clients: Clients, config: Config) => {
  const app = express();

  // Logging middleware
  app.use(morgan("short", { stream: logStream("http") }));

  // Works routes
  app.get("/works", worksController(clients, config));
  app.get("/works/:id", workController(clients, config));

  // Images routes
  app.get("/images", imagesController(clients, config));
  app.get("/images/:id", imageController(clients, config));

  // Management routes
  app.get("/management/healthcheck", healthcheckController(config));
  app.get("/management/clusterhealth", clusterHealthController(clients));
  app.get("/management/_workTypes", workTypesTallyController(clients, config));
  app.get("/_searchTemplates", searchTemplatesController(config));
  app.get("/_elasticConfig", elasticConfigController(config));

  // Error handler
  app.use(errorHandler);

  return app;
};

export default createApp;
