import { URL, URLSearchParams } from "url";
import path from "path";
import { HttpError } from "./error";
import { Config } from "../../config";

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
  params: URLSearchParams,
  config: Config
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
    (pageSize > config.maxPageSize || pageSize < limits.minSize)
  ) {
    throw new HttpError({
      status: 400,
      label: "Bad Request",
      description: `pageSize: must be between ${limits.minSize} and ${config.maxPageSize}`,
    });
  }

  return { page, pageSize };
};

export const paginationResponseGetter =
  (publicRootUrl: URL, config: Config) =>
  ({
    requestUrl,
    totalResults,
  }: {
    requestUrl: URL;
    totalResults: number;
  }): PaginationResponse => {
    const { page = 1, pageSize = config.defaultPageSize } =
      parsePaginationQueryParameters(requestUrl.searchParams, config);
    const totalPages = Math.ceil(totalResults / pageSize);
    return {
      pageSize,
      totalPages,
      totalResults,
      prevPage: pageLink(page - 1, totalPages, requestUrl, publicRootUrl),
      nextPage: pageLink(page + 1, totalPages, requestUrl, publicRootUrl),
    };
  };

const pageLink = (
  page: number,
  totalPages: number,
  requestUrl: URL,
  publicRootUrl: URL
): string | undefined => {
  if (pageExists(page, totalPages)) {
    const linkUrl = extractPublicUrl(requestUrl, publicRootUrl);
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

const extractPublicUrl = (requestUrl: URL, publicRootUrl: URL): URL => {
  const publicUrl = new URL(requestUrl.href);
  publicUrl.host = publicRootUrl.host;
  publicUrl.port = publicRootUrl.port;
  publicUrl.protocol = publicRootUrl.protocol;
  publicUrl.pathname = path.join(publicRootUrl.pathname, requestUrl.pathname);
  return publicUrl;
};

const pageExists = (page: number, totalPages: number): boolean =>
  page > 0 && page <= totalPages;
