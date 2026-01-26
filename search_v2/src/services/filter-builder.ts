import {
  WorkFilter,
  ImageFilter,
  WorkSearchOptions,
  ImageSearchOptions,
  RgbColor,
} from "../types";

// ============================================================================
// Query Types for Elasticsearch
// ============================================================================

export type ElasticQuery =
  | { term: Record<string, { value: string }> }
  | { terms: Record<string, string[]> }
  | { range: Record<string, { gte?: string; lte?: string }> }
  | {
      bool: {
        must?: ElasticQuery[];
        should?: ElasticQuery[];
        filter?: ElasticQuery[];
        must_not?: ElasticQuery[];
      };
    }
  | { match_all: Record<string, never> };

// ============================================================================
// Work Filter Query Builder
// ============================================================================

export function buildWorkFilterQuery(filter: WorkFilter): ElasticQuery | null {
  switch (filter.type) {
    case "FormatFilter":
      return {
        terms: { "filterableValues.format.id": filter.formatIds },
      };

    case "WorkTypeFilter":
      return {
        terms: { "filterableValues.workType": filter.types },
      };

    case "DateRangeFilter": {
      const range: { gte?: string; lte?: string } = {};
      if (filter.fromDate) range.gte = filter.fromDate;
      if (filter.toDate) range.lte = filter.toDate;
      return {
        range: { "filterableValues.production.dates.range.from": range },
      };
    }

    case "LanguagesFilter":
      return {
        terms: { "filterableValues.languages.id": filter.languageIds },
      };

    case "GenreLabelFilter":
      return {
        terms: { "filterableValues.genres.label": filter.labels },
      };

    case "GenreIdFilter":
      if (filter.conceptIds.length === 0) return null;
      return {
        terms: { "filterableValues.genres.concepts.id": filter.conceptIds },
      };

    case "SubjectLabelFilter":
      return {
        terms: { "filterableValues.subjects.label": filter.labels },
      };

    case "SubjectIdFilter":
      if (filter.conceptIds.length === 0) return null;
      return {
        terms: { "filterableValues.subjects.concepts.id": filter.conceptIds },
      };

    case "ContributorsLabelFilter":
      return {
        terms: {
          "filterableValues.contributors.agent.label": filter.labels,
        },
      };

    case "ContributorsIdFilter":
      return {
        terms: {
          "filterableValues.contributors.agent.id": filter.conceptIds,
        },
      };

    case "IdentifiersFilter":
      // Using query.identifiers.value for case-insensitive matching
      return {
        terms: { "query.identifiers.value": filter.values },
      };

    case "LicenseFilter":
      return {
        terms: {
          "filterableValues.items.locations.license.id": filter.licenseIds,
        },
      };

    case "AccessStatusFilter": {
      const queries: ElasticQuery[] = [];
      const mustNot: ElasticQuery[] = [];

      if (filter.includes.length > 0) {
        queries.push({
          terms: {
            "filterableValues.items.locations.accessConditions.status.id":
              filter.includes,
          },
        });
      }

      if (filter.excludes.length > 0) {
        mustNot.push({
          terms: {
            "filterableValues.items.locations.accessConditions.status.id":
              filter.excludes,
          },
        });
      }

      return {
        bool: {
          ...(queries.length > 0 ? { must: queries } : {}),
          ...(mustNot.length > 0 ? { must_not: mustNot } : {}),
        },
      };
    }

    case "ItemsFilter":
      return {
        bool: {
          should: [{ terms: { "filterableValues.items.id": filter.itemIds } }],
        },
      };

    case "ItemsIdentifiersFilter":
      return {
        bool: {
          should: [
            {
              terms: {
                "filterableValues.items.identifiers.value": filter.values,
              },
            },
          ],
        },
      };

    case "ItemLocationTypeIdFilter":
      return {
        terms: {
          "filterableValues.items.locations.locationType.id":
            filter.locationTypeIds,
        },
      };

    case "PartOfFilter":
      return {
        term: { "filterableValues.partOf.id": { value: filter.id } },
      };

    case "PartOfTitleFilter":
      return {
        term: { "filterableValues.partOf.title": { value: filter.title } },
      };

    case "AvailabilitiesFilter":
      return {
        terms: {
          "filterableValues.availabilities.id": filter.availabilityIds,
        },
      };
  }
}

// ============================================================================
// Image Filter Query Builder
// ============================================================================

export function buildImageFilterQuery(
  filter: ImageFilter
): ElasticQuery | null {
  switch (filter.type) {
    case "LicenseFilter":
      return {
        terms: { "filterableValues.locations.license.id": filter.licenseIds },
      };

    case "ContributorsLabelFilter":
      return {
        terms: {
          "filterableValues.source.contributors.agent.label": filter.labels,
        },
      };

    case "ContributorsIdFilter":
      return {
        terms: {
          "filterableValues.source.contributors.agent.id": filter.conceptIds,
        },
      };

    case "GenreLabelFilter":
      return {
        terms: { "filterableValues.source.genres.label": filter.labels },
      };

    case "GenreIdFilter":
      if (filter.conceptIds.length === 0) return null;
      return {
        terms: {
          "filterableValues.source.genres.concepts.id": filter.conceptIds,
        },
      };

    case "SubjectLabelFilter":
      return {
        terms: { "filterableValues.source.subjects.label": filter.labels },
      };

    case "SubjectIdFilter":
      if (filter.conceptIds.length === 0) return null;
      return {
        terms: {
          "filterableValues.source.subjects.concepts.id": filter.conceptIds,
        },
      };

    case "DateRangeFilter": {
      const range: { gte?: string; lte?: string } = {};
      if (filter.fromDate) range.gte = filter.fromDate;
      if (filter.toDate) range.lte = filter.toDate;
      return {
        range: {
          "filterableValues.source.production.dates.range.from": range,
        },
      };
    }
  }
}

// ============================================================================
// Visible Work Filter (always applied)
// ============================================================================

export const visibleWorkFilter: ElasticQuery = {
  term: { type: { value: "Visible" } },
};

// ============================================================================
// Check if a filter is pairable (for aggregation filtering)
// ============================================================================

export function isPairableWorkFilter(filter: WorkFilter): boolean {
  return [
    "FormatFilter",
    "LanguagesFilter",
    "GenreLabelFilter",
    "GenreIdFilter",
    "SubjectLabelFilter",
    "SubjectIdFilter",
    "ContributorsLabelFilter",
    "ContributorsIdFilter",
    "LicenseFilter",
    "AvailabilitiesFilter",
  ].includes(filter.type);
}

export function isPairableImageFilter(filter: ImageFilter): boolean {
  return [
    "LicenseFilter",
    "ContributorsLabelFilter",
    "ContributorsIdFilter",
    "GenreLabelFilter",
    "GenreIdFilter",
    "SubjectLabelFilter",
    "SubjectIdFilter",
  ].includes(filter.type);
}

// ============================================================================
// Color Query (KNN for color similarity search)
// ============================================================================

export function buildColorKnnQuery(color: RgbColor) {
  // Normalize RGB values to 0-1 range for the color vector
  const colorVector = [color.r / 255, color.g / 255, color.b / 255];

  return {
    field: "vectorValues.paletteEmbedding",
    query_vector: colorVector,
    k: 10,
    num_candidates: 100,
  };
}

// ============================================================================
// Sort Configuration
// ============================================================================

export function getWorksSortConfig(
  options: WorkSearchOptions
): { field: string; order: "asc" | "desc" } | null {
  if (!options.sortBy) return null;

  const sortFields: Record<string, string> = {
    "production.dates": "filterableValues.production.dates.range.from",
    "items.locations.createdDate":
      "filterableValues.items.locations.createdDate",
  };

  const field = sortFields[options.sortBy];
  if (!field) return null;

  return { field, order: options.sortOrder };
}

export function getImagesSortConfig(
  options: ImageSearchOptions
): { field: string; order: "asc" | "desc" } | null {
  if (!options.sortBy) return null;

  const sortFields: Record<string, string> = {
    "source.production.dates":
      "filterableValues.source.production.dates.range.from",
  };

  const field = sortFields[options.sortBy];
  if (!field) return null;

  return { field, order: options.sortOrder };
}
