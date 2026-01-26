/**
 * Integration tests for works query/search functionality using real Elasticsearch.
 *
 * Based on Scala WorksQueryTest.scala - tests free text search, ID lookups,
 * and various field searches.
 */

import supertest from "supertest";
import createApp from "../../src/app";
import { Config } from "../../config";
import {
  IntegrationTestContext,
  setupIntegrationTest,
  teardownIntegrationTest,
  indexTestDocuments,
} from "./helpers";

describe("Integration: Works Query - ID Search", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  beforeAll(async () => {
    ctx = await setupIntegrationTest();

    const config: Config = {
      pipelineDate: "2022-02-22",
      indexDateWorks: "2022-02-22",
      indexDateImages: "2022-02-22",
      worksIndex: ctx.worksIndex,
      imagesIndex: ctx.imagesIndex,
      publicRootUrl: new URL("http://api.test/catalogue/v2"),
      defaultPageSize: 10,
      maxPageSize: 100,
    };

    const app = createApp({ elastic: ctx.client }, config);
    api = supertest(app);

    // Index visible works for ID search tests
    const docs = [
      "works.visible.0",
      "works.visible.1",
      "works.visible.2",
      "works.visible.3",
      "works.visible.4",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("searches by canonicalId", async () => {
    const response = await api.get("/works?query=2twopft1");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(1);
    expect(response.body.results[0].id).toBe("2twopft1");
  });

  it("matches IDs case insensitively", async () => {
    const response = await api.get("/works?query=2TWOPFT1");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(1);
    expect(response.body.results[0].id).toBe("2twopft1");
  });

  it("matches multiple IDs", async () => {
    const response = await api.get("/works?query=2twopft1%20dph7sjip");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(2);
    const ids = response.body.results.map((r: { id: string }) => r.id);
    expect(ids).toContain("2twopft1");
    expect(ids).toContain("dph7sjip");
  });

  it("does not match on partial IDs", async () => {
    const response = await api.get("/works?query=7sji");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(0);
  });

  it("does not match partially matching source identifiers", async () => {
    const response = await api.get("/works?query=2two");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(0);
  });
});

describe("Integration: Works Query - Source Identifiers", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  beforeAll(async () => {
    ctx = await setupIntegrationTest();

    const config: Config = {
      pipelineDate: "2022-02-22",
      indexDateWorks: "2022-02-22",
      indexDateImages: "2022-02-22",
      worksIndex: ctx.worksIndex,
      imagesIndex: ctx.imagesIndex,
      publicRootUrl: new URL("http://api.test/catalogue/v2"),
      defaultPageSize: 10,
      maxPageSize: 100,
    };

    const app = createApp({ elastic: ctx.client }, config);
    api = supertest(app);

    // Index works with everything including identifiers
    const docs = [
      "work.visible.everything.0",
      "work.visible.everything.1",
      "work.visible.everything.2",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("searches by sourceIdentifier", async () => {
    // Uaequ81tpB is from works.visible.0
    const response = await api.get("/works?query=Uaequ81tpB");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });

  it("searches by otherIdentifiers", async () => {
    // UfcQYSxE7g is an otherIdentifier from work.visible.everything.0
    const response = await api.get("/works?query=UfcQYSxE7g");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });
});

describe("Integration: Works Query - Item Identifiers", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  beforeAll(async () => {
    ctx = await setupIntegrationTest();

    const config: Config = {
      pipelineDate: "2022-02-22",
      indexDateWorks: "2022-02-22",
      indexDateImages: "2022-02-22",
      worksIndex: ctx.worksIndex,
      imagesIndex: ctx.imagesIndex,
      publicRootUrl: new URL("http://api.test/catalogue/v2"),
      defaultPageSize: 10,
      maxPageSize: 100,
    };

    const app = createApp({ elastic: ctx.client }, config);
    api = supertest(app);

    const docs = [
      "work.visible.everything.0",
      "work.visible.everything.1",
      "work.visible.everything.2",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("searches by canonical ID on items", async () => {
    // ejk7jwcd is an item canonicalId from work.visible.everything.0
    const response = await api.get("/works?query=ejk7jwcd");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });

  it("searches by source identifiers on items", async () => {
    // GWWFxlGgZX is an item sourceIdentifier from work.visible.everything.0
    const response = await api.get("/works?query=GWWFxlGgZX");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });
});

describe("Integration: Works Query - Title Search", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  beforeAll(async () => {
    ctx = await setupIntegrationTest();

    const config: Config = {
      pipelineDate: "2022-02-22",
      indexDateWorks: "2022-02-22",
      indexDateImages: "2022-02-22",
      worksIndex: ctx.worksIndex,
      imagesIndex: ctx.imagesIndex,
      publicRootUrl: new URL("http://api.test/catalogue/v2"),
      defaultPageSize: 10,
      maxPageSize: 100,
    };

    const app = createApp({ elastic: ctx.client }, config);
    api = supertest(app);

    const docs = ["work-title-dodo", "work-title-mouse"];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("returns matching results if doing a full-text search", async () => {
    const response = await api.get("/works?query=dodo");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(1);
    expect(response.body.results[0].id).toBe("oo7gwaah");
  });

  it("returns empty results for non-matching query", async () => {
    const response = await api.get("/works?query=cat");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(0);
  });

  it("searches lettering", async () => {
    // "A line of legible ligatures" is lettering from work-title-dodo
    const response = await api.get(
      "/works?query=A%20line%20of%20legible%20ligatures"
    );

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
    expect(response.body.results[0].id).toBe("oo7gwaah");
  });
});

describe("Integration: Works Query - Label Search", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  beforeAll(async () => {
    ctx = await setupIntegrationTest();

    const config: Config = {
      pipelineDate: "2022-02-22",
      indexDateWorks: "2022-02-22",
      indexDateImages: "2022-02-22",
      worksIndex: ctx.worksIndex,
      imagesIndex: ctx.imagesIndex,
      publicRootUrl: new URL("http://api.test/catalogue/v2"),
      defaultPageSize: 10,
      maxPageSize: 100,
    };

    const app = createApp({ elastic: ctx.client }, config);
    api = supertest(app);

    const docs = [
      "work.visible.everything.0",
      "work.visible.everything.1",
      "work.visible.everything.2",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("searches for contributors", async () => {
    // person-eKZIqbV783 is a contributor label from work.visible.everything.0
    const response = await api.get("/works?query=person-eKZIqbV783");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });

  it("searches genre labels", async () => {
    // 9tQdPt3acHhNKnN is a genre label from work.visible.everything.0
    const response = await api.get("/works?query=9tQdPt3acHhNKnN");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });

  it("searches subject labels", async () => {
    // goKOwWLrIbnrzZj is a subject label from work.visible.everything.0
    const response = await api.get("/works?query=goKOwWLrIbnrzZj");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });
});

describe("Integration: Works Query - Collection Path", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  beforeAll(async () => {
    ctx = await setupIntegrationTest();

    const config: Config = {
      pipelineDate: "2022-02-22",
      indexDateWorks: "2022-02-22",
      indexDateImages: "2022-02-22",
      worksIndex: ctx.worksIndex,
      imagesIndex: ctx.imagesIndex,
      publicRootUrl: new URL("http://api.test/catalogue/v2"),
      defaultPageSize: 10,
      maxPageSize: 100,
    };

    const app = createApp({ elastic: ctx.client }, config);
    api = supertest(app);

    const docs = [
      "works.collection-path.NUFFINK",
      "works.collection-path.PPCRI",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("searches for collection in collectionPath.path", async () => {
    const response = await api.get("/works?query=PPCRI");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
    // The PPCRI work should be the top result
    const ids = response.body.results.map((r: { id: string }) => r.id);
    expect(ids).toContain("mvqqvcvu");
  });

  it("searches for collection in collectionPath.label", async () => {
    const response = await api.get("/works?query=PP%2FCRI");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBeGreaterThanOrEqual(1);
  });
});
