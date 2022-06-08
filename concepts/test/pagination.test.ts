import { URL } from "url";
import {
  paginationResponseGetter,
  paginationElasticBody,
} from "../src/controllers/pagination";

describe("pagination tools", () => {
  describe("paginationElasticBody", () => {
    it("returns a `size` and `from` value", () => {
      const { size, from } = paginationElasticBody({ page: 12, pageSize: 55 });

      expect(size).toBe(55);
      expect(from).toBe(11 * 55);
    });

    it("clamps the page size as per the global limit", () => {
      const { size, from } = paginationElasticBody({ page: 10, pageSize: 333 });

      expect(size).toBe(100);
      expect(from).toBe(9 * 100);
    });
  });

  describe("getPaginationResponse", () => {
    const publicRootUrl = "https://test.public.url/root";
    const getPaginationResponse = paginationResponseGetter(
      new URL(publicRootUrl)
    );

    it("returns appropriate links to prev/next pages if they exist", () => {
      const requestUrl = new URL(
        "https://test.private:123/docs?page=4&pageSize=25"
      );
      const totalResults = 100;
      const response = getPaginationResponse({ requestUrl, totalResults });

      expect(response.pageSize).toBe(25);
      expect(response.totalPages).toBe(4);
      expect(response.totalResults).toBe(totalResults);
      expect(response.nextPage).toBeUndefined();
      expect(response.prevPage).toBe(
        `${publicRootUrl}/docs?page=3&pageSize=25`
      );
    });

    it("uses defaults where appropriate", () => {
      const requestUrl = new URL("https://test.private:123/docs");
      const totalResults = 100;
      const response = getPaginationResponse({ requestUrl, totalResults });

      expect(response.pageSize).toBe(10);
      expect(response.nextPage).toBe(`${publicRootUrl}/docs?page=2`);
    });
  });
});
