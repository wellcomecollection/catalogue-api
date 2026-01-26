import { mockedApi } from "./fixtures/api";

describe("Pagination", () => {
  describe("page parameter validation", () => {
    it("returns 400 for page less than 1", async () => {
      const api = mockedApi();

      const response = await api.get("/works?page=0");
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
      expect(response.body.description).toContain("page");
    });

    it("returns 400 for negative page", async () => {
      const api = mockedApi();

      const response = await api.get("/works?page=-1");
      expect(response.statusCode).toBe(400);
    });

    it("accepts page=1", async () => {
      const api = mockedApi();

      const response = await api.get("/works?page=1");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("pageSize parameter validation", () => {
    it("returns 400 for pageSize greater than 100", async () => {
      const api = mockedApi();

      const response = await api.get("/works?pageSize=101");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("pageSize");
    });

    it("returns 400 for pageSize less than 1", async () => {
      const api = mockedApi();

      const response = await api.get("/works?pageSize=0");
      expect(response.statusCode).toBe(400);
    });

    it("accepts pageSize=100", async () => {
      const api = mockedApi();

      const response = await api.get("/works?pageSize=100");
      expect(response.statusCode).toBe(200);
    });

    it("accepts pageSize=1", async () => {
      const api = mockedApi();

      const response = await api.get("/works?pageSize=1");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("pagination links", () => {
    it("uses public root URL for pagination links", async () => {
      const api = mockedApi({
        works: Array.from({ length: 30 }).map((_, i) => ({
          id: `work-${i}`,
          display: {
            id: `work-${i}`,
            title: `Work ${i}`,
            alternativeTitles: [],
            availabilities: [],
            type: "Work" as const,
          },
        })),
        config: {
          publicRootUrl: new URL("https://api.example.com/catalogue/v2"),
        },
      });

      const response = await api.get("/works?page=2&pageSize=10");
      expect(response.statusCode).toBe(200);
      expect(response.body.nextPage).toContain("https://api.example.com");
      expect(response.body.prevPage).toContain("https://api.example.com");
    });
  });
});
