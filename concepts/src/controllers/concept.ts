import { Client as ElasticClient } from "@elastic/elasticsearch";
import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Concept } from "../types";
import { ErrorResponse, errorResponse } from "./responses";

type Params = { id: string };

type ConceptHandler = RequestHandler<Params, Concept | ErrorResponse>;

type Dependencies = { elasticClient: ElasticClient; index: string };

const conceptController = ({
  elasticClient,
  index,
}: Dependencies): ConceptHandler =>
  asyncHandler(async (req, res) => {
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
      for (const subject of work.display.subjects) {
        if (subject.id === id) {
          res.status(200).json({
            id,
            label: subject.label,
            identifiers: [subject.identifiers],
            type: "Subject",
          });
          return;
        }
      }
    }

    res.status(404).json(
      errorResponse({
        status: 404,
        label: "Not Found",
        description: "Concept not found",
      })
    );
  });

export default conceptController;
