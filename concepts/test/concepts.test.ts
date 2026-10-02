import { concept } from "./fixtures/concepts";
import { mockedApi, previewCluster } from "./fixtures/api";

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

    it("returns a 404 for an unknown cluster", async () => {
      const response = await api.get("/concepts?elasticCluster=nope");
      expect(response.statusCode).toBe(404);
      expect(response.body.description).toBe(
        "Cluster 'nope' is not configured"
      );
    });
  });
});
