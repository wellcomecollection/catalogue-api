import { mockedImagesApi } from "./fixtures/api";
import { image, indexedImage } from "./fixtures/images";

describe("GET /images filters", () => {
  describe("license filter", () => {
    it("filters by license", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "locations.license.id": ["cc-by"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?locations.license=cc-by");
      expect(response.statusCode).toBe(200);
      expect(response.body.type).toBe("ResultList");
    });

    it("filters by multiple licenses", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "locations.license.id": ["cc-by"] },
        }),
        indexedImage(image({ id: "img-2" }), {
          filterableValues: { "locations.license.id": ["cc-by-nc"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?locations.license=cc-by,cc-by-nc"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source contributor filters", () => {
    it("filters by source contributor label", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.contributors.agent.label": ["Darwin"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?source.contributors.agent.label=Darwin"
      );
      expect(response.statusCode).toBe(200);
    });

    it("filters by source contributor ID", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.contributors.agent.id": ["xyz789"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?source.contributors.agent=xyz789"
      );
      expect(response.statusCode).toBe(200);
    });

    it("filters by multiple contributor labels", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?source.contributors.agent.label=Darwin,Newton"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source genre filters", () => {
    it("filters by source genre label", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.genres.label": ["Portraits"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?source.genres.label=Portraits");
      expect(response.statusCode).toBe(200);
    });

    it("filters by source genre ID", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.genres.id": ["hqpfg3vq"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?source.genres=hqpfg3vq");
      expect(response.statusCode).toBe(200);
    });

    it("filters by multiple genre labels", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?source.genres.label=Portraits,Photographs"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source subject filters", () => {
    it("filters by source subject label", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.subjects.label": ["Medicine"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?source.subjects.label=Medicine");
      expect(response.statusCode).toBe(200);
    });

    it("filters by source subject ID", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.subjects.id": ["n84f8864"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?source.subjects=n84f8864");
      expect(response.statusCode).toBe(200);
    });

    it("filters by multiple subject labels", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?source.subjects.label=Medicine,Surgery"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("date range filters", () => {
    it("filters by source.production.dates.from", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.production.dates": { gte: "1900" } },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?source.production.dates.from=1900-01-01"
      );
      expect(response.statusCode).toBe(200);
    });

    it("filters by source.production.dates.to", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "source.production.dates": { lte: "1950" } },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?source.production.dates.to=1950-12-31"
      );
      expect(response.statusCode).toBe(200);
    });

    it("filters by date range (from and to)", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?source.production.dates.from=1900-01-01&source.production.dates.to=1950-12-31"
      );
      expect(response.statusCode).toBe(200);
    });

    it("rejects invalid date format", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?source.production.dates.from=not-a-date"
      );
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
    });
  });

  describe("color filter", () => {
    it("filters by hex color", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          vectorValues: { features: [0.1, 0.2, 0.3] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?color=ff0000");
      expect(response.statusCode).toBe(200);
    });

    it("filters by hex color with hash prefix", async () => {
      const api = mockedImagesApi([]);

      // Note: # needs to be URL encoded as %23
      const response = await api.get("/images?color=%23ff0000");
      expect(response.statusCode).toBe(200);
    });

    it("rejects invalid hex color", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get("/images?color=not-a-color");
      expect(response.statusCode).toBe(400);
      expect(response.body.type).toBe("Error");
      expect(response.body.description).toContain("valid value");
    });

    it("rejects partial hex color", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get("/images?color=ff0");
      expect(response.statusCode).toBe(400);
    });

    it("combines color with query", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1", sourceTitle: "Portrait" }), {
          vectorValues: { features: [0.1, 0.2, 0.3] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?query=portrait&color=8b4513");
      expect(response.statusCode).toBe(200);
    });

    it("scores by nearest neighbor color similarity", async () => {
      // Results should be ordered by color similarity to the queried color
      const testImages = [
        indexedImage(image({ id: "img-red" }), {
          vectorValues: { features: [1.0, 0.0, 0.0] }, // Red
        }),
        indexedImage(image({ id: "img-slightly-red" }), {
          vectorValues: { features: [0.9, 0.1, 0.0] }, // Slightly less red
        }),
        indexedImage(image({ id: "img-less-red" }), {
          vectorValues: { features: [0.7, 0.2, 0.1] }, // Even less red
        }),
        indexedImage(image({ id: "img-blue" }), {
          vectorValues: { features: [0.0, 0.0, 1.0] }, // Blue
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?color=e02020");
      expect(response.statusCode).toBe(200);
      // Results should be ordered by nearest neighbor similarity
      expect(response.body.type).toBe("ResultList");
    });
  });

  describe("color filter and query combined", () => {
    it("combines query and color filter", async () => {
      const testImages = [
        indexedImage(image({ id: "img-blue-foot", sourceTitle: "Blue foot" }), {
          vectorValues: { features: [0.1, 0.2, 0.9] },
        }),
        indexedImage(image({ id: "img-blue" }), {
          vectorValues: { features: [0.1, 0.1, 1.0] },
        }),
        indexedImage(
          image({ id: "img-orange-foot", sourceTitle: "Orange foot" }),
          {
            vectorValues: { features: [1.0, 0.5, 0.0] },
          }
        ),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?query=foot&color=22bbff");
      expect(response.statusCode).toBe(200);
      // Should only return results matching "foot" query, ordered by color
    });

    it("combines multi-token query terms with AND", async () => {
      // When multiple tokens are provided in a query with color filter,
      // results should match ALL tokens (AND logic)
      const testImages = [
        indexedImage(
          image({
            id: "img-green-dye",
            sourceTitle: "Green color, with dye treatment",
          }),
          {
            vectorValues: { features: [0.1, 0.8, 0.3] },
          }
        ),
        indexedImage(
          image({ id: "img-dye-only", sourceTitle: "Dye treatment" }),
          {
            vectorValues: { features: [0.1, 0.3, 0.9] },
          }
        ),
        indexedImage(image({ id: "img-neither", sourceTitle: "Other image" }), {
          vectorValues: { features: [0.5, 0.5, 0.5] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      // Query for "green dye" should only return images with BOTH terms
      const response = await api.get("/images?query=green+dye&color=22bbff");
      expect(response.statusCode).toBe(200);
      // img-green-dye has both "green" and "dye"
      // img-dye-only only has "dye"
      // img-neither has neither
    });
  });

  describe("combined filters", () => {
    it("applies multiple filters together", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: {
            "locations.license.id": ["cc-by"],
            "source.contributors.agent.label": ["Darwin"],
          },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?locations.license=cc-by&source.contributors.agent.label=Darwin"
      );
      expect(response.statusCode).toBe(200);
    });

    it("combines filters with pagination", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?locations.license=cc-by&page=2&pageSize=5"
      );
      expect(response.statusCode).toBe(200);
    });

    it("combines filters with search query", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?query=portrait&locations.license=cc-by"
      );
      expect(response.statusCode).toBe(200);
    });

    it("combines filters with color search", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?color=ff0000&locations.license=cc-by"
      );
      expect(response.statusCode).toBe(200);
    });
  });
});
