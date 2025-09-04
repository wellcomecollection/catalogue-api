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
  "identifiers.identifierType"?: string;
  id?: string; // comma separated list of concept IDs
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
    // If an explicit list of IDs is provided, short-circuit and fetch them via a single _mget request.
    if (req.query.id) {
      const rawIds = req.query.id
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean);
      // Deduplicate to minimise ES work but keep original order (including duplicates) for response.
      const uniqueIds = Array.from(new Set(rawIds));
      if (uniqueIds.length === 0) {
        res.status(200).json({
          type: "ResultList",
          results: [],
          pageSize: 0,
          totalPages: 0,
          totalResults: 0,
        });
        return;
      }

      const mgetResponse = await elasticClient.mget<Displayable<Concept>>({
        index,
        body: { ids: uniqueIds },
        _source: ["display"],
      } as any); // typing gap in elastic client generics for mget

      // Map id -> concept for quick lookup
      const docsMap = new Map<string, Concept>();
      // @ts-ignore - elastic client types for mget docs
      for (const doc of mgetResponse.body?.docs || mgetResponse.docs || []) {
        if (doc.found && doc._id && doc._source?.display) {
          docsMap.set(doc._id, doc._source.display as Concept);
        }
      }

      // Reconstruct results preserving original order and duplicates, omitting missing ids silently
      const orderedResults: Concept[] = rawIds.flatMap((id) =>
        docsMap.has(id) ? [docsMap.get(id)!] : []
      );

      res.status(200).json({
        type: "ResultList",
        results: orderedResults,
        pageSize: orderedResults.length,
        totalPages: 1,
        totalResults: orderedResults.length,
      });
      return; // ensure no further processing
    }

    const queryString = req.query.query;
    const identifierTypeFilter = req.query["identifiers.identifierType"];
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
