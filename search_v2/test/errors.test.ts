import { mockedApi, mockedWorksApi } from "./fixtures/api";
import { work, indexedWork } from "./fixtures/works";

describe("API Errors", () => {
  describe("Bad Request (400)", () => {
    it("returns error for invalid page parameter", async () => {
      const api = mockedApi();

      const response = await api.get("/works?page=invalid");
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
      expect(response.body.errorType).toBe("http");
      expect(response.body.httpStatus).toBe(400);
      expect(response.body.label).toBe("Bad Request");
    });

    it("returns error for invalid pageSize parameter", async () => {
      const api = mockedApi();

      const response = await api.get("/works?pageSize=invalid");
      expect(response.statusCode).toBe(400);
    });

    it("returns error for invalid date format", async () => {
      const api = mockedApi();

      const response = await api.get("/works?production.dates.from=not-a-date");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("YYYY-MM-DD");
    });

    it("returns error for date with year too large (> 9999)", async () => {
      // Regression test: ES fails to parse dates like "+011860-01-01"
      // The validation should catch invalid date formats
      const testWorks = [indexedWork(work({ id: "work-1" }))];
      const api = mockedWorksApi(testWorks);

      const response = await api.get(
        "/works?production.dates.from=%2B011860-01-01"
      );
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toMatch(/YYYY-MM-DD|year|9999|Date/i);
    });

    it("returns error for invalid color format", async () => {
      const api = mockedApi();

      const response = await api.get("/images?color=not-a-color");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("hex");
    });

    describe("pageSize validation", () => {
      it("returns error for pageSize just over maximum (101)", async () => {
        const api = mockedApi();

        const response = await api.get("/works?pageSize=101");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toMatch(/100|pageSize/);
      });

      it("returns error for pageSize of zero", async () => {
        const api = mockedApi();

        const response = await api.get("/works?pageSize=0");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toMatch(/1|pageSize/);
      });

      it("returns error for large pageSize (1000)", async () => {
        const api = mockedApi();

        const response = await api.get("/works?pageSize=1000");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toMatch(/100|pageSize/);
      });

      it("returns error for negative pageSize", async () => {
        const api = mockedApi();

        const response = await api.get("/works?pageSize=-50");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toMatch(/1|pageSize/);
      });
    });

    describe("page validation", () => {
      it("returns error for page 0", async () => {
        const api = mockedApi();

        const response = await api.get("/works?page=0");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toContain("greater than");
      });

      it("returns error for negative page", async () => {
        const api = mockedApi();

        const response = await api.get("/works?page=-50");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toContain("greater than");
      });
    });

    describe("deep pagination limits", () => {
      it("returns error for page requesting beyond 10000 results", async () => {
        const testWorks = [indexedWork(work({ id: "work-1" }))];
        const api = mockedWorksApi(testWorks);

        const response = await api.get("/works?page=10000");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toContain("10000");
      });

      it("returns error for page*pageSize overflow", async () => {
        const testWorks = [indexedWork(work({ id: "work-1" }))];
        const api = mockedWorksApi(testWorks);

        const response = await api.get("/works?page=2000000000&pageSize=100");
        expect(response.statusCode).toBe(400);
      });

      it("returns error for 101st page with 100 results per page", async () => {
        const testWorks = [indexedWork(work({ id: "work-1" }))];
        const api = mockedWorksApi(testWorks);

        const response = await api.get("/works?page=101&pageSize=100");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toContain("10000");
      });
    });

    describe("multiple errors", () => {
      it("returns multiple errors if there are multiple invalid parameters", async () => {
        const api = mockedApi();

        const response = await api.get("/works?pageSize=-60&page=-50");
        expect(response.statusCode).toBe(400);
        // Should mention both page and pageSize errors
        expect(response.body.description).toMatch(/page|pageSize/);
      });
    });
  });

  describe("Not Found (404)", () => {
    it("returns 404 for non-existent work", async () => {
      const api = mockedApi();

      const response = await api.get("/works/does-not-exist");
      expect(response.statusCode).toBe(404);
      expect(response.body.type).toBe("Error");
      expect(response.body.label).toBe("Not Found");
      expect(response.body.description).toContain("does-not-exist");
    });

    it("returns 404 for non-existent image", async () => {
      const api = mockedApi();

      const response = await api.get("/images/does-not-exist");
      expect(response.statusCode).toBe(404);
      expect(response.body.label).toBe("Not Found");
    });

    it("returns 404 for work with malformed identifier", async () => {
      const api = mockedApi();

      const response = await api.get("/works/zd224ncv]");
      expect(response.statusCode).toBe(404);
      expect(response.body.label).toBe("Not Found");
    });
  });

  describe("Gone (410)", () => {
    it("returns 410 for deleted work", async () => {
      const testWork = work({ id: "deleted-work" });
      const api = mockedWorksApi([indexedWork(testWork, { type: "Deleted" })]);

      const response = await api.get("/works/deleted-work");
      expect(response.statusCode).toBe(410);
      expect(response.body.label).toBe("Gone");
    });

    it("returns 410 for invisible work", async () => {
      const testWork = work({ id: "invisible-work" });
      const api = mockedWorksApi([
        indexedWork(testWork, { type: "Invisible" }),
      ]);

      const response = await api.get("/works/invisible-work");
      expect(response.statusCode).toBe(410);
      expect(response.body.label).toBe("Gone");
    });
  });

  describe("Error response format", () => {
    it("has correct error structure", async () => {
      const api = mockedApi();

      const response = await api.get("/works/nonexistent");
      expect(response.body).toMatchObject({
        httpStatus: 404,
        label: expect.any(String),
        description: expect.any(String),
        errorType: "http",
        type: "Error",
      });
    });
  });
});
