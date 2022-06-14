import express from "express";
import {
  conceptController,
  conceptsController,
  errorHandler,
} from "./controllers";
import { Config } from "../config";
import { Clients } from "./types";

const createApp = (clients: Clients, config: Config) => {
  const app = express();

  app.get("/concepts", conceptsController(clients, config));
  app.get("/concepts/:id", conceptController(clients, config));

  app.use(errorHandler);

  return app;
};

export default createApp;
