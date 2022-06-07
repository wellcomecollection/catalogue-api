import { PaginationResponse } from "./controllers/pagination";

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
