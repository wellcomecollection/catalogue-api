import { mockedWorksApi } from "./fixtures/api";

/**
 * Unit tests for works filter parameter validation.
 *
 * These tests verify parameter parsing, validation, and error responses.
 * Actual filter behavior is tested in integration tests.
 */
describe("GET /works filters - parameter validation", () => {
  describe("workType filter", () => {
    it("accepts single work type", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?workType=a");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multiple work types", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?workType=a,h");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("date range filters", () => {
    it("accepts production.dates.from", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?production.dates.from=1950-01-01");
      expect(response.statusCode).toBe(200);
    });

    it("accepts production.dates.to", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?production.dates.to=1950-12-31");
      expect(response.statusCode).toBe(200);
    });

    it("accepts date range (from and to)", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?production.dates.from=1900-01-01&production.dates.to=1950-12-31"
      );
      expect(response.statusCode).toBe(200);
    });

    it("rejects invalid date format for production.dates.from", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?production.dates.from=not-a-date");
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
      expect(response.body.description).toContain("YYYY-MM-DD");
    });

    it("rejects invalid date format for production.dates.to", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?production.dates.to=invalid");
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
    });
  });

  describe("language filter", () => {
    it("accepts single language", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?languages=eng");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multiple languages", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?languages=eng,fra,ger");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("genre filters", () => {
    it("accepts genre label", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?genres.label=Manuscripts");
      expect(response.statusCode).toBe(200);
    });

    it("accepts genre ID", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?genres=hqpfg3vq");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multiple genre labels", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?genres.label=Manuscripts,Letters");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("subject filters", () => {
    it("accepts subject label", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?subjects.label=Science");
      expect(response.statusCode).toBe(200);
    });

    it("accepts subject ID", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?subjects=n84f8864");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("contributor filters", () => {
    it("accepts contributor label", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?contributors.agent.label=Darwin");
      expect(response.statusCode).toBe(200);
    });

    it("accepts contributor ID", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?contributors.agent=xyz789");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("identifier filters", () => {
    it("accepts identifier value", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?identifiers=b12345678");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multiple identifiers", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?identifiers=b12345678,i99999999");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("item filters", () => {
    it("accepts item ID", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?items=item-123");
      expect(response.statusCode).toBe(200);
    });

    it("accepts item identifier", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?items.identifiers=i12345678");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("license filter", () => {
    it("accepts license", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?items.locations.license=cc-by");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multiple licenses", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?items.locations.license=cc-by,cc-by-nc"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("location type filter", () => {
    it("accepts location type", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?items.locations.locationType=iiif-presentation"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("access status filter", () => {
    it("accepts access status include", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?items.locations.accessConditions.status=open"
      );
      expect(response.statusCode).toBe(200);
    });

    it("accepts access status exclude with ! prefix", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?items.locations.accessConditions.status=!closed"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("availability filter", () => {
    it("accepts availability", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?availabilities=online");
      expect(response.statusCode).toBe(200);
    });

    it("accepts multiple availabilities", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?availabilities=online,in-library");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("partOf filter", () => {
    it("accepts partOf ID", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?partOf=parent-work-id");
      expect(response.statusCode).toBe(200);
    });

    it("accepts partOf title", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?partOf.title=Collection%20Name");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("type filter", () => {
    it("accepts work type", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?type=Collection");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("combined filters", () => {
    it("accepts multiple filters together", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?workType=a&languages=eng&production.dates.from=1900-01-01"
      );
      expect(response.statusCode).toBe(200);
    });

    it("combines filters with pagination", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?workType=a&languages=eng&page=2&pageSize=5"
      );
      expect(response.statusCode).toBe(200);
    });

    it("combines filters with search query", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get("/works?query=darwin&workType=a");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("special characters in filters", () => {
    it("handles special characters in filter values", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?contributors.agent.label=Robert%20.%3F%2B*%7C%7B%7D%3C%3E%26%40%5B%5D()%22%20Tables"
      );
      expect(response.statusCode).toBe(200);
    });

    it("handles commas in quoted filter values", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        '/works?genres.label="Psychology,%20Pathological"'
      );
      expect(response.statusCode).toBe(200);
    });

    it("handles spaces in quoted filter values (the PATCH CLAMPING bug case)", async () => {
      // This test covers the bug where subjects.label="PATCH CLAMPING" was failing
      // because quotes were not being stripped from the filter value
      const api = mockedWorksApi([]);
      const response = await api.get(
        "/works?subjects.label=%22PATCH%20CLAMPING%22"
      );
      expect(response.statusCode).toBe(200);
    });

    it("handles escaped quotes in filter values", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        '/works?genres.label="Darwin%20%5C%22Jones%5C%22,%20Charles"'
      );
      expect(response.statusCode).toBe(200);
    });

    it("handles multiple values with special characters", async () => {
      const api = mockedWorksApi([]);
      const response = await api.get(
        '/works?genres.label="Psychology,%20Pathological",Pamphlets'
      );
      expect(response.statusCode).toBe(200);
    });
  });
});
