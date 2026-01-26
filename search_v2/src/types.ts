import { Client as ElasticClient } from "@elastic/elasticsearch";

// ============================================================================
// Core Types
// ============================================================================

export type Clients = {
  elastic: ElasticClient;
};

export type Displayable = {
  display: unknown;
  type?: string;
};

// ============================================================================
// Pagination Types
// ============================================================================

export type PaginationQuery = {
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

// ============================================================================
// Result Types
// ============================================================================

export type ResultList<Result, Aggregations = unknown> = {
  type: "ResultList";
  results: Result[];
  aggregations?: Aggregations;
} & PaginationResponse;

// ============================================================================
// Error Types
// ============================================================================

export type ErrorResponse = {
  httpStatus: number;
  label: string;
  description?: string;
  errorType: "http";
  type: "Error";
};

// ============================================================================
// Work Types
// ============================================================================

export type Format = {
  id: string;
  label: string;
  type: "Format";
};

export type Work = {
  id: string;
  title: string;
  alternativeTitles: string[];
  workType?: Format;
  availabilities: Availability[];
  type: "Work" | "Collection" | "Series" | "Section";
  edition?: string;
  duration?: number;
  thumbnail?: DigitalLocation;
  identifiers?: Identifier[];
  items?: Item[];
  holdings?: Holding[];
  subjects?: Subject[];
  genres?: Genre[];
  contributors?: Contributor[];
  production?: ProductionEvent[];
  languages?: Language[];
  notes?: Note[];
  formerFrequency?: string[];
  designation?: string[];
  images?: ImageInclude[];
  parts?: RelatedWork[];
  partOf?: RelatedWork[];
  precededBy?: RelatedWork[];
  succeededBy?: RelatedWork[];
};

export type RelatedWork = {
  id: string;
  title?: string;
  type: string;
};

export type Availability = {
  id: string;
  label: string;
  type: "Availability";
};

export type DigitalLocation = {
  locationType: LocationType;
  url: string;
  license?: License;
  accessConditions: AccessCondition[];
  type: "DigitalLocation";
};

export type PhysicalLocation = {
  locationType: LocationType;
  label?: string;
  shelfmark?: string;
  accessConditions: AccessCondition[];
  type: "PhysicalLocation";
};

export type Location = DigitalLocation | PhysicalLocation;

export type LocationType = {
  id: string;
  label: string;
  type: "LocationType";
};

export type License = {
  id: string;
  label: string;
  url: string;
  type: "License";
};

export type AccessCondition = {
  method?: AccessMethod;
  status?: AccessStatus;
  terms?: string;
  type: "AccessCondition";
};

export type AccessMethod = {
  id: string;
  label: string;
  type: "AccessMethod";
};

export type AccessStatus = {
  id: string;
  label: string;
  type: "AccessStatus";
};

export type Identifier = {
  identifierType: IdentifierType;
  value: string;
  type: "Identifier";
};

export type IdentifierType = {
  id: string;
  label: string;
  type: "IdentifierType";
};

export type Item = {
  id?: string;
  identifiers?: Identifier[];
  locations: Location[];
  type: "Item";
};

export type Holding = {
  enumeration?: string[];
  location?: PhysicalLocation;
  type: "Holdings";
};

export type Subject = {
  id?: string;
  identifiers?: Identifier[];
  label: string;
  concepts: Concept[];
  type: "Subject";
};

export type Genre = {
  id?: string;
  identifiers?: Identifier[];
  label: string;
  concepts: Concept[];
  type: "Genre";
};

export type Concept = {
  id?: string;
  identifiers?: Identifier[];
  label: string;
  type:
    | "Concept"
    | "Person"
    | "Organisation"
    | "Meeting"
    | "Period"
    | "Place"
    | "Agent";
};

export type Contributor = {
  agent: Agent;
  roles: ContributorRole[];
  type: "Contributor";
};

export type Agent = {
  id?: string;
  identifiers?: Identifier[];
  label: string;
  type: "Person" | "Organisation" | "Meeting" | "Agent";
};

export type ContributorRole = {
  label: string;
  type: "ContributionRole";
};

export type ProductionEvent = {
  label?: string;
  places: Place[];
  agents: Agent[];
  dates: Period[];
  type: "ProductionEvent";
};

export type Place = {
  id?: string;
  identifiers?: Identifier[];
  label: string;
  type: "Place";
};

export type Period = {
  id?: string;
  identifiers?: Identifier[];
  label: string;
  type: "Period";
};

export type Language = {
  id: string;
  label: string;
  type: "Language";
};

export type Note = {
  noteType: NoteType;
  contents: string[];
  type: "Note";
};

export type NoteType = {
  id: string;
  label: string;
  type: "NoteType";
};

export type ImageInclude = {
  id: string;
  type: "Image";
};

// ============================================================================
// Image Types
// ============================================================================

export type Image = {
  id: string;
  thumbnail?: DigitalLocation;
  locations: DigitalLocation[];
  source: ImageSource;
  type: "Image";
  withSimilarFeatures?: SimilarImage[];
};

export type ImageSource = {
  id: string;
  title: string;
  type: string;
  contributors?: Contributor[];
  languages?: Language[];
  genres?: Genre[];
  subjects?: Subject[];
};

export type SimilarImage = {
  id: string;
  thumbnail?: DigitalLocation;
  type: "Image";
};

// ============================================================================
// Filter Types
// ============================================================================

export type WorkFilter =
  | FormatFilter
  | WorkTypeFilter
  | DateRangeFilter
  | LanguagesFilter
  | GenreLabelFilter
  | GenreIdFilter
  | SubjectLabelFilter
  | SubjectIdFilter
  | ContributorsLabelFilter
  | ContributorsIdFilter
  | LicenseFilter
  | IdentifiersFilter
  | ItemsFilter
  | ItemsIdentifiersFilter
  | AccessStatusFilter
  | ItemLocationTypeIdFilter
  | PartOfFilter
  | PartOfTitleFilter
  | AvailabilitiesFilter;

export type ImageFilter =
  | LicenseFilter
  | ContributorsLabelFilter
  | ContributorsIdFilter
  | GenreLabelFilter
  | GenreIdFilter
  | SubjectLabelFilter
  | SubjectIdFilter
  | DateRangeFilter;

// Pairable filters don't filter out their own aggregation buckets
export type PairableFilter =
  | FormatFilter
  | LanguagesFilter
  | GenreLabelFilter
  | GenreIdFilter
  | SubjectLabelFilter
  | SubjectIdFilter
  | ContributorsLabelFilter
  | ContributorsIdFilter
  | LicenseFilter
  | AvailabilitiesFilter;

export interface FormatFilter {
  type: "FormatFilter";
  formatIds: string[];
}

export interface WorkTypeFilter {
  type: "WorkTypeFilter";
  types: string[];
}

export interface DateRangeFilter {
  type: "DateRangeFilter";
  fromDate?: string; // ISO date string YYYY-MM-DD
  toDate?: string;
}

export interface LanguagesFilter {
  type: "LanguagesFilter";
  languageIds: string[];
}

export interface GenreLabelFilter {
  type: "GenreLabelFilter";
  labels: string[];
}

export interface GenreIdFilter {
  type: "GenreIdFilter";
  conceptIds: string[];
}

export interface SubjectLabelFilter {
  type: "SubjectLabelFilter";
  labels: string[];
}

export interface SubjectIdFilter {
  type: "SubjectIdFilter";
  conceptIds: string[];
}

export interface ContributorsLabelFilter {
  type: "ContributorsLabelFilter";
  labels: string[];
}

export interface ContributorsIdFilter {
  type: "ContributorsIdFilter";
  conceptIds: string[];
}

export interface LicenseFilter {
  type: "LicenseFilter";
  licenseIds: string[];
}

export interface IdentifiersFilter {
  type: "IdentifiersFilter";
  values: string[];
}

export interface ItemsFilter {
  type: "ItemsFilter";
  itemIds: string[];
}

export interface ItemsIdentifiersFilter {
  type: "ItemsIdentifiersFilter";
  values: string[];
}

export interface AccessStatusFilter {
  type: "AccessStatusFilter";
  includes: string[];
  excludes: string[];
}

export interface ItemLocationTypeIdFilter {
  type: "ItemLocationTypeIdFilter";
  locationTypeIds: string[];
}

export interface PartOfFilter {
  type: "PartOfFilter";
  id: string;
}

export interface PartOfTitleFilter {
  type: "PartOfTitleFilter";
  title: string;
}

export interface AvailabilitiesFilter {
  type: "AvailabilitiesFilter";
  availabilityIds: string[];
}

// ============================================================================
// Aggregation Types
// ============================================================================

export type WorkAggregationRequest =
  | "workType"
  | "genres.label"
  | "genres"
  | "production.dates"
  | "subjects.label"
  | "subjects"
  | "languages"
  | "contributors.agent.label"
  | "contributors.agent"
  | "items.locations.license"
  | "availabilities";

export type ImageAggregationRequest =
  | "locations.license"
  | "source.contributors.agent.label"
  | "source.contributors.agent"
  | "source.genres.label"
  | "source.genres"
  | "source.subjects.label"
  | "source.subjects";

export type Aggregation = {
  buckets: AggregationBucket[];
  type: "Aggregation";
};

export type AggregationBucket = {
  data: AggregationBucketData;
  count: number;
  type: "AggregationBucket";
};

export type AggregationBucketData = {
  id: string;
  label: string;
  type: string;
};

export type WorkAggregations = {
  workType?: Aggregation;
  "genres.label"?: Aggregation;
  genres?: Aggregation;
  "production.dates"?: Aggregation;
  "subjects.label"?: Aggregation;
  subjects?: Aggregation;
  languages?: Aggregation;
  "contributors.agent.label"?: Aggregation;
  "contributors.agent"?: Aggregation;
  "items.locations.license"?: Aggregation;
  availabilities?: Aggregation;
};

export type ImageAggregations = {
  license?: Aggregation;
  "source.contributors.agent.label"?: Aggregation;
  "source.contributors.agent"?: Aggregation;
  "source.genres.label"?: Aggregation;
  "source.genres"?: Aggregation;
  "source.subjects.label"?: Aggregation;
  "source.subjects"?: Aggregation;
};

// ============================================================================
// Includes Types
// ============================================================================

export type WorkInclude =
  | "identifiers"
  | "items"
  | "holdings"
  | "subjects"
  | "genres"
  | "contributors"
  | "production"
  | "languages"
  | "notes"
  | "formerFrequency"
  | "designation"
  | "images"
  | "parts"
  | "partOf"
  | "precededBy"
  | "succeededBy";

export type ImageIncludeType =
  | "withSimilarFeatures"
  | "source.contributors"
  | "source.languages"
  | "source.genres"
  | "source.subjects";

export type WorksIncludes = Set<WorkInclude>;
export type ImagesIncludes = Set<ImageIncludeType>;

// ============================================================================
// Sort Types
// ============================================================================

export type WorkSortField = "production.dates" | "items.locations.createdDate";
export type ImageSortField = "source.production.dates";
export type SortOrder = "asc" | "desc";

// ============================================================================
// Search Options Types
// ============================================================================

export type WorkSearchOptions = {
  searchQuery?: string;
  filters: WorkFilter[];
  aggregations: WorkAggregationRequest[];
  sortBy?: WorkSortField;
  sortOrder: SortOrder;
  pageSize: number;
  pageNumber: number;
};

export type ImageSearchOptions = {
  searchQuery?: string;
  filters: ImageFilter[];
  aggregations: ImageAggregationRequest[];
  color?: RgbColor;
  sortBy?: ImageSortField;
  sortOrder: SortOrder;
  pageSize: number;
  pageNumber: number;
};

// ============================================================================
// Color Types
// ============================================================================

export type RgbColor = {
  r: number;
  g: number;
  b: number;
};

// ============================================================================
// Elasticsearch Index Types
// ============================================================================

export type IndexedWork = Displayable & {
  query: Record<string, unknown>;
  filterableValues: Record<string, unknown>;
  aggregatableValues: Record<string, unknown>;
  redirectTo?: string;
};

export type IndexedImage = Displayable & {
  query: Record<string, unknown>;
  filterableValues: Record<string, unknown>;
  aggregatableValues: Record<string, unknown>;
  vectorValues?: {
    features?: number[];
  };
};
