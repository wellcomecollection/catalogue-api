import { readFileSync } from "fs";
import { join } from "path";
import {
  WorkSearchOptions,
  ImageSearchOptions,
  WorkFilter,
  ImageFilter,
  WorkAggregationRequest,
  ImageAggregationRequest,
} from "../types";
import {
  buildWorkFilterQuery,
  buildImageFilterQuery,
  visibleWorkFilter,
  isPairableWorkFilter,
  isPairableImageFilter,
  getWorksSortConfig,
  getImagesSortConfig,
  ElasticQuery,
} from "./filter-builder";
import {
  buildWorksAggregations,
  buildImagesAggregations,
} from "./aggregations-builder";

// ============================================================================
// Load Query Templates
// ============================================================================

function loadQueryTemplate(filename: string): unknown {
  const templatePath = join(__dirname, "../resources", filename);
  const content = readFileSync(templatePath, "utf-8");
  return JSON.parse(content);
}

const worksQueryTemplate = loadQueryTemplate("WorksQuery.json");
const imagesQueryTemplate = loadQueryTemplate("ImagesQuery.json");

// ============================================================================
// Helper to substitute {{query}} in templates
// ============================================================================

function substituteQueryInTemplate(template: unknown, query: string): unknown {
  if (typeof template === "string") {
    return template.replace(/\{\{query\}\}/g, query);
  }
  if (Array.isArray(template)) {
    return template.map((item) => substituteQueryInTemplate(item, query));
  }
  if (template !== null && typeof template === "object") {
    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(template)) {
      result[key] = substituteQueryInTemplate(value, query);
    }
    return result;
  }
  return template;
}

// ============================================================================
// Works Request Builder
// ============================================================================

export type ElasticsearchSearchRequest = {
  index: string;
  body: {
    query?: unknown;
    aggs?: Record<string, unknown>;
    post_filter?: unknown;
    sort?: unknown[];
    from: number;
    size: number;
    _source: string[];
    track_total_hits: boolean;
    knn?: unknown;
  };
};

export function buildWorksSearchRequest(
  options: WorkSearchOptions,
  index: string
): ElasticsearchSearchRequest {
  // Split filters into pairable and unpairable
  const pairableFilters = options.filters.filter(isPairableWorkFilter);
  const unpairableFilters = options.filters.filter(
    (f) => !isPairableWorkFilter(f)
  );

  // Build pre-filter (visible + unpairables) - applied to query
  const preFilterQueries: ElasticQuery[] = [visibleWorkFilter];
  for (const filter of unpairableFilters) {
    const query = buildWorkFilterQuery(filter);
    if (query) preFilterQueries.push(query);
  }

  // Build post-filter (pairables) - applied after aggregations
  const postFilterQueries: ElasticQuery[] = [];
  for (const filter of pairableFilters) {
    const query = buildWorkFilterQuery(filter);
    if (query) postFilterQueries.push(query);
  }

  // Build the main query
  let mainQuery: unknown;
  if (options.searchQuery) {
    const textQuery = substituteQueryInTemplate(
      worksQueryTemplate,
      options.searchQuery
    );
    mainQuery = {
      bool: {
        must: [textQuery],
        filter: preFilterQueries,
      },
    };
  } else {
    mainQuery = {
      bool: {
        must: [{ match_all: {} }],
        filter: preFilterQueries,
      },
    };
  }

  // Build sort configuration
  const sortConfig = getWorksSortConfig(options);
  const sort: unknown[] = [];
  if (options.searchQuery) {
    sort.push("_score");
  }
  if (sortConfig) {
    sort.push({
      [sortConfig.field]: {
        order: sortConfig.order,
        missing: "_last",
      },
    });
  }
  sort.push({ "query.id": { order: "asc" } });

  // Build aggregations
  const aggregations = buildWorksAggregations(
    pairableFilters as (WorkFilter & { type: string })[],
    options.aggregations
  );

  // Build the request
  const request: ElasticsearchSearchRequest = {
    index,
    body: {
      query: mainQuery,
      from: (options.pageNumber - 1) * options.pageSize,
      size: options.pageSize,
      _source: ["display", "type"],
      track_total_hits: true,
      sort,
    },
  };

  // Add post_filter if there are pairable filters
  if (postFilterQueries.length > 0) {
    request.body.post_filter = {
      bool: {
        must: postFilterQueries,
      },
    };
  }

  // Add aggregations
  if (Object.keys(aggregations).length > 0) {
    request.body.aggs = aggregations;
  }

  return request;
}

// ============================================================================
// Images Request Builder
// ============================================================================

export function buildImagesSearchRequest(
  options: ImageSearchOptions,
  index: string
): ElasticsearchSearchRequest {
  // Split filters into pairable and unpairable
  const pairableFilters = options.filters.filter(isPairableImageFilter);
  const unpairableFilters = options.filters.filter(
    (f) => !isPairableImageFilter(f)
  );

  // Build pre-filter (unpairables) - applied to query
  const preFilterQueries: ElasticQuery[] = [];
  for (const filter of unpairableFilters) {
    const query = buildImageFilterQuery(filter);
    if (query) preFilterQueries.push(query);
  }

  // Build post-filter (all filters for images)
  const postFilterQueries: ElasticQuery[] = [];
  for (const filter of options.filters) {
    const query = buildImageFilterQuery(filter);
    if (query) postFilterQueries.push(query);
  }

  // Build the main query
  let mainQuery: unknown;
  if (options.searchQuery) {
    const textQuery = substituteQueryInTemplate(
      imagesQueryTemplate,
      options.searchQuery
    );
    if (preFilterQueries.length > 0) {
      mainQuery = {
        bool: {
          must: [textQuery],
          filter: preFilterQueries,
        },
      };
    } else {
      mainQuery = textQuery;
    }
  } else if (preFilterQueries.length > 0) {
    mainQuery = {
      bool: {
        must: [{ match_all: {} }],
        filter: preFilterQueries,
      },
    };
  } else {
    mainQuery = { match_all: {} };
  }

  // Build sort configuration
  const sortConfig = getImagesSortConfig(options);
  const sort: unknown[] = [];
  if (options.searchQuery || options.color) {
    sort.push("_score");
  }
  if (sortConfig) {
    sort.push({
      [sortConfig.field]: {
        order: sortConfig.order,
        missing: "_last",
      },
    });
  }
  sort.push({ "query.id": { order: "asc" } });

  // Build aggregations
  const aggregations = buildImagesAggregations(
    pairableFilters as (ImageFilter & { type: string })[],
    options.aggregations
  );

  // Build the request
  const request: ElasticsearchSearchRequest = {
    index,
    body: {
      query: mainQuery,
      from: (options.pageNumber - 1) * options.pageSize,
      size: options.pageSize,
      _source: ["display", "vectorValues.features"],
      track_total_hits: true,
      sort,
    },
  };

  // Add KNN for color search
  if (options.color) {
    request.body.knn = {
      field: "vectorValues.paletteEmbedding",
      query_vector: [
        options.color.r / 255,
        options.color.g / 255,
        options.color.b / 255,
      ],
      k: 10,
      num_candidates: 100,
    };
  }

  // Add post_filter if there are filters
  if (postFilterQueries.length > 0) {
    request.body.post_filter = {
      bool: {
        must: postFilterQueries,
      },
    };
  }

  // Add aggregations
  if (Object.keys(aggregations).length > 0) {
    request.body.aggs = aggregations;
  }

  return request;
}
