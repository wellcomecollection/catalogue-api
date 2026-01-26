import { Client } from "@elastic/elasticsearch";
import Mock from "@elastic/elasticsearch-mock";
import { Work, Image, IndexedWork, IndexedImage } from "../../src/types";

type MockOptions = {
  index: string;
  docs: Array<{
    id: string;
    display: Work | Image;
    type?: string;
    filterableValues?: Record<string, unknown>;
    aggregatableValues?: Record<string, unknown>;
  }>;
};

export const mockedElasticsearchClient = ({
  index,
  docs,
}: MockOptions): Client => {
  const mock = new Mock();

  // Mock search requests
  mock.add(
    {
      method: "POST",
      path: `/${index}/_search`,
    },
    () => {
      return {
        hits: {
          total: { value: docs.length, relation: "eq" },
          max_score: 1.0,
          hits: docs.map((doc, i) => ({
            _index: index,
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
        aggregations: {},
      };
    }
  );

  // Mock GET requests (single document)
  mock.add(
    {
      method: "GET",
      path: `/${index}/_doc/:id`,
    },
    (params) => {
      const id = (params.path as string).split("/").pop();
      const doc = docs.find((d) => d.id === id);

      if (!doc) {
        return {
          statusCode: 404,
          body: {
            _index: index,
            _id: id,
            found: false,
          },
        };
      }

      return {
        _index: index,
        _id: doc.id,
        _version: 1,
        _seq_no: 0,
        _primary_term: 1,
        found: true,
        _source: {
          display: doc.display,
          type: doc.type ?? "Visible",
          filterableValues: doc.filterableValues ?? {},
          aggregatableValues: doc.aggregatableValues ?? {},
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

  // Mock aggregations for work types tally
  mock.add(
    {
      method: "POST",
      path: `/${index}/_search`,
    },
    (params) => {
      const body = params.body as { size?: number; aggs?: unknown };
      if (body.size === 0 && body.aggs) {
        // This is an aggregation-only request
        const typeCounts: Record<string, number> = {};
        for (const doc of docs) {
          const type = doc.type ?? "Visible";
          typeCounts[type] = (typeCounts[type] ?? 0) + 1;
        }

        return {
          hits: {
            total: { value: docs.length, relation: "eq" },
            hits: [],
          },
          aggregations: {
            workTypes: {
              buckets: Object.entries(typeCounts).map(([key, count]) => ({
                key,
                doc_count: count,
              })),
            },
          },
        };
      }

      // Regular search
      return {
        hits: {
          total: { value: docs.length, relation: "eq" },
          max_score: 1.0,
          hits: docs.map((doc, i) => ({
            _index: index,
            _id: doc.id,
            _score: 1.0 - i * 0.01,
            _source: {
              display: doc.display,
              type: doc.type ?? "Visible",
            },
          })),
        },
        aggregations: {},
      };
    }
  );

  return new Client({
    node: "http://localhost:9200",
    Connection: mock.getConnection(),
  });
};
