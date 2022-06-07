import { URL, URLSearchParams } from "url";
import { HttpError } from "./error";

const limits = {
  minSize: 1,
  maxSize: 100,
};

const defaultPageSize = 10;

export type PaginationQueryParameters = {
  page?: number;
  pageSize?: number;
};

export type PaginationResponse = {
  pageSize: number;
  totalPages: number;
  totalResults: number;
  prevPage?: string;
  nextPage?: string;
};

export const paginationElasticBody = ({
  page,
  pageSize,
}: PaginationQueryParameters): { from: number; size: number } => {
  const size = Math.min(pageSize ?? defaultPageSize, limits.maxSize);
  return {
    size,
    from: ((page ?? 1) - 1) * size,
  };
};

const parsePaginationQueryParameters = (
  params: URLSearchParams
): PaginationQueryParameters => {
  const page = parseNumberParam("page", params);
  const pageSize = parseNumberParam("pageSize", params);

  if (page !== undefined && page < 1) {
    throw new HttpError({
      status: 400,
      label: "Bad Request",
      description: "page: must be greater than 1",
    });
  }
  if (
    pageSize !== undefined &&
    (pageSize >= limits.maxSize || pageSize <= limits.minSize)
  ) {
    throw new HttpError({
      status: 400,
      label: "Bad request",
      description: `pageSize: must be between ${limits.minSize} and ${limits.maxSize}`,
    });
  }

  return { page, pageSize };
};

export const getPaginationResponse = ({
  requestUrl,
  totalResults,
}: {
  requestUrl: URL;
  totalResults: number;
}): PaginationResponse => {
  const { page = 1, pageSize = defaultPageSize } =
    parsePaginationQueryParameters(requestUrl.searchParams);
  const totalPages = Math.ceil(totalResults / pageSize);
  return {
    pageSize,
    totalPages,
    totalResults,
    prevPage: pageLink(page - 1, totalPages, requestUrl),
    nextPage: pageLink(page + 1, totalPages, requestUrl),
  };
};

const pageLink = (
  page: number,
  totalPages: number,
  requestUrl: URL
): string | undefined => {
  if (pageExists(page, totalPages)) {
    const linkUrl = new URL(requestUrl.href);
    linkUrl.searchParams.set("page", page.toString());
    return linkUrl.href;
  }
};

const parseNumberParam = (
  key: string,
  params: URLSearchParams
): number | undefined => {
  const number = parseInt(params.get(key) ?? "");
  return isNaN(number) ? undefined : number;
};

const pageExists = (page: number, totalPages: number): boolean =>
  page > 0 && page <= totalPages;
