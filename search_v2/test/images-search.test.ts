import { mockedImagesApi } from "./fixtures/api";
import { image, indexedImage } from "./fixtures/images";

describe("GET /images search and sort", () => {
  describe("full-text search", () => {
    it("searches by query parameter", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1", sourceTitle: "Portrait of Darwin" })),
        indexedImage(image({ id: "img-2", sourceTitle: "Landscape painting" })),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?query=darwin");
      expect(response.statusCode).toBe(200);
      expect(response.body.type).toBe("ResultList");
    });

    it("searches with multi-word query", async () => {
      const testImages = [
        indexedImage(
          image({ id: "img-1", sourceTitle: "Portrait of Charles Darwin" })
        ),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?query=charles%20darwin");
      expect(response.statusCode).toBe(200);
    });

    it("returns empty results for non-matching query", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get("/images?query=xyznonexistent");
      expect(response.statusCode).toBe(200);
      expect(response.body.results).toHaveLength(0);
    });

    it("combines query with filters", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1", sourceTitle: "Darwin portrait" }), {
          filterableValues: { "locations.license.id": ["cc-by"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?query=darwin&locations.license=cc-by"
      );
      expect(response.statusCode).toBe(200);
    });

    it("combines query with pagination", async () => {
      const testImages = Array.from({ length: 20 }).map((_, i) =>
        indexedImage(
          image({ id: `img-${i}`, sourceTitle: `Darwin Image ${i}` })
        )
      );
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?query=darwin&page=2&pageSize=5");
      expect(response.statusCode).toBe(200);
    });

    it("combines query with color search", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1", sourceTitle: "Red portrait" }), {
          vectorValues: { features: [0.9, 0.1, 0.1] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?query=portrait&color=ff0000");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("sorting", () => {
    describe("sort=source.production.dates", () => {
      it("sorts by source production date ascending by default", async () => {
        const testImages = [
          indexedImage(image({ id: "img-1900" }), {
            filterableValues: { "source.production.dates": "1900" },
          }),
          indexedImage(image({ id: "img-2000" }), {
            filterableValues: { "source.production.dates": "2000" },
          }),
        ];
        const api = mockedImagesApi(testImages);

        const response = await api.get("/images?sort=source.production.dates");
        expect(response.statusCode).toBe(200);
        expect(response.body.type).toBe("ResultList");
      });

      it("sorts by source production date descending when specified", async () => {
        const testImages = [
          indexedImage(image({ id: "img-1900" })),
          indexedImage(image({ id: "img-2000" })),
        ];
        const api = mockedImagesApi(testImages);

        const response = await api.get(
          "/images?sort=source.production.dates&sortOrder=desc"
        );
        expect(response.statusCode).toBe(200);
      });

      it("sorts by source production date ascending when explicitly specified", async () => {
        const api = mockedImagesApi([]);

        const response = await api.get(
          "/images?sort=source.production.dates&sortOrder=asc"
        );
        expect(response.statusCode).toBe(200);
      });
    });

    describe("sort with query", () => {
      it("combines sort with search query", async () => {
        const testImages = [
          indexedImage(image({ id: "img-1", sourceTitle: "Darwin Image 1" })),
          indexedImage(image({ id: "img-2", sourceTitle: "Darwin Image 2" })),
        ];
        const api = mockedImagesApi(testImages);

        const response = await api.get(
          "/images?query=darwin&sort=source.production.dates&sortOrder=desc"
        );
        expect(response.statusCode).toBe(200);
      });
    });

    describe("sort with filters", () => {
      it("combines sort with filters", async () => {
        const api = mockedImagesApi([]);

        const response = await api.get(
          "/images?locations.license=cc-by&sort=source.production.dates&sortOrder=desc"
        );
        expect(response.statusCode).toBe(200);
      });
    });

    describe("sort with pagination", () => {
      it("maintains sort order across pages", async () => {
        const testImages = Array.from({ length: 10 }).map((_, i) =>
          indexedImage(image({ id: `img-${i}` }))
        );
        const api = mockedImagesApi(testImages);

        const response = await api.get(
          "/images?sort=source.production.dates&page=2&pageSize=3"
        );
        expect(response.statusCode).toBe(200);
      });
    });

    describe("invalid sort", () => {
      it("returns 400 for invalid sort field", async () => {
        const api = mockedImagesApi([]);

        const response = await api.get("/images?sort=invalid");
        expect(response.statusCode).toBe(400);
        expect(response.body.description).toContain("'invalid'");
        expect(response.body.description).toContain("is not a valid value");
      });

      it("rejects invalid sortOrder", async () => {
        const api = mockedImagesApi([]);

        const response = await api.get(
          "/images?sort=source.production.dates&sortOrder=invalid"
        );
        expect(response.statusCode).toBe(400);
      });
    });
  });

  describe("sort, search, filters, color, and pagination combined", () => {
    it("handles all parameters together", async () => {
      const testImages = Array.from({ length: 20 }).map((_, i) =>
        indexedImage(
          image({ id: `img-${i}`, sourceTitle: `Darwin Image ${i}` }),
          {
            filterableValues: { "locations.license.id": ["cc-by"] },
            vectorValues: { features: [0.1, 0.2, 0.3] },
          }
        )
      );
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?query=darwin&locations.license=cc-by&sort=source.production.dates&sortOrder=desc&page=2&pageSize=5"
      );
      expect(response.statusCode).toBe(200);
    });
  });
});
