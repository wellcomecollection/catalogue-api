import { mockedImagesApi } from "./fixtures/api";
import {
  image,
  indexedImage,
  imageWithThumbnail,
  imageWithContributors,
} from "./fixtures/images";

describe("GET /images", () => {
  it("returns a list of images", async () => {
    const testImages = Array.from({ length: 5 }).map((_, i) =>
      indexedImage(image({ id: `image-${i}` }))
    );
    const api = mockedImagesApi(testImages);

    const response = await api.get("/images");
    expect(response.statusCode).toBe(200);
    expect(response.body.type).toBe("ResultList");
    expect(response.body.results).toHaveLength(5);
    expect(response.body.totalResults).toBe(5);
  });

  it("returns pagination info", async () => {
    const testImages = Array.from({ length: 5 }).map((_, i) =>
      indexedImage(image({ id: `image-${i}` }))
    );
    const api = mockedImagesApi(testImages);

    const response = await api.get("/images?page=1&pageSize=2");
    expect(response.statusCode).toBe(200);
    expect(response.body.pageSize).toBe(2);
    expect(response.body.totalPages).toBe(3);
    expect(response.body.totalResults).toBe(5);
  });

  it("returns empty list when no images exist", async () => {
    const api = mockedImagesApi([]);

    const response = await api.get("/images");
    expect(response.statusCode).toBe(200);
    expect(response.body.results).toHaveLength(0);
    expect(response.body.totalResults).toBe(0);
  });

  it("ignores unknown parameters", async () => {
    const testImages = [indexedImage(image({ id: "image-1" }))];
    const api = mockedImagesApi(testImages);

    const response = await api.get("/images?foo=bar&unknown=param");
    expect(response.statusCode).toBe(200);
  });
});

describe("GET /images/:id", () => {
  it("returns a single image when requested with id", async () => {
    const testImage = image({ id: "img123", sourceTitle: "Source Work" });
    const api = mockedImagesApi([indexedImage(testImage)]);

    const response = await api.get("/images/img123");
    expect(response.statusCode).toBe(200);
    expect(response.body.id).toBe("img123");
    expect(response.body.type).toBe("Image");
    expect(response.body.source.title).toBe("Source Work");
  });

  it("returns 404 when image not found", async () => {
    const api = mockedImagesApi([]);

    const response = await api.get("/images/nonexistent");
    expect(response.statusCode).toBe(404);
    expect(response.body.type).toBe("Error");
    expect(response.body.label).toBe("Not Found");
  });

  it("shows the thumbnail field if available", async () => {
    const testImage = imageWithThumbnail({ id: "image-thumbnail" });
    const api = mockedImagesApi([indexedImage(testImage)]);

    const response = await api.get("/images/image-thumbnail");
    expect(response.statusCode).toBe(200);
    expect(response.body.thumbnail).toBeDefined();
    expect(response.body.thumbnail.type).toBe("DigitalLocation");
  });

  it("returns minimal source by default", async () => {
    const testImage = imageWithContributors({ id: "image-contrib" });
    const api = mockedImagesApi([indexedImage(testImage)]);

    const response = await api.get("/images/image-contrib");
    expect(response.statusCode).toBe(200);
    // Contributors should not be included by default
    expect(response.body.source.contributors).toBeUndefined();
  });

  it("includes source.contributors when requested", async () => {
    const testImage = imageWithContributors({ id: "image-contrib" });
    const api = mockedImagesApi([indexedImage(testImage)]);

    const response = await api.get(
      "/images/image-contrib?include=source.contributors"
    );
    expect(response.statusCode).toBe(200);
    expect(response.body.source.contributors).toBeDefined();
    expect(response.body.source.contributors).toHaveLength(1);
  });
});
