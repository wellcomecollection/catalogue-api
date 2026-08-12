import { PaginationResponse } from "./controllers/pagination";
import { ResilientElasticClient } from "./services/elasticsearch";

export type Clients = {
  elastic: ResilientElasticClient;
};

export type Displayable<T = any> = {
  display: T;
};

// These types mirror the pipeline's ConceptDisplay model
// (catalogue_graph/src/ingestor/models/indexable/concept.py) and the Concept
// schema in reference/catalogue.yaml. The service passes documents through
// untyped at runtime; openapi.test.ts holds these three in sync.
export type Concept = {
  id: string;
  identifiers: Identifier[];
  label: string;
  displayLabel: string;
  alternativeLabels: string[];
  description?: ConceptDescription;
  type: ConceptType;
  relatedConcepts: RelatedConcepts;
  sameAs: string[];
  displayImages: DigitalLocation[];
};

export type ConceptDescription = {
  text: string;
  sourceLabel?: string;
  sourceUrl?: string;
};

export type RelatedConcept = {
  id: string;
  label: string;
  relationshipType?: string;
  conceptType: string;
};

export type RelatedConcepts = {
  relatedTo: RelatedConcept[];
  fieldsOfWork: RelatedConcept[];
  narrowerThan: RelatedConcept[];
  broaderThan: RelatedConcept[];
  people: RelatedConcept[];
  frequentCollaborators: RelatedConcept[];
  relatedTopics: RelatedConcept[];
  foundedBy: RelatedConcept[];
};

export type DigitalLocation = {
  locationType: {
    id: string;
    label: string;
    type: "LocationType";
  };
  url: string;
  credit?: string;
  linkText?: string;
  license?: {
    id: string;
    label: string;
    url: string;
    type: "License";
  };
  accessConditions: AccessCondition[];
  type: "DigitalLocation";
};

export type AccessCondition = {
  method?: {
    id: string;
    label: string;
    type: "AccessMethod";
  };
  status?: {
    id: string;
    label: string;
    type: "AccessStatus";
  };
  terms?: string;
  note?: string;
  type: "AccessCondition";
};

export type ResultList<Result> = {
  type: "ResultList";
  results: Result[];
} & PaginationResponse;

export type Identifier = {
  identifierType: {
    id: string;
    label: string;
    type: "IdentifierType";
  };
  value: string;
  type: "Identifier";
};

export type ConceptType =
  | "Agent"
  | "Concept"
  | "Genre"
  | "Meeting"
  | "Organisation"
  | "Period"
  | "Person"
  | "Place"
  | "Subject";
