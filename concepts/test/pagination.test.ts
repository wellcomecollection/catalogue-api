import { URL } from "url";
import {
  getPaginationResponse,
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
    it("returns appropriate links to prev/next pages if they exist", () => {
      const requestUrl = new URL("https://test.test/docs?page=4&pageSize=25");
      const totalResults = 100;
      const response = getPaginationResponse({ requestUrl, totalResults });

      expect(response.pageSize).toBe(25);
      expect(response.totalPages).toBe(4);
      expect(response.totalResults).toBe(totalResults);
      expect(response.nextPage).toBeUndefined();
      expect(response.prevPage).toBe(
        "https://test.test/docs?page=3&pageSize=25"
      );
    });

    it("uses defaults where appropriate", () => {
      const requestUrl = new URL("https://test.test/docs");
      const totalResults = 100;
      const response = getPaginationResponse({ requestUrl, totalResults });

      expect(response.pageSize).toBe(10);
      expect(response.nextPage).toBe("https://test.test/docs?page=2");
    });
  });
});
