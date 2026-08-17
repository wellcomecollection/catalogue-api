import { PaginationResponse } from "./controllers/pagination";
import { ResilientElasticClient } from "./services/elasticsearch";

export type Clients = {
  elastic: ResilientElasticClient;
};

export type Displayable<T = any> = {
  display: T;
};

// Mirrors the pipeline's ConceptDisplay and the spec's Concept schema;
// openapi.test.ts validates the fixture builder against the schema.
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
  displayImages: ConceptImage[];
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

// The subset of digital location fields concept documents carry; the
// accessConditions list is always empty.
export type ConceptImage = {
  locationType: {
    id: string;
    label: string;
    type: "LocationType";
  };
  url: string;
  accessConditions: [];
  type: "DigitalLocation";
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
