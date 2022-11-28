import { concept } from "./fixtures/concepts";
import { mockedApi } from "./fixtures/api";

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
});
