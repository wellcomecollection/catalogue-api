import { z } from "zod";
import {
  WorkFilter,
  ImageFilter,
  WorkAggregationRequest,
  ImageAggregationRequest,
  WorkInclude,
  ImageIncludeType,
  WorkSortField,
  ImageSortField,
  SortOrder,
  RgbColor,
} from "../types";

// ============================================================================
// Helper Parsers
// ============================================================================

// CSV-style parser that handles quoted values correctly
// e.g., "PATCH CLAMPING","Frogs" => ["PATCH CLAMPING", "Frogs"]
// This matches the Scala API behavior which uses a proper CSV parser
function parseCommaSeparatedWithQuotes(val: string): string[] {
  const result: string[] = [];
  let current = "";
  let inQuotes = false;

  for (let i = 0; i < val.length; i++) {
    const char = val[i];

    if (char === '"' && (i === 0 || val[i - 1] !== "\\")) {
      inQuotes = !inQuotes;
    } else if (char === "," && !inQuotes) {
      const trimmed = current.trim();
      if (trimmed) result.push(trimmed);
      current = "";
    } else if (char === "\\" && i + 1 < val.length && val[i + 1] === '"') {
      // Handle escaped quotes
      current += '"';
      i++; // Skip the next character
    } else {
      current += char;
    }
  }

  // Don't forget the last value
  const trimmed = current.trim();
  if (trimmed) result.push(trimmed);

  return result;
}

const commaSeparatedList = z
  .string()
  .transform((val) => parseCommaSeparatedWithQuotes(val));

const parseLocalDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, {
  message: "Date must be in YYYY-MM-DD format",
});

const parseHexColor = z.string().transform((val, ctx): RgbColor => {
  const hex = val.replace(/^#/, "");
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: `'${val}' is not a valid value. Please supply a single hex string.`,
    });
    return { r: 0, g: 0, b: 0 };
  }
  return {
    r: parseInt(hex.slice(0, 2), 16),
    g: parseInt(hex.slice(2, 4), 16),
    b: parseInt(hex.slice(4, 6), 16),
  };
});

// Helper to format valid values list like Scala: ['val1', 'val2', ...]
function formatValidValues(values: readonly string[]): string {
  return "[" + values.map((v) => `'${v}'`).join(", ") + "]";
}

// Helper to format invalid values for error message
function formatInvalidValues(values: string[]): string {
  if (values.length === 1) {
    return `'${values[0]}' is not a valid value`;
  }
  return values.map((v) => `'${v}'`).join(", ") + " are not valid values";
}

// Creates a parser that validates comma-separated values against allowed list
function createValidatedListParser<T extends string>(
  validValues: readonly T[],
  paramName: string
) {
  return z.string().transform((val, ctx): T[] => {
    const items = parseCommaSeparatedWithQuotes(val);
    const valid: T[] = [];
    const invalid: string[] = [];

    for (const item of items) {
      if (validValues.includes(item as T)) {
        valid.push(item as T);
      } else {
        invalid.push(item);
      }
    }

    if (invalid.length > 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `${formatInvalidValues(
          invalid
        )}. Please choose one of: ${formatValidValues(validValues)}`,
      });
    }

    return valid;
  });
}

// Creates a parser for include values that returns a Set
function createValidatedIncludeParser<T extends string>(
  validValues: readonly T[],
  paramName: string
) {
  return z.string().transform((val, ctx): Set<T> => {
    const items = parseCommaSeparatedWithQuotes(val);
    const valid: T[] = [];
    const invalid: string[] = [];

    for (const item of items) {
      if (validValues.includes(item as T)) {
        valid.push(item as T);
      } else {
        invalid.push(item);
      }
    }

    if (invalid.length > 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `${formatInvalidValues(
          invalid
        )}. Please choose one of: ${formatValidValues(validValues)}`,
      });
    }

    return new Set(valid);
  });
}

// Creates a parser for sort that validates a single sort field
function createValidatedSortParser<T extends string>(
  validValues: readonly T[]
) {
  return z.string().transform((val, ctx): T | undefined => {
    const items = val
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
    const invalid: string[] = [];
    let validSort: T | undefined;

    for (const item of items) {
      if (validValues.includes(item as T)) {
        validSort = item as T;
      } else {
        invalid.push(item);
      }
    }

    if (invalid.length > 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `${formatInvalidValues(
          invalid
        )}. Please choose one of: ${formatValidValues(validValues)}`,
      });
    }

    return validSort;
  });
}

// ============================================================================
// Pagination Schema
// ============================================================================

export const paginationSchema = z.object({
  page: z.coerce.number().int().min(1).optional(),
  pageSize: z.coerce.number().int().min(1).max(100).optional(),
});

// ============================================================================
// Works Query Schema
// ============================================================================

const workIncludeValues: WorkInclude[] = [
  "identifiers",
  "items",
  "holdings",
  "subjects",
  "genres",
  "contributors",
  "production",
  "languages",
  "notes",
  "formerFrequency",
  "designation",
  "images",
  "parts",
  "partOf",
  "precededBy",
  "succeededBy",
];

const workAggregationValues: WorkAggregationRequest[] = [
  "workType",
  "genres.label",
  "genres",
  "production.dates",
  "subjects.label",
  "subjects",
  "languages",
  "contributors.agent.label",
  "contributors.agent",
  "items.locations.license",
  "availabilities",
];

const workSortValues: WorkSortField[] = [
  "production.dates",
  "items.locations.createdDate",
];

export const multipleWorksQuerySchema = z.object({
  // Pagination
  page: z.coerce.number().int().min(1).optional(),
  pageSize: z.coerce.number().int().min(1).max(100).optional(),

  // Search
  query: z.string().optional(),

  // Includes (with validation)
  include: createValidatedIncludeParser(
    workIncludeValues,
    "include"
  ).optional(),

  // Aggregations (with validation)
  aggregations: createValidatedListParser(
    workAggregationValues,
    "aggregations"
  ).optional(),

  // Sorting (with validation)
  sort: createValidatedSortParser(workSortValues).optional(),
  sortOrder: z.enum(["asc", "desc"]).optional(),

  // Filters
  workType: commaSeparatedList.optional(),
  "production.dates.from": parseLocalDate.optional(),
  "production.dates.to": parseLocalDate.optional(),
  languages: commaSeparatedList.optional(),
  "genres.label": commaSeparatedList.optional(),
  genres: commaSeparatedList.optional(),
  "subjects.label": commaSeparatedList.optional(),
  subjects: commaSeparatedList.optional(),
  "contributors.agent.label": commaSeparatedList.optional(),
  "contributors.agent": commaSeparatedList.optional(),
  identifiers: commaSeparatedList.optional(),
  items: commaSeparatedList.optional(),
  "items.identifiers": commaSeparatedList.optional(),
  "items.locations.license": commaSeparatedList.optional(),
  "items.locations.locationType": commaSeparatedList.optional(),
  "items.locations.accessConditions.status": z.string().optional(),
  type: commaSeparatedList.optional(),
  partOf: z.string().optional(),
  "partOf.title": z.string().optional(),
  availabilities: commaSeparatedList.optional(),
});

export type MultipleWorksQuery = z.infer<typeof multipleWorksQuerySchema>;

export const singleWorkQuerySchema = z.object({
  include: createValidatedIncludeParser(
    workIncludeValues,
    "include"
  ).optional(),
});

export type SingleWorkQuery = z.infer<typeof singleWorkQuerySchema>;

// ============================================================================
// Images Query Schema
// ============================================================================

const imageIncludeValues: ImageIncludeType[] = [
  "withSimilarFeatures",
  "source.contributors",
  "source.languages",
  "source.genres",
  "source.subjects",
];

const imageAggregationValues: ImageAggregationRequest[] = [
  "locations.license",
  "source.contributors.agent.label",
  "source.contributors.agent",
  "source.genres.label",
  "source.genres",
  "source.subjects.label",
  "source.subjects",
];

const imageSortValues: ImageSortField[] = ["source.production.dates"];

export const multipleImagesQuerySchema = z.object({
  // Pagination
  page: z.coerce.number().int().min(1).optional(),
  pageSize: z.coerce.number().int().min(1).max(100).optional(),

  // Search
  query: z.string().optional(),

  // Color search
  color: parseHexColor.optional(),

  // Includes (with validation)
  include: createValidatedIncludeParser(
    imageIncludeValues,
    "include"
  ).optional(),

  // Aggregations (with validation)
  aggregations: createValidatedListParser(
    imageAggregationValues,
    "aggregations"
  ).optional(),

  // Sorting (with validation)
  sort: createValidatedSortParser(imageSortValues).optional(),
  sortOrder: z.enum(["asc", "desc"]).optional(),

  // Filters
  "locations.license": commaSeparatedList.optional(),
  "source.contributors.agent.label": commaSeparatedList.optional(),
  "source.contributors.agent": commaSeparatedList.optional(),
  "source.genres.label": commaSeparatedList.optional(),
  "source.genres": commaSeparatedList.optional(),
  "source.subjects.label": commaSeparatedList.optional(),
  "source.subjects": commaSeparatedList.optional(),
  "source.production.dates.from": parseLocalDate.optional(),
  "source.production.dates.to": parseLocalDate.optional(),
});

export type MultipleImagesQuery = z.infer<typeof multipleImagesQuerySchema>;

export const singleImageQuerySchema = z.object({
  include: createValidatedIncludeParser(
    imageIncludeValues,
    "include"
  ).optional(),
});

export type SingleImageQuery = z.infer<typeof singleImageQuerySchema>;

// ============================================================================
// Filter Builders
// ============================================================================

export function buildWorkFilters(query: MultipleWorksQuery): WorkFilter[] {
  const filters: WorkFilter[] = [];

  if (query.workType?.length) {
    filters.push({ type: "FormatFilter", formatIds: query.workType });
  }

  if (query.type?.length) {
    filters.push({ type: "WorkTypeFilter", types: query.type });
  }

  if (query["production.dates.from"] || query["production.dates.to"]) {
    filters.push({
      type: "DateRangeFilter",
      fromDate: query["production.dates.from"],
      toDate: query["production.dates.to"],
    });
  }

  if (query.languages?.length) {
    filters.push({ type: "LanguagesFilter", languageIds: query.languages });
  }

  if (query["genres.label"]?.length) {
    filters.push({ type: "GenreLabelFilter", labels: query["genres.label"] });
  }

  if (query.genres?.length) {
    filters.push({ type: "GenreIdFilter", conceptIds: query.genres });
  }

  if (query["subjects.label"]?.length) {
    filters.push({
      type: "SubjectLabelFilter",
      labels: query["subjects.label"],
    });
  }

  if (query.subjects?.length) {
    filters.push({ type: "SubjectIdFilter", conceptIds: query.subjects });
  }

  if (query["contributors.agent.label"]?.length) {
    filters.push({
      type: "ContributorsLabelFilter",
      labels: query["contributors.agent.label"],
    });
  }

  if (query["contributors.agent"]?.length) {
    filters.push({
      type: "ContributorsIdFilter",
      conceptIds: query["contributors.agent"],
    });
  }

  if (query.identifiers?.length) {
    filters.push({ type: "IdentifiersFilter", values: query.identifiers });
  }

  if (query.items?.length) {
    filters.push({ type: "ItemsFilter", itemIds: query.items });
  }

  if (query["items.identifiers"]?.length) {
    filters.push({
      type: "ItemsIdentifiersFilter",
      values: query["items.identifiers"],
    });
  }

  if (query["items.locations.license"]?.length) {
    filters.push({
      type: "LicenseFilter",
      licenseIds: query["items.locations.license"],
    });
  }

  if (query["items.locations.locationType"]?.length) {
    filters.push({
      type: "ItemLocationTypeIdFilter",
      locationTypeIds: query["items.locations.locationType"],
    });
  }

  if (query["items.locations.accessConditions.status"]) {
    const statusValue = query["items.locations.accessConditions.status"];
    const { includes, excludes } = parseIncludesExcludes(statusValue);
    filters.push({ type: "AccessStatusFilter", includes, excludes });
  }

  if (query.partOf) {
    filters.push({ type: "PartOfFilter", id: query.partOf });
  }

  if (query["partOf.title"]) {
    filters.push({ type: "PartOfTitleFilter", title: query["partOf.title"] });
  }

  if (query.availabilities?.length) {
    filters.push({
      type: "AvailabilitiesFilter",
      availabilityIds: query.availabilities,
    });
  }

  return filters;
}

export function buildImageFilters(query: MultipleImagesQuery): ImageFilter[] {
  const filters: ImageFilter[] = [];

  if (query["locations.license"]?.length) {
    filters.push({
      type: "LicenseFilter",
      licenseIds: query["locations.license"],
    });
  }

  if (query["source.contributors.agent.label"]?.length) {
    filters.push({
      type: "ContributorsLabelFilter",
      labels: query["source.contributors.agent.label"],
    });
  }

  if (query["source.contributors.agent"]?.length) {
    filters.push({
      type: "ContributorsIdFilter",
      conceptIds: query["source.contributors.agent"],
    });
  }

  if (query["source.genres.label"]?.length) {
    filters.push({
      type: "GenreLabelFilter",
      labels: query["source.genres.label"],
    });
  }

  if (query["source.genres"]?.length) {
    filters.push({
      type: "GenreIdFilter",
      conceptIds: query["source.genres"],
    });
  }

  if (query["source.subjects.label"]?.length) {
    filters.push({
      type: "SubjectLabelFilter",
      labels: query["source.subjects.label"],
    });
  }

  if (query["source.subjects"]?.length) {
    filters.push({
      type: "SubjectIdFilter",
      conceptIds: query["source.subjects"],
    });
  }

  if (
    query["source.production.dates.from"] ||
    query["source.production.dates.to"]
  ) {
    filters.push({
      type: "DateRangeFilter",
      fromDate: query["source.production.dates.from"],
      toDate: query["source.production.dates.to"],
    });
  }

  return filters;
}

// Parse access status with include/exclude support (e.g., "!closed" excludes closed)
function parseIncludesExcludes(value: string): {
  includes: string[];
  excludes: string[];
} {
  const parts = value
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  const includes: string[] = [];
  const excludes: string[] = [];

  for (const part of parts) {
    if (part.startsWith("!")) {
      excludes.push(part.slice(1));
    } else {
      includes.push(part);
    }
  }

  return { includes, excludes };
}
