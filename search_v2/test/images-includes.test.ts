import { mockedImagesApi } from "./fixtures/api";
import { image, imageWithContributors, indexedImage } from "./fixtures/images";
import { Image } from "../src/types";

// Helper to create images with various includes
const imageWithLanguages = (
  options: { id?: string; sourceTitle?: string } = {}
): Image => {
  const baseImage = image(options);
  return {
    ...baseImage,
    source: {
      ...baseImage.source,
      languages: [
        {
          id: "eng",
          label: "English",
          type: "Language",
        },
      ],
    },
  };
};

const imageWithGenres = (
  options: { id?: string; sourceTitle?: string } = {}
): Image => {
  const baseImage = image(options);
  return {
    ...baseImage,
    source: {
      ...baseImage.source,
      genres: [
        {
          label: "Photographs",
          concepts: [
            {
              id: "hqpfg3vq",
              label: "Photographs",
              type: "Concept",
            },
          ],
          type: "Genre",
        },
      ],
    },
  };
};

const imageWithSubjects = (
  options: { id?: string; sourceTitle?: string } = {}
): Image => {
  const baseImage = image(options);
  return {
    ...baseImage,
    source: {
      ...baseImage.source,
      subjects: [
        {
          id: "subject-1",
          label: "Medicine",
          concepts: [
            {
              id: "n84f8864",
              label: "Medicine",
              type: "Concept",
            },
          ],
          type: "Subject",
        },
      ],
    },
  };
};

describe("GET /images includes", () => {
  describe("include=source.contributors", () => {
    it("includes source.contributors in list response", async () => {
      const testImage = imageWithContributors({ id: "img-with-contributors" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images?include=source.contributors");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].source.contributors).toBeDefined();
      expect(response.body.results[0].source.contributors).toHaveLength(1);
    });

    it("includes source.contributors in single image response", async () => {
      const testImage = imageWithContributors({ id: "img-with-contributors" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get(
        "/images/img-with-contributors?include=source.contributors"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.source.contributors).toBeDefined();
    });

    it("does not include source.contributors by default", async () => {
      const testImage = imageWithContributors({ id: "img-with-contributors" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images/img-with-contributors");
      expect(response.statusCode).toBe(200);
      expect(response.body.source.contributors).toBeUndefined();
    });
  });

  describe("include=source.languages", () => {
    it("includes source.languages in list response", async () => {
      const testImage = imageWithLanguages({ id: "img-with-languages" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images?include=source.languages");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].source.languages).toBeDefined();
    });

    it("includes source.languages in single image response", async () => {
      const testImage = imageWithLanguages({ id: "img-with-languages" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get(
        "/images/img-with-languages?include=source.languages"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.source.languages).toBeDefined();
    });

    it("does not include source.languages by default", async () => {
      const testImage = imageWithLanguages({ id: "img-with-languages" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images/img-with-languages");
      expect(response.statusCode).toBe(200);
      expect(response.body.source.languages).toBeUndefined();
    });
  });

  describe("include=source.genres", () => {
    it("includes source.genres in list response", async () => {
      const testImage = imageWithGenres({ id: "img-with-genres" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images?include=source.genres");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].source.genres).toBeDefined();
    });

    it("includes source.genres in single image response", async () => {
      const testImage = imageWithGenres({ id: "img-with-genres" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get(
        "/images/img-with-genres?include=source.genres"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.source.genres).toBeDefined();
    });

    it("does not include source.genres by default", async () => {
      const testImage = imageWithGenres({ id: "img-with-genres" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images/img-with-genres");
      expect(response.statusCode).toBe(200);
      expect(response.body.source.genres).toBeUndefined();
    });
  });

  describe("include=source.subjects", () => {
    it("includes source.subjects in list response", async () => {
      const testImage = imageWithSubjects({ id: "img-with-subjects" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images?include=source.subjects");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].source.subjects).toBeDefined();
    });

    it("includes source.subjects in single image response", async () => {
      const testImage = imageWithSubjects({ id: "img-with-subjects" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get(
        "/images/img-with-subjects?include=source.subjects"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.source.subjects).toBeDefined();
    });

    it("does not include source.subjects by default", async () => {
      const testImage = imageWithSubjects({ id: "img-with-subjects" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get("/images/img-with-subjects");
      expect(response.statusCode).toBe(200);
      expect(response.body.source.subjects).toBeUndefined();
    });
  });

  describe("multiple includes", () => {
    it("includes multiple source fields when requested", async () => {
      const testImage: Image = {
        ...imageWithLanguages({ id: "img-multi-include" }),
        source: {
          ...imageWithLanguages().source,
          genres: imageWithGenres().source.genres,
          subjects: imageWithSubjects().source.subjects,
        },
      };
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get(
        "/images?include=source.languages,source.genres,source.subjects"
      );
      expect(response.statusCode).toBe(200);
    });

    it("returns 400 for invalid includes mixed with valid ones", async () => {
      const testImage = imageWithLanguages({ id: "img-mixed-include" });
      const api = mockedImagesApi([indexedImage(testImage)]);

      const response = await api.get(
        "/images?include=source.languages,invalid,source.genres"
      );
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
    });
  });

  describe("include=withSimilarFeatures", () => {
    it("accepts withSimilarFeatures include parameter", async () => {
      const testImage = image({ id: "img-with-features" });
      const api = mockedImagesApi([
        indexedImage(testImage, {
          vectorValues: { features: [0.1, 0.2, 0.3, 0.4, 0.5] },
        }),
      ]);

      const response = await api.get("/images?include=withSimilarFeatures");
      expect(response.statusCode).toBe(200);
    });

    it("accepts withSimilarFeatures for single image", async () => {
      const testImage = image({ id: "img-with-features" });
      const api = mockedImagesApi([
        indexedImage(testImage, {
          vectorValues: { features: [0.1, 0.2, 0.3, 0.4, 0.5] },
        }),
      ]);

      const response = await api.get(
        "/images/img-with-features?include=withSimilarFeatures"
      );
      expect(response.statusCode).toBe(200);
    });
  });
});
