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
});
