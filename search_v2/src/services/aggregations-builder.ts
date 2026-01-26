import {
  WorkFilter,
  ImageFilter,
  WorkAggregationRequest,
  ImageAggregationRequest,
} from "../types";
import {
  buildWorkFilterQuery,
  buildImageFilterQuery,
  ElasticQuery,
} from "./filter-builder";

// ============================================================================
// Aggregation Configuration Types
// ============================================================================

type AggregationType = "labeledId" | "labelOnly";

type AggregationParams = {
  name: string;
  fieldPath: string;
  size: number;
  aggregationType: AggregationType;
};

// ============================================================================
// Works Aggregation Parameters
// ============================================================================

const worksAggregationParams: Record<
  WorkAggregationRequest,
  AggregationParams
> = {
  workType: {
    name: "format",
    fieldPath: "aggregatableValues.workType",
    size: 30,
    aggregationType: "labeledId",
  },
  "production.dates": {
    name: "productionDates",
    fieldPath: "aggregatableValues.production.dates",
    size: 10,
    aggregationType: "labeledId",
  },
  "genres.label": {
    name: "genres",
    fieldPath: "aggregatableValues.genres",
    size: 20,
    aggregationType: "labelOnly",
  },
  genres: {
    name: "genres",
    fieldPath: "aggregatableValues.genres",
    size: 20,
    aggregationType: "labeledId",
  },
  "subjects.label": {
    name: "subjects",
    fieldPath: "aggregatableValues.subjects",
    size: 20,
    aggregationType: "labelOnly",
  },
  subjects: {
    name: "subjects",
    fieldPath: "aggregatableValues.subjects",
    size: 20,
    aggregationType: "labeledId",
  },
  "contributors.agent.label": {
    name: "contributors",
    fieldPath: "aggregatableValues.contributors.agent",
    size: 20,
    aggregationType: "labelOnly",
  },
  "contributors.agent": {
    name: "contributors",
    fieldPath: "aggregatableValues.contributors.agent",
    size: 20,
    aggregationType: "labeledId",
  },
  languages: {
    name: "languages",
    fieldPath: "aggregatableValues.languages",
    size: 200,
    aggregationType: "labeledId",
  },
  "items.locations.license": {
    name: "license",
    fieldPath: "aggregatableValues.items.locations.license",
    size: 20,
    aggregationType: "labeledId",
  },
  availabilities: {
    name: "availabilities",
    fieldPath: "aggregatableValues.availabilities",
    size: 10,
    aggregationType: "labeledId",
  },
};

// ============================================================================
// Images Aggregation Parameters
// ============================================================================

const imagesAggregationParams: Record<
  ImageAggregationRequest,
  AggregationParams
> = {
  "locations.license": {
    name: "license",
    fieldPath: "aggregatableValues.locations.license",
    size: 20,
    aggregationType: "labeledId",
  },
  "source.contributors.agent.label": {
    name: "contributors",
    fieldPath: "aggregatableValues.source.contributors.agent",
    size: 20,
    aggregationType: "labelOnly",
  },
  "source.contributors.agent": {
    name: "contributors",
    fieldPath: "aggregatableValues.source.contributors.agent",
    size: 20,
    aggregationType: "labeledId",
  },
  "source.genres.label": {
    name: "genres",
    fieldPath: "aggregatableValues.source.genres",
    size: 20,
    aggregationType: "labelOnly",
  },
  "source.genres": {
    name: "genres",
    fieldPath: "aggregatableValues.source.genres",
    size: 20,
    aggregationType: "labeledId",
  },
  "source.subjects.label": {
    name: "subjects",
    fieldPath: "aggregatableValues.source.subjects",
    size: 20,
    aggregationType: "labelOnly",
  },
  "source.subjects": {
    name: "subjects",
    fieldPath: "aggregatableValues.source.subjects",
    size: 20,
    aggregationType: "labeledId",
  },
};

// ============================================================================
// Paired Aggregation Mapping (filter type -> aggregation requests)
// ============================================================================

function getPairedWorksAggregations(
  filter: WorkFilter
): WorkAggregationRequest[] {
  switch (filter.type) {
    case "FormatFilter":
      return ["workType"];
    case "LanguagesFilter":
      return ["languages"];
    case "GenreLabelFilter":
      return ["genres.label"];
    case "GenreIdFilter":
      return ["genres"];
    case "SubjectLabelFilter":
      return ["subjects.label"];
    case "SubjectIdFilter":
      return ["subjects"];
    case "ContributorsLabelFilter":
      return ["contributors.agent.label"];
    case "ContributorsIdFilter":
      return ["contributors.agent"];
    case "LicenseFilter":
      return ["items.locations.license"];
    case "AvailabilitiesFilter":
      return ["availabilities"];
    default:
      return [];
  }
}

function getPairedImagesAggregations(
  filter: ImageFilter
): ImageAggregationRequest[] {
  switch (filter.type) {
    case "LicenseFilter":
      return ["locations.license"];
    case "ContributorsLabelFilter":
      return ["source.contributors.agent.label"];
    case "ContributorsIdFilter":
      return ["source.contributors.agent"];
    case "GenreLabelFilter":
      return ["source.genres.label"];
    case "GenreIdFilter":
      return ["source.genres"];
    case "SubjectLabelFilter":
      return ["source.subjects.label"];
    case "SubjectIdFilter":
      return ["source.subjects"];
    default:
      return [];
  }
}

// ============================================================================
// Extract filter values for self-aggregations
// ============================================================================

function getFilterValues(filter: WorkFilter | ImageFilter): string[] {
  switch (filter.type) {
    case "FormatFilter":
      return filter.formatIds;
    case "LanguagesFilter":
      return filter.languageIds;
    case "GenreLabelFilter":
    case "SubjectLabelFilter":
    case "ContributorsLabelFilter":
      return filter.labels;
    case "GenreIdFilter":
    case "SubjectIdFilter":
    case "ContributorsIdFilter":
      return filter.conceptIds;
    case "LicenseFilter":
      return filter.licenseIds;
    case "AvailabilitiesFilter":
      return filter.availabilityIds;
    default:
      return [];
  }
}

// ============================================================================
// Build Terms Aggregation
// ============================================================================

function buildTermsAggregation(
  name: string,
  fieldPath: string,
  size: number,
  includeValues: string[] = []
): Record<string, unknown> {
  const termsAgg: Record<string, unknown> = {
    field: fieldPath,
    size,
    order: [{ _count: "desc" }, { _key: "asc" }],
  };

  // For self-aggregations, include exact values even if count is 0
  if (includeValues.length > 0) {
    termsAgg.include = includeValues;
    termsAgg.min_doc_count = 0;
  }

  return { terms: termsAgg };
}

// ============================================================================
// Build Labeled ID Aggregation (nested with id and label sub-aggs)
// ============================================================================

function buildLabeledIdAggregation(
  params: AggregationParams,
  nestedName: string,
  includeValues: string[] = []
): Record<string, unknown> {
  const idTermsAgg = buildTermsAggregation(
    "terms",
    `${params.fieldPath}.id`,
    params.size,
    includeValues
  );

  // Add label sub-aggregation to get the label for each id
  const termsWithLabels = {
    ...idTermsAgg,
    terms: {
      ...(idTermsAgg.terms as Record<string, unknown>),
    },
    aggs: {
      labels: {
        terms: {
          field: `${params.fieldPath}.label`,
          size: 1,
        },
      },
    },
  };

  // The nestedName is used for the outer NestedAggregation name (e.g., "nested" or "nestedSelf")
  // The inner terms aggregation should always be named "terms" to match Scala structure
  return {
    [nestedName]: {
      nested: {
        path: params.fieldPath,
      },
      aggs: {
        terms: termsWithLabels,
      },
    },
  };
}

// ============================================================================
// Build Label Only Aggregation (nested with just label sub-agg)
// ============================================================================

function buildLabelOnlyAggregation(
  params: AggregationParams,
  nestedName: string,
  includeValues: string[] = []
): Record<string, unknown> {
  const labelTermsAgg = buildTermsAggregation(
    "terms",
    `${params.fieldPath}.label`,
    params.size,
    includeValues
  );

  // The nestedName is used for the outer NestedAggregation name (e.g., "nested" or "nestedSelf")
  // The inner terms aggregation should always be named "terms" to match Scala structure
  return {
    [nestedName]: {
      nested: {
        path: params.fieldPath,
      },
      aggs: {
        terms: labelTermsAgg,
      },
    },
  };
}

// ============================================================================
// Build Aggregation (labeled ID or label only)
// ============================================================================

function buildAggregation(
  params: AggregationParams,
  nestedName: string,
  includeValues: string[] = []
): Record<string, unknown> {
  // The buildLabeledIdAggregation and buildLabelOnlyAggregation functions now
  // return { [nestedName]: { nested: {...}, aggs: {...} } }
  // So we just return the inner part directly
  if (params.aggregationType === "labeledId") {
    const result = buildLabeledIdAggregation(params, nestedName, includeValues);
    return result[nestedName] as Record<string, unknown>;
  } else {
    const result = buildLabelOnlyAggregation(params, nestedName, includeValues);
    return result[nestedName] as Record<string, unknown>;
  }
}

// ============================================================================
// Build RFC 37 Compliant Aggregations
//
// Each aggregation follows this structure:
// - [field]: Container filter aggregation (match_all)
//   - filtered: Filter agg with all filters EXCEPT paired filter
//     - nested: Main nested aggregation
//     - nestedSelf: Self aggregation (if paired filter exists)
// - [field]Global: Global aggregation (ignores query/filters)
//   - nestedSelf: Self aggregation for edge cases
// ============================================================================

function buildFilterAggregation<
  Filter extends WorkFilter | ImageFilter,
  AggRequest extends string
>(
  aggregationRequest: AggRequest,
  params: AggregationParams,
  filters: Filter[],
  getPairedAggregations: (filter: Filter) => AggRequest[],
  buildFilterQuery: (filter: Filter) => ElasticQuery | null
): Record<string, unknown> {
  // Find the paired filter for this aggregation
  const pairedFilter = filters.find((filter) =>
    getPairedAggregations(filter).includes(aggregationRequest)
  );

  // Build filter queries for all non-paired filters
  const nonPairedFilters = pairedFilter
    ? filters.filter((f) => f !== pairedFilter)
    : filters;

  const filterQueries: ElasticQuery[] = nonPairedFilters
    .map(buildFilterQuery)
    .filter((q): q is ElasticQuery => q !== null);

  // Build nested aggregation
  const nestedAgg = buildAggregation(params, "nested");

  // Build self aggregation if we have a paired filter
  let selfAgg: Record<string, unknown> | null = null;
  const pairedValues = pairedFilter ? getFilterValues(pairedFilter) : [];
  if (pairedValues.length > 0) {
    selfAgg = buildAggregation(params, "nestedSelf", pairedValues);
  }

  // Build the filtered aggregation with subaggregations
  const filteredSubAggs: Record<string, unknown> = {
    nested: nestedAgg,
  };
  if (selfAgg) {
    filteredSubAggs.nestedSelf = selfAgg;
  }

  const filteredAgg = {
    filter: {
      bool: {
        must: filterQueries.length > 0 ? filterQueries : [{ match_all: {} }],
      },
    },
    aggs: filteredSubAggs,
  };

  // Build the main container aggregation
  const result: Record<string, unknown> = {};

  result[params.name] = {
    filter: { match_all: {} },
    aggs: {
      filtered: filteredAgg,
    },
  };

  // Build global aggregation for edge cases (returns self values even with 0 results)
  if (selfAgg) {
    result[`${params.name}Global`] = {
      global: {},
      aggs: {
        nestedSelf: selfAgg,
      },
    };
  }

  return result;
}

// ============================================================================
// Build Works Aggregations
// ============================================================================

export function buildWorksAggregations(
  filters: WorkFilter[],
  aggregationRequests: WorkAggregationRequest[]
): Record<string, unknown> {
  const result: Record<string, unknown> = {};

  for (const request of aggregationRequests) {
    const params = worksAggregationParams[request];
    if (!params) continue;

    const aggs = buildFilterAggregation(
      request,
      params,
      filters,
      getPairedWorksAggregations,
      buildWorkFilterQuery
    );

    Object.assign(result, aggs);
  }

  return result;
}

// ============================================================================
// Build Images Aggregations
// ============================================================================

export function buildImagesAggregations(
  filters: ImageFilter[],
  aggregationRequests: ImageAggregationRequest[]
): Record<string, unknown> {
  const result: Record<string, unknown> = {};

  for (const request of aggregationRequests) {
    const params = imagesAggregationParams[request];
    if (!params) continue;

    const aggs = buildFilterAggregation(
      request,
      params,
      filters,
      getPairedImagesAggregations,
      buildImageFilterQuery
    );

    Object.assign(result, aggs);
  }

  return result;
}
