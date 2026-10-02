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
    const defaultConcept = concept({ id: "id-in-default-cluster" });
    const previewConcept = concept({ id: "id-in-additional-cluster" });
    const api = mockedApi([defaultConcept], [previewConcept]);

    it("reads the default cluster when absent", async () => {
      expect(
        (await api.get("/concepts/id-in-default-cluster")).body
      ).toStrictEqual(defaultConcept);
      expect(
        (await api.get("/concepts/id-in-additional-cluster")).statusCode
      ).toBe(404);
    });

    it("reads the named cluster when present", async () => {
      const response = await api.get(
        `/concepts/id-in-additional-cluster?elasticCluster=${previewCluster}`
      );
      expect(response.statusCode).toBe(200);
      expect(response.body).toStrictEqual(previewConcept);
      expect(
        (
          await api.get(
            `/concepts/id-in-default-cluster?elasticCluster=${previewCluster}`
          )
        ).statusCode
      ).toBe(404);
    });

    it("reads the default cluster when named default", async () => {
      const response = await api.get(
        "/concepts/id-in-default-cluster?elasticCluster=default"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body).toStrictEqual(defaultConcept);
    });

    it("returns a 404 for an unknown cluster", async () => {
      const response = await api.get(
        "/concepts/id-in-default-cluster?elasticCluster=nope"
      );
      expect(response.statusCode).toBe(404);
      expect(response.body).toStrictEqual({
        httpStatus: 404,
        label: "Not Found",
        description: "Cluster 'nope' is not configured",
        errorType: "http",
        type: "Error",
      });
    });

    // server.ts drops a cluster whose client fails to build but keeps its config
    it("returns a 404 for a configured cluster whose client was dropped", async () => {
      const droppedApi = mockedApi([defaultConcept], [previewConcept], {
        previewClientDropped: true,
      });
      const response = await droppedApi.get(
        `/concepts/id-in-additional-cluster?elasticCluster=${previewCluster}`
      );
      expect(response.statusCode).toBe(404);
      expect(response.body.description).toBe(
        `Cluster '${previewCluster}' is not configured`
      );
    });
  });
});
