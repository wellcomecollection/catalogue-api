import { mockedWorksApi } from "./fixtures/api";
import {
  work,
  workWithSubjects,
  workWithContributors,
  workWithProduction,
  indexedWork,
} from "./fixtures/works";
import { Work } from "../src/types";

// Helper to create works with various includes
const workWithIdentifiers = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  identifiers: [
    {
      identifierType: {
        id: "sierra-system-number",
        label: "Sierra system number",
        type: "IdentifierType",
      },
      value: "b12345678",
      type: "Identifier",
    },
  ],
});

const workWithItems = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  items: [
    {
      id: "item-1",
      identifiers: [
        {
          identifierType: {
            id: "sierra-system-number",
            label: "Sierra system number",
            type: "IdentifierType",
          },
          value: "i12345678",
          type: "Identifier",
        },
      ],
      locations: [
        {
          locationType: {
            id: "iiif-presentation",
            label: "IIIF Presentation API",
            type: "LocationType",
          },
          url: "https://iiif.example.org/presentation/v3/b12345678",
          license: {
            id: "cc-by",
            label: "Attribution 4.0 International (CC BY 4.0)",
            url: "http://creativecommons.org/licenses/by/4.0/",
            type: "License",
          },
          accessConditions: [],
          type: "DigitalLocation",
        },
      ],
      type: "Item",
    },
  ],
});

const workWithGenres = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  genres: [
    {
      label: "Manuscripts",
      concepts: [
        {
          id: "hqpfg3vq",
          label: "Manuscripts",
          type: "Concept",
        },
      ],
      type: "Genre",
    },
  ],
});

const workWithLanguages = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  languages: [
    {
      id: "eng",
      label: "English",
      type: "Language",
    },
  ],
});

const workWithNotes = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  notes: [
    {
      contents: ["This is a test note"],
      noteType: {
        id: "general-note",
        label: "General note",
        type: "NoteType",
      },
      type: "Note",
    },
  ],
});

const workWithImages = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  images: [
    {
      id: "image-1",
      type: "Image",
    },
  ],
});

const workWithParts = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  parts: [
    {
      id: "child-work-1",
      title: "Child Work 1",
      type: "Work",
    },
  ],
});

const workWithPartOf = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  partOf: [
    {
      id: "parent-work-1",
      title: "Parent Collection",
      type: "Work",
    },
  ],
});

const workWithPrecededBy = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  precededBy: [
    {
      id: "preceding-work-1",
      title: "Volume 1",
      type: "Work",
    },
  ],
});

const workWithSucceededBy = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  succeededBy: [
    {
      id: "succeeding-work-1",
      title: "Volume 3",
      type: "Work",
    },
  ],
});

const workWithHoldings = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  holdings: [
    {
      enumeration: ["v.1", "v.2"],
      location: {
        locationType: {
          id: "closed-stores",
          label: "Closed stores",
          type: "LocationType",
        },
        label: "Closed stores",
        accessConditions: [],
        type: "PhysicalLocation",
      },
      type: "Holdings",
    },
  ],
});

const workWithFormerFrequency = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  formerFrequency: ["Published in 2001", "Published in 2002"],
});

const workWithDesignation = (
  options: { id?: string; title?: string } = {}
): Work => ({
  ...work(options),
  designation: ["Designation #1", "Designation #2", "Designation #3"],
});

describe("GET /works includes", () => {
  describe("include=identifiers", () => {
    it("includes identifiers in list response", async () => {
      const testWork = workWithIdentifiers({ id: "work-with-identifiers" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=identifiers");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].identifiers).toBeDefined();
      expect(response.body.results[0].identifiers).toHaveLength(1);
      expect(response.body.results[0].identifiers[0].value).toBe("b12345678");
    });

    it("includes identifiers in single work response", async () => {
      const testWork = workWithIdentifiers({ id: "work-with-identifiers" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get(
        "/works/work-with-identifiers?include=identifiers"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.identifiers).toBeDefined();
    });

    it("does not include identifiers by default", async () => {
      const testWork = workWithIdentifiers({ id: "work-with-identifiers" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-identifiers");
      expect(response.statusCode).toBe(200);
      expect(response.body.identifiers).toBeUndefined();
    });
  });

  describe("include=items", () => {
    it("includes items in response", async () => {
      const testWork = workWithItems({ id: "work-with-items" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=items");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].items).toBeDefined();
      expect(response.body.results[0].items).toHaveLength(1);
    });

    it("does not include items by default", async () => {
      const testWork = workWithItems({ id: "work-with-items" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-items");
      expect(response.statusCode).toBe(200);
      expect(response.body.items).toBeUndefined();
    });
  });

  describe("include=subjects", () => {
    it("includes subjects in response", async () => {
      const testWork = workWithSubjects({ id: "work-with-subjects" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=subjects");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].subjects).toBeDefined();
    });

    it("does not include subjects by default", async () => {
      const testWork = workWithSubjects({ id: "work-with-subjects" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-subjects");
      expect(response.statusCode).toBe(200);
      expect(response.body.subjects).toBeUndefined();
    });
  });

  describe("include=genres", () => {
    it("includes genres in response", async () => {
      const testWork = workWithGenres({ id: "work-with-genres" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=genres");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].genres).toBeDefined();
    });

    it("does not include genres by default", async () => {
      const testWork = workWithGenres({ id: "work-with-genres" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-genres");
      expect(response.statusCode).toBe(200);
      expect(response.body.genres).toBeUndefined();
    });
  });

  describe("include=contributors", () => {
    it("includes contributors in response", async () => {
      const testWork = workWithContributors({ id: "work-with-contributors" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=contributors");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].contributors).toBeDefined();
    });

    it("does not include contributors by default", async () => {
      const testWork = workWithContributors({ id: "work-with-contributors" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-contributors");
      expect(response.statusCode).toBe(200);
      expect(response.body.contributors).toBeUndefined();
    });
  });

  describe("include=production", () => {
    it("includes production in response", async () => {
      const testWork = workWithProduction({ id: "work-with-production" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=production");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].production).toBeDefined();
    });

    it("does not include production by default", async () => {
      const testWork = workWithProduction({ id: "work-with-production" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-production");
      expect(response.statusCode).toBe(200);
      expect(response.body.production).toBeUndefined();
    });
  });

  describe("include=languages", () => {
    it("includes languages in response", async () => {
      const testWork = workWithLanguages({ id: "work-with-languages" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=languages");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].languages).toBeDefined();
    });

    it("does not include languages by default", async () => {
      const testWork = workWithLanguages({ id: "work-with-languages" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-languages");
      expect(response.statusCode).toBe(200);
      expect(response.body.languages).toBeUndefined();
    });
  });

  describe("include=notes", () => {
    it("includes notes in response", async () => {
      const testWork = workWithNotes({ id: "work-with-notes" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=notes");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].notes).toBeDefined();
    });
  });

  describe("include=images", () => {
    it("includes images in response", async () => {
      const testWork = workWithImages({ id: "work-with-images" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=images");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].images).toBeDefined();
    });
  });

  describe("include=parts", () => {
    it("includes parts in response", async () => {
      const testWork = workWithParts({ id: "work-with-parts" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=parts");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].parts).toBeDefined();
    });
  });

  describe("include=partOf", () => {
    it("includes partOf in response", async () => {
      const testWork = workWithPartOf({ id: "work-with-partOf" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=partOf");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].partOf).toBeDefined();
    });
  });

  describe("include=precededBy", () => {
    it("includes precededBy in response", async () => {
      const testWork = workWithPrecededBy({ id: "work-with-precededBy" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=precededBy");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].precededBy).toBeDefined();
    });
  });

  describe("include=succeededBy", () => {
    it("includes succeededBy in response", async () => {
      const testWork = workWithSucceededBy({ id: "work-with-succeededBy" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=succeededBy");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].succeededBy).toBeDefined();
    });
  });

  describe("include=holdings", () => {
    it("includes holdings in response", async () => {
      const testWork = workWithHoldings({ id: "work-with-holdings" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=holdings");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].holdings).toBeDefined();
    });
  });

  describe("include=formerFrequency", () => {
    it("includes formerFrequency in list response", async () => {
      const testWork = workWithFormerFrequency({ id: "work-with-frequency" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=formerFrequency");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].formerFrequency).toBeDefined();
      expect(response.body.results[0].formerFrequency).toEqual([
        "Published in 2001",
        "Published in 2002",
      ]);
    });

    it("includes formerFrequency in single work response", async () => {
      const testWork = workWithFormerFrequency({ id: "work-with-frequency" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get(
        "/works/work-with-frequency?include=formerFrequency"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.formerFrequency).toBeDefined();
    });

    it("does not include formerFrequency by default", async () => {
      const testWork = workWithFormerFrequency({ id: "work-with-frequency" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-frequency");
      expect(response.statusCode).toBe(200);
      expect(response.body.formerFrequency).toBeUndefined();
    });
  });

  describe("include=designation", () => {
    it("includes designation in list response", async () => {
      const testWork = workWithDesignation({ id: "work-with-designation" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=designation");
      expect(response.statusCode).toBe(200);
      expect(response.body.results[0].designation).toBeDefined();
      expect(response.body.results[0].designation).toEqual([
        "Designation #1",
        "Designation #2",
        "Designation #3",
      ]);
    });

    it("includes designation in single work response", async () => {
      const testWork = workWithDesignation({ id: "work-with-designation" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get(
        "/works/work-with-designation?include=designation"
      );
      expect(response.statusCode).toBe(200);
      expect(response.body.designation).toBeDefined();
    });

    it("does not include designation by default", async () => {
      const testWork = workWithDesignation({ id: "work-with-designation" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works/work-with-designation");
      expect(response.statusCode).toBe(200);
      expect(response.body.designation).toBeUndefined();
    });
  });

  describe("multiple includes", () => {
    it("includes multiple fields when requested", async () => {
      const testWork: Work = {
        ...workWithSubjects({ id: "work-multi-include" }),
        ...workWithContributors(),
        ...workWithLanguages(),
      };
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get(
        "/works?include=subjects,contributors,languages"
      );
      expect(response.statusCode).toBe(200);
    });

    it("returns 400 for invalid includes mixed with valid ones", async () => {
      const testWork = workWithSubjects({ id: "work-mixed-include" });
      const api = mockedWorksApi([indexedWork(testWork)]);

      const response = await api.get("/works?include=subjects,invalid,genres");
      expect(response.statusCode).toBe(400);
      expect(response.body.description).toContain("'invalid'");
      expect(response.body.description).toContain("is not a valid value");
    });
  });
});
