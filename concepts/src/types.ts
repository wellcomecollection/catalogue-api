import { PaginationResponse } from "./controllers/pagination";
import { Client as ElasticClient } from "@elastic/elasticsearch";

export type Clients = {
  elastic: ElasticClient;
};

export type Concept = {
  id: string;
  identifiers: Identifier[];
  label: string;
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

export type ConceptType = "Person" | "Organisation" | "Subject";
