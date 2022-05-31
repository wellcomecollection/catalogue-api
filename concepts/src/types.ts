export type Concept = {
  id: string;
  identifiers: Identifier[];
  label: string;
  type: ConceptType;
};

export type ResultList<Result> = {
  type: "ResultList";
  pageSize: number;
  totalPages: number;
  totalResults: number;
  results: Result[];
  nextPage?: string;
  prevPage?: string;
};

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
