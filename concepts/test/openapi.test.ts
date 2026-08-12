import fs from "fs";
import path from "path";
import Ajv2020 from "ajv/dist/2020";
import { parse } from "yaml";
import createApp from "../src/app";
import { defaultPageSize, limits } from "../src/controllers/pagination";
import { concept } from "./fixtures/concepts";

/**
 * Checks that reference/catalogue.yaml describes what this service serves.
 *
 * Express can enumerate its own routes, so unlike the search API's equivalent test
 * (search/src/test/scala/weco/api/search/openapi/) this compares both directions: a
 * documented endpoint that doesn't exist, and an endpoint added here that nobody
 * wrote down.
 */

const specPath = path.resolve(__dirname, "../../reference/catalogue.yaml");
const spec = parse(fs.readFileSync(specPath, "utf8"));

/**
 * Endpoints this service serves on purpose but does not document. If you document
 * one of these, delete it from here.
 */
const undocumentedInternalPaths = ["/management/healthcheck"];

/** The routes express will actually match, e.g. `/concepts/:id`. */
const servedPaths = (): string[] => {
  const app = createApp(
    { elastic: {} as never },
    {
      conceptsIndex: "test-index",
      pipelineDate: "2022-02-22",
      publicRootUrl: new URL("http://concepts.test"),
    }
  );

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const stack = (app as any)._router.stack as any[];
  return stack.filter((layer) => layer.route).map((layer) => layer.route.path);
};

/** `/concepts/{id}` in the spec is `/concepts/:id` in express. */
const asExpressPath = (specPath: string): string =>
  specPath.replace(/\{(\w+)\}/g, ":$1");

const specPathsTaggedConcepts = (): string[] =>
  Object.entries(spec.paths as Record<string, { get: { tags: string[] } }>)
    .filter(([, operations]) => operations.get.tags.includes("Concepts"))
    .map(([p]) => p);

const allSpecPaths = (): string[] => Object.keys(spec.paths);

describe("the endpoints this service serves", () => {
  it("serves exactly the concepts endpoints the spec documents", () => {
    const documented = specPathsTaggedConcepts().map(asExpressPath);
    const served = servedPaths().filter(
      (p) => !undocumentedInternalPaths.includes(p)
    );

    expect(documented.length).toBeGreaterThan(0);
    expect(served.sort()).toEqual(documented.sort());
  });

  it("finds the routes it is looking for", () => {
    // A negative control: without this, the assertion above would pass if
    // servedPaths() silently returned nothing.
    expect(servedPaths()).toContain("/concepts");
  });
});

describe("the endpoints this service keeps out of the spec", () => {
  it("serves the internal endpoints", () => {
    undocumentedInternalPaths.forEach((p) => {
      expect(servedPaths()).toContain(p);
    });
  });

  it("does not document the internal endpoints", () => {
    undocumentedInternalPaths.forEach((p) => {
      expect(allSpecPaths()).not.toContain(p);
    });
  });
});

// Response-schema checks mirroring the search API's OpenApiSpecResponseTest:
// the pipeline-generated fixtures (concepts.*.json) must validate against the
// Concept schema, contain no undocumented keys, and collectively exhibit every
// documented field (compared per component, so `Identifier.value` counts
// wherever an identifier appears).
describe("the response schemas", () => {
  const fixturesDir = path.resolve(
    __dirname,
    "../../common/search/src/test/resources/test_documents"
  );

  const conceptFixtures: { name: string; display: any }[] = fs
    .readdirSync(fixturesDir)
    .filter((f) => f.startsWith("concepts.") && f.endsWith(".json"))
    .sort()
    .map((f) => ({
      name: f,
      display: JSON.parse(fs.readFileSync(path.join(fixturesDir, f), "utf8"))
        .document.display,
    }));

  const ajv = new Ajv2020({ strict: false });
  const validateConcept = ajv.compile({
    $ref: "#/components/schemas/Concept",
    components: spec.components,
  });

  it("finds fixtures to check", () => {
    // A negative control: everything below would pass vacuously on an empty set.
    expect(conceptFixtures.length).toBeGreaterThanOrEqual(4);
  });

  it("accepts every concept document the pipeline generates", () => {
    conceptFixtures.forEach(({ name, display }) => {
      const valid = validateConcept(display);
      expect({ name, errors: valid ? [] : validateConcept.errors }).toEqual({
        name,
        errors: [],
      });
    });
  });

  it("rejects a concept document with the wrong shape", () => {
    // A second control: the validator must actually reject something.
    expect(validateConcept({ id: 12345, type: "Concept" })).toBe(false);
  });

  it("rejects a concept document with a missing field", () => {
    // Guards the schema's required list: without it, ajv passes {} and a
    // dropped field would never fail validation.
    expect(validateConcept({})).toBe(false);
  });

  it("keeps types.ts and its fixture builder aligned with the schema", () => {
    const built = concept();
    const valid = validateConcept(built);
    expect(valid ? [] : validateConcept.errors).toEqual([]);
  });

  const schemaOf = (name: string): any => spec.components.schemas[name];
  const refNameOf = (schema: any): string | undefined =>
    schema?.$ref?.replace("#/components/schemas/", "");

  // The walks understand $ref, properties and items only; anything else must
  // fail loudly rather than silently under-covering a subtree.
  const assertWalkable = (schema: any, p: string): void => {
    ["oneOf", "allOf", "anyOf", "patternProperties"].forEach((keyword) => {
      if (schema?.[keyword] !== undefined) {
        throw new Error(`${p} uses ${keyword}, which these walks don't handle`);
      }
    });
  };

  // Every property of every component reachable from the Concept schema, as
  // `Component.property` paths; inline sub-objects extend the dotted path.
  const documentedFields = (): Set<string> => {
    const paths = new Set<string>();
    const visited = new Set(["Concept"]);
    const queue = ["Concept"];

    const follow = (schema: any, p: string): void => {
      const ref = refNameOf(schema);
      if (ref !== undefined) {
        if (!visited.has(ref)) {
          visited.add(ref);
          queue.push(ref);
        }
        return;
      }
      walk(schema, p);
    };

    const walk = (schema: any, p: string): void => {
      assertWalkable(schema, p);
      Object.entries(schema?.properties ?? {}).forEach(([key, sub]) => {
        paths.add(`${p}.${key}`);
        follow(sub, `${p}.${key}`);
      });
      if (schema?.items !== undefined) follow(schema.items, `${p}[]`);
    };

    while (queue.length > 0) {
      const name = queue.shift() as string;
      walk(schemaOf(name), name);
    }
    return paths;
  };

  // Walks a document alongside its schema, recording the documented fields it
  // exhibits and any keys the schema doesn't know about. Paths restart at each
  // named component, matching documentedFields.
  const walkDocument = (
    schema: any,
    doc: any,
    p: string,
    observed: Set<string>,
    undocumented: Set<string>
  ): void => {
    const ref = refNameOf(schema);
    const base = ref ?? p;
    const resolved = ref !== undefined ? schemaOf(ref) : schema;
    assertWalkable(resolved, base);

    if (Array.isArray(doc)) {
      if (resolved?.items !== undefined) {
        doc.forEach((item) =>
          walkDocument(
            resolved.items,
            item,
            `${base}[]`,
            observed,
            undocumented
          )
        );
      }
      return;
    }
    if (doc === null || typeof doc !== "object") return;

    const properties = resolved?.properties;
    if (properties === undefined) return; // opaque schema; free-form objects don't report

    Object.entries(doc).forEach(([key, value]) => {
      if (properties[key] !== undefined) {
        observed.add(`${base}.${key}`);
        walkDocument(
          properties[key],
          value,
          `${base}.${key}`,
          observed,
          undocumented
        );
      } else {
        undocumented.add(`${base}.${key}`);
      }
    });
  };

  // Documented fields with no fixture yet. Each needs a reason; an entry the
  // fixtures do exercise fails the guard test below, so this cannot rot.
  const allowedUnexercised = new Set<string>([]);

  let cachedWalk: { observed: Set<string>; undocumented: Set<string> };
  const walkAllFixtures = (): typeof cachedWalk => {
    if (cachedWalk === undefined) {
      const observed = new Set<string>();
      const undocumented = new Set<string>();
      conceptFixtures.forEach(({ display }) =>
        walkDocument(
          schemaOf("Concept"),
          display,
          "Concept",
          observed,
          undocumented
        )
      );
      cachedWalk = { observed, undocumented };
    }
    return cachedWalk;
  };

  it("documents every field the pipeline's concept documents contain", () => {
    expect([...walkAllFixtures().undocumented].sort()).toEqual([]);
  });

  it("exhibits every field the spec documents", () => {
    const { observed } = walkAllFixtures();
    const missing = [...documentedFields()]
      .filter((p) => !observed.has(p) && !allowedUnexercised.has(p))
      .sort();
    expect(missing).toEqual([]);
  });

  it("does not allow fields the fixtures exercise", () => {
    const { observed } = walkAllFixtures();
    expect([...allowedUnexercised].filter((p) => observed.has(p))).toEqual([]);
  });

  it("notices a documented field that no fixture contains", () => {
    // A control: an invented property must show up in the documented set.
    const schemas = spec.components.schemas;
    schemas.Concept.properties.neverEmitted = { type: "string" };
    try {
      expect(documentedFields()).toContain("Concept.neverEmitted");
    } finally {
      delete schemas.Concept.properties.neverEmitted;
    }
  });
});

describe("the pagination parameters", () => {
  // These are shared spec parameters, used by both this service and the search API,
  // so if the two ever disagree about their limits, one of them fails here.
  const pageSizeSchema = spec.components.parameters.PageSize.schema;
  const pageSchema = spec.components.parameters.Page.schema;

  it("documents the page size limits this service enforces", () => {
    expect(pageSizeSchema.minimum).toBe(limits.minSize);
    expect(pageSizeSchema.maximum).toBe(limits.maxSize);
  });

  it("documents the default page size this service uses", () => {
    expect(pageSizeSchema.default).toBe(defaultPageSize);
  });

  it("documents the minimum page number this service enforces", () => {
    expect(pageSchema.minimum).toBe(1);
  });
});
