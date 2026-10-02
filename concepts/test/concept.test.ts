import { mockedApi, previewCluster } from "./fixtures/api";
import { concept } from "./fixtures/concepts";

describe("GET /concepts/:id", () => {
  it("returns a concept for the given ID", async () => {
    const testConcept = concept();
    const api = mockedApi([testConcept]);

    const response = await api.get(`/concepts/${testConcept.id}`);
    expect(response.statusCode).toBe(200);
    expect(response.body).toStrictEqual(testConcept);
  });

  it("returns a 404 if no concept for the given ID exists", async () => {
    const api = mockedApi([]);

    const response = await api.get(`/concepts/blahblah`);
    expect(response.statusCode).toBe(404);
  });

  describe("the elasticCluster parameter", () => {
    const defaultConcept = concept({ id: "default" });
    const previewConcept = concept({ id: "preview" });
    const api = mockedApi([defaultConcept], [previewConcept]);

    it("reads the default cluster when absent", async () => {
      expect((await api.get("/concepts/default")).body).toStrictEqual(
        defaultConcept
      );
      expect((await api.get("/concepts/preview")).statusCode).toBe(404);
    });

    it("reads the named cluster when present", async () => {
      const response = await api.get(
        `/concepts/preview?elasticCluster=${previewCluster}`
      );
      expect(response.statusCode).toBe(200);
      expect(response.body).toStrictEqual(previewConcept);
      expect(
        (await api.get(`/concepts/default?elasticCluster=${previewCluster}`))
          .statusCode
      ).toBe(404);
    });

    it("returns a 404 for an unknown cluster", async () => {
      const response = await api.get("/concepts/default?elasticCluster=nope");
      expect(response.statusCode).toBe(404);
      expect(response.body).toStrictEqual({
        httpStatus: 404,
        label: "Not Found",
        description: "Cluster 'nope' is not configured",
        errorType: "http",
        type: "Error",
      });
    });
  });
});
