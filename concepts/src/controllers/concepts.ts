import { Client as ElasticClient } from "@elastic/elasticsearch";
import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import levenshtein from "leven";
import * as R from "ramda";
import { Concept, ResultList } from "../types";
import {
  getPaginationResponse,
  paginationElasticBody,
  PaginationQueryParameters,
} from "./pagination";

type QueryParams = { query?: string } & PaginationQueryParameters;

type ConceptHandler = RequestHandler<
  never,
  ResultList<Concept>,
  never,
  QueryParams
>;

type Dependencies = { elasticClient: ElasticClient; index: string };

const conceptsController = ({
  elasticClient,
  index,
}: Dependencies): ConceptHandler =>
  asyncHandler(async (req, res) => {
    const query = req.query.query;
    const elasticResponse = await elasticClient.search({
      index,
      body: {
        ...paginationElasticBody(req.query),
        _source: ["display.subjects"],
        query: {
          bool: {
            must: [
              {
                term: { type: "Visible" },
              },
              {
                exists: { field: "query.subjects.id" },
              },
            ],
            should: query
              ? [
                  {
                    match: {
                      "data.subjects.label": query,
                    },
                  },
                ]
              : [],
          },
        },
      },
    });

    // Because we're actually looking for unique concept IDs,
    // the pagination and page sizes end up being garbage - that's fine for now
    const accumulatedConcepts = new Set<string>();
    const results: Concept[] = elasticResponse.body.hits.hits.flatMap(
      (document: any) => {
        const subjects: Array<Omit<Concept, "type">> =
          document._source.display.subjects;
        // Hack alert: find the matching subject by sorting the subjects
        // by Levenshtein distance to the query
        const matchingSubject = R.sortBy(
          R.compose(
            R.partial(levenshtein, [query?.toLowerCase() ?? ""]),
            R.prop("label")
          ),
          subjects
        )[0];

        if (accumulatedConcepts.has(matchingSubject.id)) {
          return [];
        } else {
          accumulatedConcepts.add(matchingSubject.id);
          return [
            {
              id: matchingSubject.id,
              label: matchingSubject.label,
              identifiers: matchingSubject.identifiers,
              type: "Subject",
            },
          ];
        }
      }
    );

    const requestUrl = new URL(
      req.url,
      `${req.protocol}://${req.headers.host}`
    );
    const paginationResponse = getPaginationResponse({
      requestUrl,
      totalResults: elasticResponse.body.hits.total.value,
    });
    res.status(200).json({
      type: "ResultList",
      results,
      ...paginationResponse,
    });
  });

export default conceptsController;
