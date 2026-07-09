import fs from "fs";
import path from "path";
import { parse } from "yaml";
import createApp from "../src/app";
import { defaultPageSize, limits } from "../src/controllers/pagination";

/**
 * Checks that reference/catalogue.yaml still describes what this service serves.
 *
 * Express can enumerate its own routes, so unlike the search API's equivalent test
 * (search/src/test/scala/weco/api/search/openapi/) this can check both directions:
 * a documented endpoint that doesn't exist, *and* an endpoint added here that nobody
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

describe("the pagination parameters", () => {
  // These are shared spec parameters, used by both this service and the search API.
  // If the two services ever disagree about their limits, one of them is now lying
  // to its own documentation.
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
