import { mockedWorksApi } from "./fixtures/api";
import { work, workWithProduction, indexedWork } from "./fixtures/works";

/**
 * Unit tests for works search and sort parameter validation.
 *
 * These tests verify parameter parsing, validation, and error responses.
 * Actual search/sort behavior is tested in integration tests.
 */
describe("GET /works search and sort - parameter validation", () => {
  describe("query parameter", () => {
    it("accepts query parameter", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=darwin");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multi-word query", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=origin%20species");
      expect(response.statusCode).toBe(200);
    });

    it("accepts special characters in query", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=darwin%27s%20notebook");
      expect(response.statusCode).toBe(200);
    });

    it("returns empty results for non-matching query", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=xyznonexistent");
      expect(response.statusCode).toBe(200);
      expect(response.body.results).toHaveLength(0);
    });

    it("combines query with filters", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=darwin&languages=eng");
      expect(response.statusCode).toBe(200);
    });

    it("combines query with pagination", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=darwin&page=2&pageSize=5");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("sort parameter validation", () => {
    it("accepts valid sort=production.dates", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?sort=production.dates");
      expect(response.statusCode).toBe(200);
    });

    it("accepts valid sort=items.locations.createdDate", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?sort=items.locations.createdDate");
      expect(response.statusCode).toBe(200);
    });

    it("accepts sortOrder=asc", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?sort=production.dates&sortOrder=asc"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts sortOrder=desc", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?sort=production.dates&sortOrder=desc"
      );
      expect(response.statusCode).toBe(200);
    });

    it("returns 400 for invalid sort field", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?sort=invalid");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
      expect(response.body.description).toContain("is not a valid value");
    });

    it("returns 400 for multiple invalid sort values", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?sort=foo,bar");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'foo'");
      expect(response.body.description).toContain("'bar'");
    });

    it("returns 400 for mixture of valid and invalid sort values", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?sort=foo,production.dates,bar");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'foo'");
      expect(response.body.description).toContain("'bar'");
    });

    it("returns 400 for invalid sortOrder", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?sort=production.dates&sortOrder=invalid"
      );
      expect(response.statusCode).toBe(400);
    });
  });

  describe("combined parameters", () => {
    it("accepts query + sort", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?query=darwin&sort=production.dates&sortOrder=desc"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts sort + filters", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?languages=eng&sort=production.dates&sortOrder=desc"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts sort + pagination", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?sort=production.dates&page=2&pageSize=3"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts all parameters together", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?query=darwin&languages=eng&sort=production.dates&sortOrder=desc&page=2&pageSize=5"
      );
      expect(response.statusCode).toBe(200);
    });
  });
});
