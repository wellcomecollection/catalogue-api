import { mockedWorksApi } from "./fixtures/api";
import { work, indexedWork } from "./fixtures/works";

describe("GET /works/:id redirects", () => {
  it("returns 302 redirect for redirected works", async () => {
    const testWork = work({ id: "redirect-source" });
    const api = mockedWorksApi([
      indexedWork(testWork, {
        type: "Redirected",
        redirectTo: "redirect-target",
      }),
    ]);

    const response = await api.get("/works/redirect-source");
    expect(response.statusCode).toBe(302);
    expect(response.headers.location).toContain("/works/redirect-target");
  });

  it("preserves query parameters on redirect", async () => {
    const testWork = work({ id: "redirect-source" });
    const api = mockedWorksApi([
      indexedWork(testWork, {
        type: "Redirected",
        redirectTo: "redirect-target",
      }),
    ]);

    const response = await api.get(
      "/works/redirect-source?include=identifiers"
    );
    expect(response.statusCode).toBe(302);
    expect(response.headers.location).toContain("/works/redirect-target");
    expect(response.headers.location).toContain("include=identifiers");
  });

  it("returns 404 for redirected work without redirectTo", async () => {
    const testWork = work({ id: "redirect-broken" });
    const api = mockedWorksApi([
      indexedWork(testWork, { type: "Redirected" }), // No redirectTo
    ]);

    const response = await api.get("/works/redirect-broken");
    expect(response.statusCode).toBe(404);
  });
});
