import { mockedWorksApi } from "./fixtures/api";

/**
 * Unit tests for works aggregation parameter validation.
 *
 * These tests verify parameter parsing, validation, and error responses.
 * Actual aggregation behavior is tested in integration tests.
 */
describe("GET /works aggregations - parameter validation", () => {
  describe("valid aggregation parameters", () => {
    it("accepts workType aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=workType");
      expect(response.statusCode).toBe(200);
    });

    it("accepts genres.label aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=genres.label");
      expect(response.statusCode).toBe(200);
    });

    it("accepts genres aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=genres");
      expect(response.statusCode).toBe(200);
    });

    it("accepts production.dates aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=production.dates");
      expect(response.statusCode).toBe(200);
    });

    it("accepts subjects.label aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=subjects.label");
      expect(response.statusCode).toBe(200);
    });

    it("accepts subjects aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=subjects");
      expect(response.statusCode).toBe(200);
    });

    it("accepts languages aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=languages");
      expect(response.statusCode).toBe(200);
    });

    it("accepts contributors.agent.label aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=contributors.agent.label"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts contributors.agent aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=contributors.agent");
      expect(response.statusCode).toBe(200);
    });

    it("accepts items.locations.license aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=items.locations.license"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts availabilities aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=availabilities");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("multiple aggregations", () => {
    it("accepts multiple aggregations in a single request", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=workType,languages,genres.label"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts all aggregations at once", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=workType,languages,genres.label,genres,subjects.label,subjects,contributors.agent.label,contributors.agent,availabilities,items.locations.license,production.dates"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("aggregations with other parameters", () => {
    it("accepts aggregations with filters", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?languages=eng&aggregations=workType"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts aggregations with search query", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?query=darwin&aggregations=workType"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts aggregations with pagination", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=workType&page=2&pageSize=10"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts aggregations with sorting", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=workType&sort=production.dates"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("invalid aggregation parameters", () => {
    it("returns 400 for single invalid aggregation", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=invalid");
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
      expect(response.body.description).toContain("'invalid'");
    });

    it("returns 400 for multiple invalid aggregations", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?aggregations=invalid,unknown");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
      expect(response.body.description).toContain("'unknown'");
    });

    it("returns 400 for invalid aggregations mixed with valid ones", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?aggregations=workType,invalid,languages"
      );
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
    });
  });
});
