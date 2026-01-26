import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { errors as elasticErrors } from "@elastic/elasticsearch";
import { ZodError } from "zod";
import {
  Clients,
  Work,
  Displayable,
  ResultList,
  WorkAggregations,
  IndexedWork,
} from "../types";
import { Config } from "../../config";
import { HttpError } from "./error";
import { paginationResponseGetter } from "./pagination";
import {
  multipleWorksQuerySchema,
  singleWorkQuerySchema,
  buildWorkFilters,
} from "../services/query-params";
import { buildWorksSearchRequest } from "../services/request-builder";
import { parseWorksAggregations } from "../services/aggregation-parser";

// ============================================================================
// Multiple Works Controller (GET /works)
// ============================================================================

type WorksHandler = RequestHandler<
  never,
  ResultList<Work, WorkAggregations>,
  never,
  Record<string, string | undefined>
>;

export const worksController = (
  clients: Clients,
  config: Config
): WorksHandler => {
  const elasticClient = clients.elastic;
  const getPaginationResponse = paginationResponseGetter(
    config.publicRootUrl,
    config
  );

  return asyncHandler(async (req, res) => {
    // Parse and validate query parameters
    let parsedQuery;
    try {
      parsedQuery = multipleWorksQuerySchema.parse(req.query);
    } catch (error) {
      if (error instanceof ZodError) {
        const messages = error.errors.map(
          (e) => `${e.path.join(".")}: ${e.message}`
        );
        throw new HttpError({
          status: 400,
          label: "Bad Request",
          description: messages.join("; "),
        });
      }
      throw error;
    }

    const pageSize = parsedQuery.pageSize ?? config.defaultPageSize;
    const pageNumber = parsedQuery.page ?? 1;

    // Validate deep pagination - Elasticsearch limits to 10000 total results
    const maxResults = 10000;
    const requestedOffset = (pageNumber - 1) * pageSize + pageSize;
    if (
      requestedOffset > maxResults ||
      pageNumber > Math.floor(maxResults / pageSize)
    ) {
      throw new HttpError({
        status: 400,
        label: "Bad Request",
        description:
          `Only the first 10000 works are available in the API. ` +
          `If you want more works, please contact us: digital@wellcomecollection.org`,
      });
    }

    // Build search options
    const searchOptions = {
      searchQuery: parsedQuery.query,
      filters: buildWorkFilters(parsedQuery),
      aggregations: parsedQuery.aggregations ?? [],
      sortBy: parsedQuery.sort,
      sortOrder: parsedQuery.sortOrder ?? ("asc" as const),
      pageSize,
      pageNumber,
    };

    // Build and execute the search request
    const searchRequest = buildWorksSearchRequest(
      searchOptions,
      config.worksIndex
    );

    const searchResponse = await elasticClient.search<IndexedWork>(
      searchRequest
    );

    // Extract results
    const results: Work[] = searchResponse.hits.hits.flatMap((hit) =>
      hit._source?.display ? [hit._source.display as Work] : []
    );

    // Calculate total results
    const totalResults =
      typeof searchResponse.hits.total === "number"
        ? searchResponse.hits.total
        : searchResponse.hits.total?.value ?? 0;

    // Build pagination response
    const requestUrl = new URL(
      req.url,
      `${req.protocol}://${req.headers.host}`
    );
    const paginationResponse = getPaginationResponse({
      requestUrl,
      totalResults,
    });

    // Parse aggregations
    const aggregations = searchResponse.aggregations
      ? parseWorksAggregations(
          searchResponse.aggregations as Record<string, unknown>,
          searchOptions.aggregations
        )
      : undefined;

    res.status(200).json({
      type: "ResultList",
      results,
      ...(aggregations && Object.keys(aggregations).length > 0
        ? { aggregations }
        : {}),
      ...paginationResponse,
    });
  });
};

// ============================================================================
// Single Work Controller (GET /works/:id)
// ============================================================================

type SingleWorkHandler = RequestHandler<
  { id: string },
  Work,
  never,
  Record<string, string | undefined>
>;

export const workController = (
  clients: Clients,
  config: Config
): SingleWorkHandler => {
  const elasticClient = clients.elastic;

  return asyncHandler(async (req, res) => {
    const id = req.params.id;

    // Parse and validate query parameters
    let parsedQuery;
    try {
      parsedQuery = singleWorkQuerySchema.parse(req.query);
    } catch (error) {
      if (error instanceof ZodError) {
        const messages = error.errors.map(
          (e) => `${e.path.join(".")}: ${e.message}`
        );
        throw new HttpError({
          status: 400,
          label: "Bad Request",
          description: messages.join("; "),
        });
      }
      throw error;
    }

    try {
      const getResponse = await elasticClient.get<IndexedWork>({
        index: config.worksIndex,
        id,
        _source: ["display", "type", "redirectTo"],
      });

      const doc = getResponse._source;

      // Check if work is visible
      if (doc?.type === "Redirected") {
        if (doc.redirectTo) {
          // Build redirect URL preserving query parameters
          const redirectUrl = new URL(
            `${config.publicRootUrl}/works/${doc.redirectTo}`
          );
          const originalQuery = req.url.split("?")[1];
          if (originalQuery) {
            redirectUrl.search = originalQuery;
          }
          res.redirect(302, redirectUrl.toString());
          return;
        }
        // Redirected work without redirectTo target
        throw new HttpError({
          status: 404,
          label: "Not Found",
          description: `Work not found for identifier ${id}`,
        });
      }

      if (doc?.type === "Deleted") {
        throw new HttpError({
          status: 410,
          label: "Gone",
          description: `This work has been deleted`,
        });
      }

      if (doc?.type === "Invisible") {
        throw new HttpError({
          status: 410,
          label: "Gone",
          description: `This work has been deleted`,
        });
      }

      if (!doc?.display) {
        throw new HttpError({
          status: 404,
          label: "Not Found",
          description: `Work not found for identifier ${id}`,
        });
      }

      // Apply includes filtering
      const work = applyWorkIncludes(doc.display as Work, parsedQuery.include);

      res.status(200).json(work);
    } catch (error) {
      if (error instanceof elasticErrors.ResponseError) {
        if (error.statusCode === 404) {
          throw new HttpError({
            status: 404,
            label: "Not Found",
            description: `Work not found for identifier ${id}`,
          });
        }
      }
      throw error;
    }
  });
};

// ============================================================================
// Apply Work Includes (filter optional fields)
// ============================================================================

function applyWorkIncludes(work: Work, includes?: Set<string>): Work {
  // If no includes specified, return minimal work
  if (!includes || includes.size === 0) {
    const {
      identifiers,
      items,
      holdings,
      subjects,
      genres,
      contributors,
      production,
      languages,
      notes,
      formerFrequency,
      designation,
      images,
      parts,
      partOf,
      precededBy,
      succeededBy,
      ...minimalWork
    } = work;
    return minimalWork;
  }

  // Return work with only requested includes
  const result: Work = {
    id: work.id,
    title: work.title,
    alternativeTitles: work.alternativeTitles,
    availabilities: work.availabilities,
    type: work.type,
  };

  // Copy optional base fields
  if (work.workType) result.workType = work.workType;
  if (work.edition) result.edition = work.edition;
  if (work.duration) result.duration = work.duration;
  if (work.thumbnail) result.thumbnail = work.thumbnail;

  // Add requested includes
  if (includes.has("identifiers") && work.identifiers) {
    result.identifiers = work.identifiers;
  }
  if (includes.has("items") && work.items) {
    result.items = work.items;
  }
  if (includes.has("holdings") && work.holdings) {
    result.holdings = work.holdings;
  }
  if (includes.has("subjects") && work.subjects) {
    result.subjects = work.subjects;
  }
  if (includes.has("genres") && work.genres) {
    result.genres = work.genres;
  }
  if (includes.has("contributors") && work.contributors) {
    result.contributors = work.contributors;
  }
  if (includes.has("production") && work.production) {
    result.production = work.production;
  }
  if (includes.has("languages") && work.languages) {
    result.languages = work.languages;
  }
  if (includes.has("notes") && work.notes) {
    result.notes = work.notes;
  }
  if (includes.has("formerFrequency") && work.formerFrequency) {
    result.formerFrequency = work.formerFrequency;
  }
  if (includes.has("designation") && work.designation) {
    result.designation = work.designation;
  }
  if (includes.has("images") && work.images) {
    result.images = work.images;
  }
  if (includes.has("parts") && work.parts) {
    result.parts = work.parts;
  }
  if (includes.has("partOf") && work.partOf) {
    result.partOf = work.partOf;
  }
  if (includes.has("precededBy") && work.precededBy) {
    result.precededBy = work.precededBy;
  }
  if (includes.has("succeededBy") && work.succeededBy) {
    result.succeededBy = work.succeededBy;
  }

  return result;
}

export default { worksController, workController };
