import supertest from "supertest";
import { Client } from "@elastic/elasticsearch";
import Mock from "@elastic/elasticsearch-mock";
import { Work, Image } from "../../src/types";
import createApp from "../../src/app";
import { Config } from "../../config";

// ============================================================================
// Aggregation Response Builder
// ============================================================================

// Mapping from API aggregation request to internal ES aggregation name
const worksAggregationNameMap: Record<string, string> = {
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

// Mapping from API aggregation request to aggregatableValues field prefix
const worksAggregationFieldMap: Record<string, string> = {
  workType: "workType",
  "production.dates": "production.dates",
  "genres.label": "genres",
  genres: "genres",
  "subjects.label": "subjects",
  subjects: "subjects",
  "contributors.agent.label": "contributors.agent",
  "contributors.agent": "contributors.agent",
  languages: "languages",
  "items.locations.license": "items.locations.license",
  availabilities: "availabilities",
};

// Aggregations that use labeled ID format (id + label)
const labeledIdAggregations = new Set([
  "workType",
  "production.dates",
  "genres",
  "subjects",
  "contributors.agent",
  "languages",
  "items.locations.license",
  "availabilities",
]);

type AggregationBucketData = {
  id: string;
  label: string;
  count: number;
};

function buildMockAggregations(
  docs: Array<{ aggregatableValues?: Record<string, unknown> }>,
  requestedAggregations: string[]
): Record<string, unknown> {
  if (requestedAggregations.length === 0) {
    return {};
  }

  const result: Record<string, unknown> = {};

  for (const aggRequest of requestedAggregations) {
    const esAggName = worksAggregationNameMap[aggRequest];
    const fieldPrefix = worksAggregationFieldMap[aggRequest];

    if (!esAggName || !fieldPrefix) continue;

    const isLabeledId = labeledIdAggregations.has(aggRequest);

    // Collect and count values from documents
    const bucketMap = new Map<string, AggregationBucketData>();

    for (const doc of docs) {
      const aggValues = doc.aggregatableValues ?? {};

      if (isLabeledId) {
        // For labeled ID aggregations, look for .id and .label fields
        const idField = `${fieldPrefix}.id`;
        const labelField = `${fieldPrefix}.label`;
        const idValue = aggValues[idField];
        const labelValue = aggValues[labelField];

        if (idValue !== undefined) {
          const ids = Array.isArray(idValue) ? idValue : [idValue];
          const labels = labelValue
            ? Array.isArray(labelValue)
              ? labelValue
              : [labelValue]
            : ids; // Use id as label if no label provided

          for (let i = 0; i < ids.length; i++) {
            const id = String(ids[i]);
            const label = String(labels[i] ?? id);
            const existing = bucketMap.get(id);
            if (existing) {
              existing.count++;
            } else {
              bucketMap.set(id, { id, label, count: 1 });
            }
          }
        }
      } else {
        // For label-only aggregations, look for .label field
        const labelField = `${fieldPrefix}.label`;
        const labelValue = aggValues[labelField];

        if (labelValue !== undefined) {
          const labels = Array.isArray(labelValue) ? labelValue : [labelValue];

          for (const label of labels) {
            const labelStr = String(label);
            const existing = bucketMap.get(labelStr);
            if (existing) {
              existing.count++;
            } else {
              bucketMap.set(labelStr, {
                id: labelStr,
                label: labelStr,
                count: 1,
              });
            }
          }
        }
      }
    }

    // Convert to sorted buckets (count desc, key asc)
    const buckets = Array.from(bucketMap.values()).sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count;
      return a.id.localeCompare(b.id);
    });

    // Build ES-style nested aggregation response
    const termsBuckets = buckets.map((b) => {
      const bucket: Record<string, unknown> = {
        key: b.id,
        doc_count: b.count,
      };

      // For labeled ID aggregations, add labels sub-aggregation
      if (isLabeledId) {
        bucket.labels = {
          buckets: [{ key: b.label, doc_count: b.count }],
        };
      }

      return bucket;
    });

    // Build the full nested structure expected by aggregation-parser.ts
    result[esAggName] = {
      doc_count: docs.length,
      filtered: {
        doc_count: docs.length,
        nested: {
          doc_count: termsBuckets.reduce(
            (sum, b) => sum + (b.doc_count as number),
            0
          ),
          terms: {
            buckets: termsBuckets,
          },
        },
      },
    };

    // Add global aggregation (for edge cases with zero results)
    result[`${esAggName}Global`] = {
      doc_count: docs.length,
      nestedSelf: {
        doc_count: termsBuckets.reduce(
          (sum, b) => sum + (b.doc_count as number),
          0
        ),
        terms: {
          buckets: termsBuckets,
        },
      },
    };
  }

  return result;
}

// Images aggregation field mappings
const imagesAggregationNameMap: Record<string, string> = {
  "locations.license": "license",
  "source.contributors.agent.label": "contributors",
  "source.contributors.agent": "contributors",
  "source.genres.label": "genres",
  "source.genres": "genres",
  "source.subjects.label": "subjects",
  "source.subjects": "subjects",
};

const imagesAggregationFieldMap: Record<string, string> = {
  "locations.license": "locations.license",
  "source.contributors.agent.label": "source.contributors.agent",
  "source.contributors.agent": "source.contributors.agent",
  "source.genres.label": "source.genres",
  "source.genres": "source.genres",
  "source.subjects.label": "source.subjects",
  "source.subjects": "source.subjects",
};

const imagesLabeledIdAggregations = new Set([
  "locations.license",
  "source.contributors.agent",
  "source.genres",
  "source.subjects",
]);

function buildMockImagesAggregations(
  docs: Array<{ aggregatableValues?: Record<string, unknown> }>,
  requestedAggregations: string[]
): Record<string, unknown> {
  if (requestedAggregations.length === 0) {
    return {};
  }

  const result: Record<string, unknown> = {};

  for (const aggRequest of requestedAggregations) {
    const esAggName = imagesAggregationNameMap[aggRequest];
    const fieldPrefix = imagesAggregationFieldMap[aggRequest];

    if (!esAggName || !fieldPrefix) continue;

    const isLabeledId = imagesLabeledIdAggregations.has(aggRequest);

    // Collect and count values from documents
    const bucketMap = new Map<string, AggregationBucketData>();

    for (const doc of docs) {
      const aggValues = doc.aggregatableValues ?? {};

      if (isLabeledId) {
        const idField = `${fieldPrefix}.id`;
        const labelField = `${fieldPrefix}.label`;
        const idValue = aggValues[idField];
        const labelValue = aggValues[labelField];

        if (idValue !== undefined) {
          const ids = Array.isArray(idValue) ? idValue : [idValue];
          const labels = labelValue
            ? Array.isArray(labelValue)
              ? labelValue
              : [labelValue]
            : ids;

          for (let i = 0; i < ids.length; i++) {
            const id = String(ids[i]);
            const label = String(labels[i] ?? id);
            const existing = bucketMap.get(id);
            if (existing) {
              existing.count++;
            } else {
              bucketMap.set(id, { id, label, count: 1 });
            }
          }
        }
      } else {
        const labelField = `${fieldPrefix}.label`;
        const labelValue = aggValues[labelField];

        if (labelValue !== undefined) {
          const labels = Array.isArray(labelValue) ? labelValue : [labelValue];

          for (const label of labels) {
            const labelStr = String(label);
            const existing = bucketMap.get(labelStr);
            if (existing) {
              existing.count++;
            } else {
              bucketMap.set(labelStr, {
                id: labelStr,
                label: labelStr,
                count: 1,
              });
            }
          }
        }
      }
    }

    const buckets = Array.from(bucketMap.values()).sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count;
      return a.id.localeCompare(b.id);
    });

    const termsBuckets = buckets.map((b) => {
      const bucket: Record<string, unknown> = {
        key: b.id,
        doc_count: b.count,
      };

      if (isLabeledId) {
        bucket.labels = {
          buckets: [{ key: b.label, doc_count: b.count }],
        };
      }

      return bucket;
    });

    result[esAggName] = {
      doc_count: docs.length,
      filtered: {
        doc_count: docs.length,
        nested: {
          doc_count: termsBuckets.reduce(
            (sum, b) => sum + (b.doc_count as number),
            0
          ),
          terms: {
            buckets: termsBuckets,
          },
        },
      },
    };

    result[`${esAggName}Global`] = {
      doc_count: docs.length,
      nestedSelf: {
        doc_count: termsBuckets.reduce(
          (sum, b) => sum + (b.doc_count as number),
          0
        ),
        terms: {
          buckets: termsBuckets,
        },
      },
    };
  }

  return result;
}

const defaultConfig: Config = {
  pipelineDate: "2022-02-22",
  indexDateWorks: "2022-02-22",
  indexDateImages: "2022-02-22",
  worksIndex: "test-works-index",
  imagesIndex: "test-images-index",
  publicRootUrl: new URL("http://api.test/catalogue/v2"),
  defaultPageSize: 10,
  maxPageSize: 100,
};

type WorkDoc = {
  id: string;
  display: Work;
  type?: string;
  redirectTo?: string;
  filterableValues?: Record<string, unknown>;
  aggregatableValues?: Record<string, unknown>;
};

type ImageDoc = {
  id: string;
  display: Image;
  filterableValues?: Record<string, unknown>;
  aggregatableValues?: Record<string, unknown>;
  vectorValues?: { features?: number[] };
};

type MockedApiOptions = {
  works?: WorkDoc[];
  images?: ImageDoc[];
  config?: Partial<Config>;
};

// Create a combined mock client that handles both indices
const createMockedClient = (
  worksIndex: string,
  imagesIndex: string,
  works: WorkDoc[],
  images: ImageDoc[]
): Client => {
  const mock = new Mock();

  // Label-only aggregations (use .label field only, not .id)
  const labelOnlyAggregations = new Set([
    "genres.label",
    "subjects.label",
    "contributors.agent.label",
    "source.genres.label",
    "source.subjects.label",
    "source.contributors.agent.label",
  ]);

  // Mock search requests for works
  mock.add(
    {
      method: "POST",
      path: `/${worksIndex}/_search`,
    },
    (params) => {
      // Extract requested aggregations from the request body
      const body = params.body as
        | { aggs?: Record<string, unknown> }
        | undefined;
      const requestedAggs = body?.aggs ? Object.keys(body.aggs) : [];

      // Map ES aggregation names back to API request names
      const esNameToApiNames: Record<string, string[]> = {
        format: ["workType"],
        productionDates: ["production.dates"],
        genres: ["genres", "genres.label"],
        subjects: ["subjects", "subjects.label"],
        contributors: ["contributors.agent", "contributors.agent.label"],
        languages: ["languages"],
        license: ["items.locations.license"],
        availabilities: ["availabilities"],
      };

      // Determine the correct API name for each ES aggregation
      const aggRequests: string[] = [];
      for (const esName of requestedAggs) {
        // Skip global aggregations
        if (esName.endsWith("Global")) continue;

        const possibleApiNames = esNameToApiNames[esName];
        if (!possibleApiNames) continue;

        // Navigate the nested structure to find the terms aggregation
        // Structure: body.aggs.[esName].aggs.filtered.aggs.nested.aggs.terms
        const aggDef = body?.aggs?.[esName] as
          | Record<string, unknown>
          | undefined;
        const containerAggs = aggDef?.aggs as
          | Record<string, unknown>
          | undefined;
        const filteredAgg = containerAggs?.filtered as
          | Record<string, unknown>
          | undefined;
        const filteredAggs = filteredAgg?.aggs as
          | Record<string, unknown>
          | undefined;
        const outerNestedAgg = filteredAggs?.nested as
          | Record<string, unknown>
          | undefined;
        const outerNestedAggs = outerNestedAgg?.aggs as
          | Record<string, unknown>
          | undefined;
        const termsAgg = outerNestedAggs?.terms as
          | Record<string, unknown>
          | undefined;

        // Check if this terms agg has a labels sub-aggregation
        const hasLabelSubAgg = !!(
          termsAgg?.aggs as Record<string, unknown> | undefined
        )?.labels;
        const termsConfig = termsAgg?.terms as
          | Record<string, unknown>
          | undefined;
        const termsField = termsConfig?.field as string | undefined;
        const isLabelField = termsField?.endsWith(".label");

        if (isLabelField && !hasLabelSubAgg) {
          // This is a label-only aggregation
          const labelApiName = possibleApiNames.find((n) =>
            labelOnlyAggregations.has(n)
          );
          if (labelApiName) {
            aggRequests.push(labelApiName);
          }
        } else {
          // This is a labeled-id aggregation
          const idApiName = possibleApiNames.find(
            (n) => !labelOnlyAggregations.has(n)
          );
          if (idApiName) {
            aggRequests.push(idApiName);
          }
        }
      }

      const aggregations = buildMockAggregations(works, aggRequests);

      return {
        hits: {
          total: { value: works.length, relation: "eq" },
          max_score: 1.0,
          hits: works.map((doc, i) => ({
            _index: worksIndex,
            _id: doc.id,
            _score: 1.0 - i * 0.01,
            _source: {
              display: doc.display,
              type: doc.type ?? "Visible",
              filterableValues: doc.filterableValues ?? {},
              aggregatableValues: doc.aggregatableValues ?? {},
            },
          })),
        },
        aggregations,
      };
    }
  );

  // Mock search requests for images
  mock.add(
    {
      method: "POST",
      path: `/${imagesIndex}/_search`,
    },
    (params) => {
      // Extract requested aggregations from the request body
      const body = params.body as
        | { aggs?: Record<string, unknown> }
        | undefined;
      const requestedAggs = body?.aggs ? Object.keys(body.aggs) : [];

      // Map ES aggregation names back to API request names for images
      const esNameToApiNames: Record<string, string[]> = {
        license: ["locations.license"],
        contributors: [
          "source.contributors.agent",
          "source.contributors.agent.label",
        ],
        genres: ["source.genres", "source.genres.label"],
        subjects: ["source.subjects", "source.subjects.label"],
      };

      const aggRequests: string[] = [];
      for (const esName of requestedAggs) {
        if (esName.endsWith("Global")) continue;
        const possibleApiNames = esNameToApiNames[esName];
        if (!possibleApiNames) continue;

        // Navigate the nested structure to find the terms aggregation
        // Structure: body.aggs.[esName].aggs.filtered.aggs.nested.aggs.terms
        const aggDef = body?.aggs?.[esName] as
          | Record<string, unknown>
          | undefined;
        const containerAggs = aggDef?.aggs as
          | Record<string, unknown>
          | undefined;
        const filteredAgg = containerAggs?.filtered as
          | Record<string, unknown>
          | undefined;
        const filteredAggs = filteredAgg?.aggs as
          | Record<string, unknown>
          | undefined;
        const outerNestedAgg = filteredAggs?.nested as
          | Record<string, unknown>
          | undefined;
        const outerNestedAggs = outerNestedAgg?.aggs as
          | Record<string, unknown>
          | undefined;
        const termsAgg = outerNestedAggs?.terms as
          | Record<string, unknown>
          | undefined;

        const hasLabelSubAgg = !!(
          termsAgg?.aggs as Record<string, unknown> | undefined
        )?.labels;
        const termsConfig = termsAgg?.terms as
          | Record<string, unknown>
          | undefined;
        const termsField = termsConfig?.field as string | undefined;
        const isLabelField = termsField?.endsWith(".label");

        if (isLabelField && !hasLabelSubAgg) {
          const labelApiName = possibleApiNames.find((n) =>
            labelOnlyAggregations.has(n)
          );
          if (labelApiName) {
            aggRequests.push(labelApiName);
          }
        } else {
          const idApiName = possibleApiNames.find(
            (n) => !labelOnlyAggregations.has(n)
          );
          if (idApiName) {
            aggRequests.push(idApiName);
          }
        }
      }

      // For images, use the images aggregation builder
      const aggregations = buildMockImagesAggregations(images, aggRequests);

      return {
        hits: {
          total: { value: images.length, relation: "eq" },
          max_score: 1.0,
          hits: images.map((doc, i) => ({
            _index: imagesIndex,
            _id: doc.id,
            _score: 1.0 - i * 0.01,
            _source: {
              display: doc.display,
              filterableValues: doc.filterableValues ?? {},
              aggregatableValues: doc.aggregatableValues ?? {},
              vectorValues: doc.vectorValues ?? {},
            },
          })),
        },
        aggregations,
      };
    }
  );

  // Mock GET requests for works (single document)
  mock.add(
    {
      method: "GET",
      path: `/${worksIndex}/_doc/:id`,
    },
    (params) => {
      const id = (params.path as string).split("/").pop();
      const doc = works.find((d) => d.id === id);

      if (!doc) {
        return {
          statusCode: 404,
          body: {
            _index: worksIndex,
            _id: id,
            found: false,
          },
        };
      }

      return {
        _index: worksIndex,
        _id: doc.id,
        _version: 1,
        _seq_no: 0,
        _primary_term: 1,
        found: true,
        _source: {
          display: doc.display,
          type: doc.type ?? "Visible",
          redirectTo: doc.redirectTo,
          filterableValues: doc.filterableValues ?? {},
          aggregatableValues: doc.aggregatableValues ?? {},
        },
      };
    }
  );

  // Mock GET requests for images (single document)
  mock.add(
    {
      method: "GET",
      path: `/${imagesIndex}/_doc/:id`,
    },
    (params) => {
      const id = (params.path as string).split("/").pop();
      const doc = images.find((d) => d.id === id);

      if (!doc) {
        return {
          statusCode: 404,
          body: {
            _index: imagesIndex,
            _id: id,
            found: false,
          },
        };
      }

      return {
        _index: imagesIndex,
        _id: doc.id,
        _version: 1,
        _seq_no: 0,
        _primary_term: 1,
        found: true,
        _source: {
          display: doc.display,
          filterableValues: doc.filterableValues ?? {},
          aggregatableValues: doc.aggregatableValues ?? {},
          vectorValues: doc.vectorValues ?? {},
        },
      };
    }
  );

  // Mock cluster health
  mock.add(
    {
      method: "GET",
      path: "/_cluster/health",
    },
    () => ({
      cluster_name: "test-cluster",
      status: "green",
      number_of_nodes: 1,
      number_of_data_nodes: 1,
    })
  );

  return new Client({
    node: "http://localhost:9200",
    Connection: mock.getConnection(),
  });
};

export const mockedApi = (options: MockedApiOptions = {}) => {
  const config = { ...defaultConfig, ...options.config };

  const elastic = createMockedClient(
    config.worksIndex,
    config.imagesIndex,
    options.works ?? [],
    options.images ?? []
  );

  const app = createApp({ elastic }, config);

  return supertest.agent(app);
};

export const mockedWorksApi = (works: WorkDoc[], config?: Partial<Config>) =>
  mockedApi({ works, config });

export const mockedImagesApi = (images: ImageDoc[], config?: Partial<Config>) =>
  mockedApi({ images, config });
