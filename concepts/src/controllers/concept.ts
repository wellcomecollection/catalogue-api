import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Clients, Concept } from "../types";
import { HttpError } from "./error";
import { Config } from "../../config";

type PathParams = { id: string };

type ConceptHandler = RequestHandler<PathParams, Concept>;

const conceptController = (
  clients: Clients,
  config: Config
): ConceptHandler => {
  const index = `works-indexed-${config.pipelineDate}`;
  const elasticClient = clients.elastic;

  return asyncHandler(async (req, res) => {
    const id = req.params.id;
    const worksIndexResponse = await elasticClient.search({
      index,
      body: {
        size: 1,
        _source: ["display.subjects"],
        query: {
          term: {
            "query.subjects.id": id,
          },
        },
      },
    });

    for (const work of worksIndexResponse.body.hits.hits) {
      for (const subject of work._source.display.subjects) {
        if (subject.id === id) {
          res.status(200).json({
            id,
            label: subject.label,
            identifiers: subject.identifiers,
            type: subject.type,
          });
          return;
        }
      }
    }

    throw new HttpError({
      status: 404,
      label: "Not Found",
      description: `Concept not found for identifier ${id}`,
    });
  });
};

export default conceptController;
