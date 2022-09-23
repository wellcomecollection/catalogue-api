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
        _source: ["display.contributors.agent", "display.subjects"],
        query: {
          bool: {
            should: [
              {
                term: {
                  "query.subjects.id": id
                }
              },
              {
                term: {
                  "query.contributors.agent.id": id
                }
              }
            ],
            minimum_should_match: 1
          }
        },
        // We don't care much about how we sort here, we just care that we have a sort
        // that will be consistent across Elasticsearch churn.
        //
        // e.g. the concept with ID v3m7uhy9 is labelled as "Darwin, Charles, 1809-1882"
        // and "Darwin, Charles, 1809-1882 Influence" on different works.  For the prototype
        // it doesn't matter which we pick, but if we don't specify a sort then we're at the
        // mercy of Elasticsearch to choose, and it may change over time.
        //
        // See https://wellcome.slack.com/archives/C02ANCYL90E/p1663920016045829
        sort: ['query.id']
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

      for (const contributor of work._source.display.contributors) {
        if (contributor.agent.id === id) {
          res.status(200).json({
            id,
            label: contributor.agent.label,
            identifiers: contributor.agent.identifiers,
            type: contributor.agent.type,
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
