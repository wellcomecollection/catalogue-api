import { mockedImagesApi } from "./fixtures/api";
import { image, indexedImage } from "./fixtures/images";

describe("GET /images aggregations", () => {
  describe("locations.license aggregation", () => {
    it("returns locations.license aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "locations.license.id": "cc-by" },
        }),
        indexedImage(image({ id: "img-2" }), {
          aggregatableValues: { "locations.license.id": "cc-by-nc" },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?aggregations=locations.license");
      expect(response.statusCode).toBe(200);
      expect(response.body.type).toBe("ResultList");
    });
  });

  describe("source.contributors.agent.label aggregation", () => {
    it("returns source.contributors.agent.label aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "source.contributors.agent.label": ["Darwin"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?aggregations=source.contributors.agent.label"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source.contributors.agent aggregation", () => {
    it("returns source.contributors.agent aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "source.contributors.agent.id": ["xyz789"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?aggregations=source.contributors.agent"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source.genres.label aggregation", () => {
    it("returns source.genres.label aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "source.genres.label": ["Photographs"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?aggregations=source.genres.label"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source.genres aggregation", () => {
    it("returns source.genres aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "source.genres.id": ["hqpfg3vq"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?aggregations=source.genres");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source.subjects.label aggregation", () => {
    it("returns source.subjects.label aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "source.subjects.label": ["Medicine"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?aggregations=source.subjects.label"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("source.subjects aggregation", () => {
    it("returns source.subjects aggregation when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: { "source.subjects.id": ["n84f8864"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get("/images?aggregations=source.subjects");
      expect(response.statusCode).toBe(200);
    });
  });

  describe("multiple aggregations", () => {
    it("returns multiple aggregations when requested", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          aggregatableValues: {
            "locations.license.id": "cc-by",
            "source.genres.label": ["Photographs"],
            "source.subjects.label": ["Medicine"],
          },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?aggregations=locations.license,source.genres.label,source.subjects.label"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("aggregations with filters", () => {
    it("returns aggregations with active filters", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          filterableValues: { "locations.license.id": ["cc-by"] },
          aggregatableValues: { "source.genres.label": ["Photographs"] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?locations.license=cc-by&aggregations=source.genres.label"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("aggregations with search query", () => {
    it("returns aggregations with search query", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1", sourceTitle: "Portrait of Darwin" })),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?query=darwin&aggregations=locations.license"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("aggregations with color filter", () => {
    it("returns aggregations with color filter", async () => {
      const testImages = [
        indexedImage(image({ id: "img-1" }), {
          vectorValues: { features: [0.1, 0.2, 0.3] },
        }),
      ];
      const api = mockedImagesApi(testImages);

      const response = await api.get(
        "/images?color=ff0000&aggregations=locations.license"
      );
      expect(response.statusCode).toBe(200);
    });
  });

  describe("invalid aggregations", () => {
    it("returns 400 for invalid aggregation values", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get("/images?aggregations=invalid,unknown");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
      expect(response.body.description).toContain("'unknown'");
    });

    it("returns 400 for invalid aggregations mixed with valid ones", async () => {
      const api = mockedImagesApi([]);

      const response = await api.get(
        "/images?aggregations=locations.license,invalid,source.genres"
      );
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
    });
  });
});
