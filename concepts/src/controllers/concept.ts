import { errors as elasticErrors } from "@elastic/elasticsearch";
import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Clients, Concept, Displayable } from "../types";
import { HttpError } from "./error";
import { Config } from "../../config";

type PathParams = { id: string };

type ConceptHandler = RequestHandler<PathParams, Concept>;

const conceptController = (
  clients: Clients,
  config: Config
): ConceptHandler => {
  const index = config.conceptsIndex;
  const elasticClient = clients.elastic;

  return asyncHandler(async (req, res) => {
    const id = req.params.id;
    try {
      const getResponse = await elasticClient.get<Displayable<Concept>>({
        index,
        id,
        _source: ["display"],
      });

      res.status(200).json(getResponse._source!.display);
    } catch (error) {
      if (error instanceof elasticErrors.ResponseError) {
        if (error.statusCode === 404) {
          throw new HttpError({
            status: 404,
            label: "Not Found",
            description: `Concept not found for identifier ${id}`,
          });
        }
      }
      throw error;
    }
  });
};

export default conceptController;
