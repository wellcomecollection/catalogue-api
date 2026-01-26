import {
  WorkAggregationRequest,
  ImageAggregationRequest,
  WorkAggregations,
  ImageAggregations,
  Aggregation,
  AggregationBucket,
} from "../types";

// ============================================================================
// Aggregation Response Types (from Elasticsearch)
// ============================================================================

type ESBucket = {
  key: string;
  doc_count: number;
  labels?: {
    buckets: Array<{ key: string; doc_count: number }>;
  };
};

type ESTermsAggregation = {
  buckets: ESBucket[];
};

type ESNestedAggregation = {
  doc_count: number;
  terms?: ESTermsAggregation;
  nested?: ESNestedAggregation;
  nestedSelf?: ESNestedAggregation;
};

type ESFilteredAggregation = {
  doc_count: number;
  nested?: ESNestedAggregation;
  nestedSelf?: ESNestedAggregation;
};

type ESContainerAggregation = {
  doc_count: number;
  filtered: ESFilteredAggregation;
};

type ESGlobalAggregation = {
  doc_count: number;
  nestedSelf?: ESNestedAggregation;
};

// ============================================================================
// Aggregation Name Mapping
// ============================================================================

const worksAggregationNames: Record<WorkAggregationRequest, string> = {
  workType: "format",
  "production.dates": "productionDates",
  "genres.label": "genres",
  genres: "genres",
  "subjects.label": "subjects",
  subjects: "subjects",
  "contributors.agent.label": "contributors",
  "contributors.agent": "contributors",
  languages: "languages",
  "items.locations.license": "license",
  availabilities: "availabilities",
};

const imagesAggregationNames: Record<ImageAggregationRequest, string> = {
  "locations.license": "license",
  "source.contributors.agent.label": "contributors",
  "source.contributors.agent": "contributors",
  "source.genres.label": "genres",
  "source.genres": "genres",
  "source.subjects.label": "subjects",
  "source.subjects": "subjects",
};

// ============================================================================
// Bucket Type Detection
// ============================================================================

const labeledIdAggregations = new Set([
  "workType",
  "production.dates",
  "genres",
  "subjects",
  "contributors.agent",
  "languages",
  "items.locations.license",
  "availabilities",
  "locations.license",
  "source.contributors.agent",
  "source.genres",
  "source.subjects",
]);

function isLabeledIdAggregation(request: string): boolean {
  return labeledIdAggregations.has(request);
}

// ============================================================================
// Bucket Data Type Mapping
// ============================================================================

function getBucketType(request: string): string {
  const typeMap: Record<string, string> = {
    workType: "Format",
    "production.dates": "Period",
    "genres.label": "Genre",
    genres: "Genre",
    "subjects.label": "Subject",
    subjects: "Subject",
    "contributors.agent.label": "Agent",
    "contributors.agent": "Agent",
    languages: "Language",
    "items.locations.license": "License",
    availabilities: "Availability",
    "locations.license": "License",
    "source.contributors.agent.label": "Agent",
    "source.contributors.agent": "Agent",
    "source.genres.label": "Genre",
    "source.genres": "Genre",
    "source.subjects.label": "Subject",
    "source.subjects": "Subject",
  };
  return typeMap[request] ?? "AggregationBucketData";
}

// ============================================================================
// Parse Buckets from ES Response
// ============================================================================

function parseBuckets(
  nestedAgg: ESNestedAggregation | undefined,
  isLabeledId: boolean,
  bucketType: string
): AggregationBucket[] {
  if (!nestedAgg?.terms?.buckets) {
    return [];
  }

  return nestedAgg.terms.buckets.map((bucket) => {
    let label = bucket.key;

    // For labeled ID aggregations, get the label from the sub-aggregation
    if (isLabeledId && bucket.labels?.buckets?.length) {
      label = bucket.labels.buckets[0].key;
    }

    return {
      data: {
        id: bucket.key,
        label,
        type: bucketType,
      },
      count: bucket.doc_count,
      type: "AggregationBucket" as const,
    };
  });
}

// ============================================================================
// Merge Main and Self Buckets
// ============================================================================

function mergeBuckets(
  mainBuckets: AggregationBucket[],
  selfBuckets: AggregationBucket[],
  globalSelfBuckets: AggregationBucket[]
): AggregationBucket[] {
  // Create a map of existing bucket IDs
  const bucketMap = new Map<string, AggregationBucket>();

  // Add main buckets
  for (const bucket of mainBuckets) {
    bucketMap.set(bucket.data.id, bucket);
  }

  // Add or update with self buckets (preserves filtered values)
  for (const bucket of selfBuckets) {
    if (!bucketMap.has(bucket.data.id)) {
      bucketMap.set(bucket.data.id, bucket);
    }
  }

  // For zero-count buckets, get labels from global self
  for (const bucket of globalSelfBuckets) {
    const existing = bucketMap.get(bucket.data.id);
    if (existing && existing.count === 0 && !existing.data.label) {
      existing.data.label = bucket.data.label;
    } else if (!bucketMap.has(bucket.data.id)) {
      bucketMap.set(bucket.data.id, bucket);
    }
  }

  // Convert back to array and sort by count desc, then key asc
  return Array.from(bucketMap.values()).sort((a, b) => {
    if (b.count !== a.count) return b.count - a.count;
    return a.data.id.localeCompare(b.data.id);
  });
}

// ============================================================================
// Parse Single Aggregation
// ============================================================================

function parseAggregation(
  esAggregations: Record<string, unknown>,
  aggName: string,
  request: string
): Aggregation | null {
  const containerAgg = esAggregations[aggName] as
    | ESContainerAggregation
    | undefined;
  const globalAgg = esAggregations[`${aggName}Global`] as
    | ESGlobalAggregation
    | undefined;

  if (!containerAgg?.filtered) {
    return null;
  }

  const isLabeledId = isLabeledIdAggregation(request);
  const bucketType = getBucketType(request);

  // Parse main buckets
  const mainBuckets = parseBuckets(
    containerAgg.filtered.nested,
    isLabeledId,
    bucketType
  );

  // Parse self buckets (for filtered values)
  const selfBuckets = parseBuckets(
    containerAgg.filtered.nestedSelf,
    isLabeledId,
    bucketType
  );

  // Parse global self buckets (for zero-count edge cases)
  const globalSelfBuckets = globalAgg?.nestedSelf
    ? parseBuckets(globalAgg.nestedSelf, isLabeledId, bucketType)
    : [];

  // Merge all buckets
  const buckets = mergeBuckets(mainBuckets, selfBuckets, globalSelfBuckets);

  if (buckets.length === 0) {
    return null;
  }

  return {
    buckets,
    type: "Aggregation",
  };
}

// ============================================================================
// Parse Works Aggregations
// ============================================================================

export function parseWorksAggregations(
  esAggregations: Record<string, unknown>,
  requests: WorkAggregationRequest[]
): WorkAggregations {
  const result: WorkAggregations = {};

  for (const request of requests) {
    const aggName = worksAggregationNames[request];
    const aggregation = parseAggregation(esAggregations, aggName, request);

    if (aggregation) {
      // Map the internal aggregation name to the API response key
      switch (request) {
        case "workType":
          result.workType = aggregation;
          break;
        case "production.dates":
          result["production.dates"] = aggregation;
          break;
        case "genres.label":
          result["genres.label"] = aggregation;
          break;
        case "genres":
          result.genres = aggregation;
          break;
        case "subjects.label":
          result["subjects.label"] = aggregation;
          break;
        case "subjects":
          result.subjects = aggregation;
          break;
        case "contributors.agent.label":
          result["contributors.agent.label"] = aggregation;
          break;
        case "contributors.agent":
          result["contributors.agent"] = aggregation;
          break;
        case "languages":
          result.languages = aggregation;
          break;
        case "items.locations.license":
          result["items.locations.license"] = aggregation;
          break;
        case "availabilities":
          result.availabilities = aggregation;
          break;
      }
    }
  }

  return result;
}

// ============================================================================
// Parse Images Aggregations
// ============================================================================

export function parseImagesAggregations(
  esAggregations: Record<string, unknown>,
  requests: ImageAggregationRequest[]
): ImageAggregations {
  const result: ImageAggregations = {};

  for (const request of requests) {
    const aggName = imagesAggregationNames[request];
    const aggregation = parseAggregation(esAggregations, aggName, request);

    if (aggregation) {
      // Map the internal aggregation name to the API response key
      switch (request) {
        case "locations.license":
          result.license = aggregation;
          break;
        case "source.contributors.agent.label":
          result["source.contributors.agent.label"] = aggregation;
          break;
        case "source.contributors.agent":
          result["source.contributors.agent"] = aggregation;
          break;
        case "source.genres.label":
          result["source.genres.label"] = aggregation;
          break;
        case "source.genres":
          result["source.genres"] = aggregation;
          break;
        case "source.subjects.label":
          result["source.subjects.label"] = aggregation;
          break;
        case "source.subjects":
          result["source.subjects"] = aggregation;
          break;
      }
    }
  }

  return result;
}
