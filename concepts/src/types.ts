import { PaginationResponse } from "./controllers/pagination";
import { ResilientElasticClient } from "./services/elasticsearch";

export type Clients = {
  elastic: ResilientElasticClient;
};

export type Displayable<T = any> = {
  display: T;
};

export type Concept = {
  id: string;
  identifiers: Identifier[];
  label: string;
  alternativeLabels: [];
  type: ConceptType;
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
  | "Concept"
  | "Person"
  | "Organisation"
  | "Meeting"
  | "Period"
  | "Subject";
