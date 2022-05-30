import express from "express";

const app = express();

const concepts: Record<string, any> = {
  azxzhnuh: {
    id: "azxzhnuh",
    identifiers: [
      {
        identifierType: "lc-names",
        value: "n12345678",
        type: "Identifier",
      },
    ],
    label: "Florence Nightingale",
    alternativeLabels: ["The Lady with the Lamp"],
    type: "Person",
  },
};

app.get("/concepts", (req, res) => {
  res.status(200).json({
    type: "ResultList",
    results: Object.values(concepts),
  });
});

app.get("/concepts/:id", (req, res) => {
  const concept = concepts[req.params.id];
  if (concept) {
    res.status(200).json(concept);
  } else {
    res.sendStatus(404);
  }
});

export default app;
