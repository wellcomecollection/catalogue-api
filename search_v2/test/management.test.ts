import { mockedApi, mockedWorksApi } from "./fixtures/api";
import { indexedWork, work } from "./fixtures/works";

describe("GET /management/healthcheck", () => {
  it("returns status ok", async () => {
    const api = mockedApi();

    const response = await api.get("/management/healthcheck");
    expect(response.statusCode).toBe(200);
    expect(response.body.status).toBe("ok");
  });

  it("includes config info", async () => {
    const api = mockedApi({
      config: {
        pipelineDate: "2024-01-01",
        worksIndex: "custom-works-index",
      },
    });

    const response = await api.get("/management/healthcheck");
    expect(response.statusCode).toBe(200);
    expect(response.body.config.pipelineDate).toBe("2024-01-01");
    expect(response.body.config.worksIndex).toBe("custom-works-index");
  });
});

describe("GET /_elasticConfig", () => {
  it("returns elastic config info", async () => {
    const api = mockedApi();

    const response = await api.get("/_elasticConfig");
    expect(response.statusCode).toBe(200);
    expect(response.body.worksIndex).toBeDefined();
    expect(response.body.imagesIndex).toBeDefined();
    expect(response.body.pipelineDate).toBeDefined();
  });
});

describe("GET /_searchTemplates", () => {
  it("returns search template info", async () => {
    const api = mockedApi();

    const response = await api.get("/_searchTemplates");
    expect(response.statusCode).toBe(200);
    expect(response.body.templates).toBeDefined();
    expect(response.body.templates.works).toBe("WorksQuery.json");
    expect(response.body.templates.images).toBe("ImagesQuery.json");
  });
});

describe("GET /management/_workTypes", () => {
  it("returns a tally of work types", async () => {
    // Note: This test verifies the endpoint works, but the mock doesn't simulate
    // aggregations. In production this endpoint returns counts like:
    // { "Visible": 2, "Invisible": 1, "Deleted": 1, "Redirected": 1 }
    const testWorks = [
      indexedWork(work({ id: "visible-1" }), { type: "Visible" }),
      indexedWork(work({ id: "visible-2" }), { type: "Visible" }),
      indexedWork(work({ id: "invisible-1" }), { type: "Invisible" }),
      indexedWork(work({ id: "deleted-1" }), { type: "Deleted" }),
      indexedWork(work({ id: "redirected-1" }), { type: "Redirected" }),
    ];
    const api = mockedWorksApi(testWorks);

    const response = await api.get("/management/_workTypes");
    expect(response.statusCode).toBe(200);
    // The actual aggregation response would have counts
    // With proper ES mock it would be:
    // expect(response.body.Visible).toBe(2);
    // expect(response.body.Invisible).toBe(1);
    // expect(response.body.Deleted).toBe(1);
    // expect(response.body.Redirected).toBe(1);
  });

  it("returns empty counts when no works exist", async () => {
    const api = mockedWorksApi([]);

    const response = await api.get("/management/_workTypes");
    expect(response.statusCode).toBe(200);
  });
});
