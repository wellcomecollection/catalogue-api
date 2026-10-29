import { concept } from "./fixtures/concepts";
import { mockedApi, previewCluster } from "./fixtures/api";
import { SearchRequest } from "./fixtures/elasticsearch";

describe("GET /concepts", () => {
  it("returns a list of concepts", async () => {
    const testConcepts = Array.from({ length: 10 }).map((_, i) =>
      concept({ id: i.toString() })
    );
    const api = mockedApi(testConcepts);

    const response = await api.get("/concepts");
    expect(response.statusCode).toBe(200);
    expect(response.body.results).toStrictEqual(testConcepts);
  });

  it("sends the query, sort and pagination in the search request body", async () => {
    const requests: SearchRequest[] = [];
    const api = mockedApi([concept({ id: "a" })], [], {
      onSearch: (request) => requests.push(request),
    });

    const response = await api.get(
      "/concepts?query=test&identifiers.identifierType=test-type&page=2&pageSize=5"
    );
    expect(response.statusCode).toBe(200);
    expect(requests).toHaveLength(1);
    expect(requests[0].body).toMatchObject({
      from: 5,
      size: 5,
      _source: ["display"],
      track_total_hits: true,
      sort: ["_score"],
      query: {
        bool: {
          filter: [
            {
              term: { "query.identifiers.identifierType": "test-type" },
            },
          ],
        },
      },
    });
    expect(requests[0].querystring).not.toHaveProperty("from");
    expect(requests[0].querystring).not.toHaveProperty("sort");
  });

  it("returns only requested ids when id query param supplied, preserving order and duplicates", async () => {
    const testConcepts = [
      concept({ id: "a" }),
      concept({ id: "b" }),
      concept({ id: "c" }),
    ];
    const api = mockedApi(testConcepts);

    const response = await api.get("/concepts?id=b,a,b,missing");
    expect(response.statusCode).toBe(200);
    // Should include b, a, b (duplicate) but not missing
    // @ts-ignore - test helper, dynamic typing acceptable here
    const resultIds = response.body.results.map((c) => c.id);
    expect(resultIds).toStrictEqual(["b", "a", "b"]);
    expect(response.body.totalResults).toBe(3);
  });

  describe("the elasticCluster parameter", () => {
    const defaultConcept = concept({ id: "id-in-default-cluster" });
    const previewConcept = concept({ id: "id-in-additional-cluster" });
    const api = mockedApi([defaultConcept], [previewConcept]);

    it("searches the default cluster when absent", async () => {
      const response = await api.get("/concepts");
      expect(response.statusCode).toBe(200);
      expect(response.body.results).toStrictEqual([defaultConcept]);
    });

    it("searches the named cluster when present", async () => {
      const response = await api.get(
        `/concepts?elasticCluster=${previewCluster}`
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.results).toStrictEqual([previewConcept]);
    });

    it("fetches ids from the named cluster when present", async () => {
      const response = await api.get(
        `/concepts?id=id-in-default-cluster,id-in-additional-cluster&elasticCluster=${previewCluster}`
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.results).toStrictEqual([previewConcept]);
    });

    it("fetches ids from the default cluster when named default", async () => {
      const response = await api.get(
        "/concepts?id=id-in-default-cluster,id-in-additional-cluster&elasticCluster=default"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.results).toStrictEqual([defaultConcept]);
    });

    it("returns a 404 for an unknown cluster", async () => {
      const response = await api.get("/concepts?elasticCluster=nope");
      expect(response.statusCode).toBe(404);
      expect(response.body.description).toBe(
        "Cluster 'nope' is not configured"
      );
    });
  });
});
