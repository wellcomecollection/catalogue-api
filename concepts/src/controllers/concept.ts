import { errors as elasticErrors } from "@elastic/elasticsearch";
import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Clients, Concept, Displayable } from "../types";
import { HttpError } from "./error";
import { clusterGetter } from "./cluster";
import { Config } from "../../config";

type PathParams = { id: string };

type QueryParams = { elasticCluster?: string };

type ConceptHandler = RequestHandler<PathParams, Concept, never, QueryParams>;

const conceptController = (
  clients: Clients,
  config: Config
): ConceptHandler => {
  const getCluster = clusterGetter(clients, config);

  return asyncHandler(async (req, res) => {
    const { elastic, index } = getCluster(req.query.elasticCluster);
    const id = req.params.id;
    try {
      const getResponse = await elastic.execute((client) =>
        client.get<Displayable<Concept>>({
          index,
          id,
          _source: ["display"],
        })
      );

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
