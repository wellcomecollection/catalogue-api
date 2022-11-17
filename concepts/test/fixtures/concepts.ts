import { Concept } from "../../src/types";

export const concept = (
  { id }: { id: string } = { id: "abcdefgh" }
): Concept => ({
  id,
  label: "Test Concept",
  alternativeLabels: [],
  identifiers: [
    {
      identifierType: {
        id: "label-derived",
        label: "Label derived",
        type: "IdentifierType",
      },
      value: "label-derived:test concept",
      type: "Identifier",
    },
  ],
  type: "Concept",
});
