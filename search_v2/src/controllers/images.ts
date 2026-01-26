import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { errors as elasticErrors } from "@elastic/elasticsearch";
import { ZodError } from "zod";
import {
  Clients,
  Image,
  ResultList,
  ImageAggregations,
  IndexedImage,
} from "../types";
import { Config } from "../../config";
import { HttpError } from "./error";
import { paginationResponseGetter } from "./pagination";
import {
  multipleImagesQuerySchema,
  singleImageQuerySchema,
  buildImageFilters,
} from "../services/query-params";
import { buildImagesSearchRequest } from "../services/request-builder";
import { parseImagesAggregations } from "../services/aggregation-parser";

// ============================================================================
// Multiple Images Controller (GET /images)
// ============================================================================

type ImagesHandler = RequestHandler<
  never,
  ResultList<Image, ImageAggregations>,
  never,
  Record<string, string | undefined>
>;

export const imagesController = (
  clients: Clients,
  config: Config
): ImagesHandler => {
  const elasticClient = clients.elastic;
  const getPaginationResponse = paginationResponseGetter(
    config.publicRootUrl,
    config
  );

  return asyncHandler(async (req, res) => {
    // Parse and validate query parameters
    let parsedQuery;
    try {
      parsedQuery = multipleImagesQuerySchema.parse(req.query);
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

    // Build search options
    const sortOrder = parsedQuery.sortOrder ?? "asc";
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
          `Only the first 10000 images are available in the API. ` +
          `If you want more images, please contact us: digital@wellcomecollection.org`,
      });
    }

    const searchOptions = {
      searchQuery: parsedQuery.query,
      filters: buildImageFilters(parsedQuery),
      aggregations: parsedQuery.aggregations ?? [],
      color: parsedQuery.color,
      sortBy: parsedQuery.sort,
      sortOrder: sortOrder as "asc" | "desc",
      pageSize,
      pageNumber,
    };

    // Build and execute the search request
    const searchRequest = buildImagesSearchRequest(
      searchOptions,
      config.imagesIndex
    );

    const searchResponse = await elasticClient.search<IndexedImage>(
      searchRequest
    );

    // Extract results
    const results: Image[] = searchResponse.hits.hits.flatMap((hit) =>
      hit._source?.display ? [hit._source.display as Image] : []
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
      ? parseImagesAggregations(
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
// Single Image Controller (GET /images/:id)
// ============================================================================

type SingleImageHandler = RequestHandler<
  { id: string },
  Image,
  never,
  Record<string, string | undefined>
>;

export const imageController = (
  clients: Clients,
  config: Config
): SingleImageHandler => {
  const elasticClient = clients.elastic;

  return asyncHandler(async (req, res) => {
    const id = req.params.id;

    // Parse and validate query parameters
    let parsedQuery;
    try {
      parsedQuery = singleImageQuerySchema.parse(req.query);
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
      const getResponse = await elasticClient.get<IndexedImage>({
        index: config.imagesIndex,
        id,
        _source: ["display", "vectorValues.features"],
      });

      const doc = getResponse._source;

      if (!doc?.display) {
        throw new HttpError({
          status: 404,
          label: "Not Found",
          description: `Image not found for identifier ${id}`,
        });
      }

      // Apply includes filtering
      const image = applyImageIncludes(
        doc.display as Image,
        parsedQuery.include
      );

      // Handle withSimilarFeatures include
      if (
        parsedQuery.include?.has("withSimilarFeatures") &&
        doc.vectorValues?.features
      ) {
        // TODO: Implement similarity search using features vector
        // This would require another ES query using the features vector
      }

      res.status(200).json(image);
    } catch (error) {
      if (error instanceof elasticErrors.ResponseError) {
        if (error.statusCode === 404) {
          throw new HttpError({
            status: 404,
            label: "Not Found",
            description: `Image not found for identifier ${id}`,
          });
        }
      }
      throw error;
    }
  });
};

// ============================================================================
// Apply Image Includes (filter optional fields)
// ============================================================================

function applyImageIncludes(image: Image, includes?: Set<string>): Image {
  // If no includes specified, return minimal image
  if (!includes || includes.size === 0) {
    const result: Image = {
      id: image.id,
      locations: image.locations,
      source: {
        id: image.source.id,
        title: image.source.title,
        type: image.source.type,
      },
      type: image.type,
    };
    if (image.thumbnail) result.thumbnail = image.thumbnail;
    return result;
  }

  // Return image with requested includes
  const result: Image = {
    id: image.id,
    locations: image.locations,
    source: {
      id: image.source.id,
      title: image.source.title,
      type: image.source.type,
    },
    type: image.type,
  };

  if (image.thumbnail) result.thumbnail = image.thumbnail;

  // Add requested source includes
  if (includes.has("source.contributors") && image.source.contributors) {
    result.source.contributors = image.source.contributors;
  }
  if (includes.has("source.languages") && image.source.languages) {
    result.source.languages = image.source.languages;
  }
  if (includes.has("source.genres") && image.source.genres) {
    result.source.genres = image.source.genres;
  }
  if (includes.has("source.subjects") && image.source.subjects) {
    result.source.subjects = image.source.subjects;
  }

  return result;
}

export default { imagesController, imageController };
