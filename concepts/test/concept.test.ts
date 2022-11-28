import { mockedApi } from "./fixtures/api";
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
});
