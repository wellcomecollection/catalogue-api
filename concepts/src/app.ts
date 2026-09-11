import express from "express";
import morgan from "morgan";
import { logStream } from "./services/logging";
import {
  conceptController,
  conceptsController,
  errorHandler,
  healthcheckController,
  manifestController,
} from "./controllers";
import { Config } from "../config";
import { Clients } from "./types";

const createApp = (clients: Clients, config: Config) => {
  const app = express();

  app.use(morgan("short", { stream: logStream("http") }));

  app.get("/concepts", conceptsController(clients, config));
  app.get("/concepts/:id", conceptController(clients, config));
  app.get("/management/healthcheck", healthcheckController(config));
  app.get("/management/manifest", manifestController());

  app.use(errorHandler);

  return app;
};

export default createApp;
