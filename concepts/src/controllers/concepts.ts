import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Clients, Concept, Displayable, ResultList } from "../types";
import {
  paginationElasticBody,
  PaginationQueryParameters,
  paginationResponseGetter,
} from "./pagination";
import { Config } from "../../config";

type QueryParams = {
  query?: string;
  "identifiers.identifierType.id"?: string;
} & PaginationQueryParameters;

type ConceptHandler = RequestHandler<
  never,
  ResultList<Concept>,
  never,
  QueryParams
>;

const conceptsController = (
  clients: Clients,
  config: Config
): ConceptHandler => {
  const index = config.conceptsIndex;
  const elasticClient = clients.elastic;
  const getPaginationResponse = paginationResponseGetter(config.publicRootUrl);

  return asyncHandler(async (req, res) => {
    const queryString = req.query.query;
    const identifierTypeFilter = req.query["identifiers.identifierType.id"];
    const searchResponse = await elasticClient.search<Displayable<Concept>>({
      index,
      body: {
        ...paginationElasticBody(req.query),
        _source: ["display"],
        track_total_hits: true,
        query: {
          bool: {
            should: queryString
              ? [
                  {
                    match: {
                      "query.identifiers.value": {
                        query: queryString,
                        analyzer: "whitespace",
                        operator: "OR",
                        boost: 1000,
                      },
                    },
                  },
                  {
                    multi_match: {
                      query: queryString,
                      fields: ["query.label", "query.alternativeLabels"],
                      type: "cross_fields",
                    },
                  },
                ]
              : [],
            filter: identifierTypeFilter
              ? [
                  {
                    term: {
                      "query.identifiers.identifierType": identifierTypeFilter,
                    },
                  },
                ]
              : [],
          },
        },
        sort: queryString ? ["_score"] : ["query.id"],
      },
    });

    const results = searchResponse.hits.hits.flatMap((hit) =>
      hit._source ? [hit._source.display] : []
    );

    const requestUrl = new URL(
      req.url,
      `${req.protocol}://${req.headers.host}`
    );
    const totalResults =
      typeof searchResponse.hits.total === "number"
        ? searchResponse.hits.total
        : searchResponse.hits.total?.value ?? 0;
    const paginationResponse = getPaginationResponse({
      requestUrl,
      totalResults,
    });
    res.status(200).json({
      type: "ResultList",
      results,
      ...paginationResponse,
    });
  });
};

export default conceptsController;
