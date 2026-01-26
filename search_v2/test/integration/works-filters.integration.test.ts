/**
 * Integration tests for works filter functionality using real Elasticsearch.
 *
 * Based on Scala WorksFiltersTest.scala - tests actual filtering behavior
 * with real ES queries.
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

describe("Integration: Works Filters - Date Range", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const productionWorks = [
    "work-production.1098",
    "work-production.1900",
    "work-production.1904",
    "work-production.1976",
    "work-production.2020",
  ];

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

    await indexTestDocuments(ctx.client, ctx.worksIndex, productionWorks);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by date range", async () => {
    const response = await api.get(
      "/works?production.dates.from=1900-01-01&production.dates.to=1960-01-01"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);

    const ids = response.body.results.map((r: { id: string }) => r.id);
    expect(ids).toContain("ksrc8m8n"); // work-production.1900
    expect(ids).toContain("fhppmnk5"); // work-production.1904
  });

  it("filters by from date only", async () => {
    const response = await api.get("/works?production.dates.from=1900-01-01");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(4);

    const ids = response.body.results.map((r: { id: string }) => r.id);
    expect(ids).toContain("ksrc8m8n"); // 1900
    expect(ids).toContain("fhppmnk5"); // 1904
    expect(ids).toContain("n3b8jjdd"); // 1976
    expect(ids).toContain("qp7mtyyt"); // 2020
    expect(ids).not.toContain("aer7yjec"); // 1098
  });

  it("filters by to date only", async () => {
    const response = await api.get("/works?production.dates.to=1960-01-01");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);

    const ids = response.body.results.map((r: { id: string }) => r.id);
    expect(ids).toContain("aer7yjec"); // 1098
    expect(ids).toContain("ksrc8m8n"); // 1900
    expect(ids).toContain("fhppmnk5"); // 1904
  });
});

describe("Integration: Works Filters - Location Type", () => {
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
      "work.items-with-location-types.0",
      "work.items-with-location-types.1",
      "work.items-with-location-types.2",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters works by item LocationType", async () => {
    const response = await api.get(
      "/works?items.locations.locationType=iiif-presentation,closed-stores"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);

    const ids = response.body.results.map((r: { id: string }) => r.id);
    // work.items-with-location-types.1 and .2 have these location types
    expect(ids).toHaveLength(2);
  });
});

describe("Integration: Works Filters - License", () => {
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
      "works.items-with-licenses.0",
      "works.items-with-licenses.1",
      "works.items-with-licenses.2",
      "works.items-with-licenses.3",
      "works.items-with-licenses.4",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by license", async () => {
    const response = await api.get("/works?items.locations.license=cc-by");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);
  });

  it("filters by multiple licenses", async () => {
    const response = await api.get(
      "/works?items.locations.license=cc-by,cc-by-nc"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(4);
  });
});

describe("Integration: Works Filters - Genre ID", () => {
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
      "works.examples.genre-filters-tests.0",
      "works.examples.genre-filters-tests.1",
      "works.examples.genre-filters-tests.2",
      "works.examples.genre-filters-tests.3",
      "works.examples.genre-filters-tests.4",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("does not apply the filter if there are no values provided", async () => {
    const response = await api.get("/works?genres=");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(5);
  });

  it("filters by one concept id", async () => {
    const response = await api.get("/works?genres=baadf00d");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("filters containing multiple concept ids return documents containing ANY of the requested ids", async () => {
    const response = await api.get("/works?genres=g00dcafe,baadf00d");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(4);
  });
});

describe("Integration: Works Filters - Identifiers", () => {
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

  it("filters by a sourceIdentifier", async () => {
    // Aic5qOhRoS is sourceIdentifier from work.visible.everything.0
    const response = await api.get("/works?identifiers=Aic5qOhRoS");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });

  it("filters by multiple sourceIdentifiers", async () => {
    const response = await api.get("/works?identifiers=Aic5qOhRoS,LMVvWxgXRS");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("filters by an otherIdentifier", async () => {
    // Hq3k05Fqag is an otherIdentifier from work.visible.everything.2
    const response = await api.get("/works?identifiers=Hq3k05Fqag");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });
});

describe("Integration: Works Filters - Access Status", () => {
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

    const docs = Array.from(
      { length: 7 },
      (_, i) => `works.examples.access-status-filters-tests.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("includes works by access status", async () => {
    const response = await api.get(
      "/works?items.locations.accessConditions.status=restricted,closed"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);
  });

  it("includes works which are licensed resources", async () => {
    const response = await api.get(
      "/works?items.locations.accessConditions.status=licensed-resources"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("excludes works by access status", async () => {
    const response = await api.get(
      "/works?items.locations.accessConditions.status=!restricted,!closed"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(4);
  });
});

describe("Integration: Works Filters - Availabilities", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const availabilityDocs = [
    "works.examples.availabilities.open-only",
    "works.examples.availabilities.closed-only",
    "works.examples.availabilities.online-only",
    "works.examples.availabilities.everywhere",
    "works.examples.availabilities.nowhere",
  ];

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

    await indexTestDocuments(ctx.client, ctx.worksIndex, availabilityDocs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by availability ID", async () => {
    const response = await api.get("/works?availabilities=open-shelves");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("filters by multiple comma-separated availability IDs", async () => {
    const response = await api.get(
      "/works?availabilities=open-shelves,closed-stores"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);
  });
});

describe("Integration: Works Filters - Relation Filters", () => {
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

  it("filters partOf by id", async () => {
    // nrvdy0jg is a partOf id from work.visible.everything.0
    const response = await api.get("/works?partOf=nrvdy0jg");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });

  it("filters partOf by title", async () => {
    // title-MS5Hy6x38N is a partOf title from work.visible.everything.0
    const response = await api.get("/works?partOf.title=title-MS5Hy6x38N");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });
});

describe("Integration: Works Filters - Item Filters", () => {
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

  it("filters by canonical ID on items", async () => {
    // a7xxlndb is an item canonicalId from work.visible.everything.0
    const response = await api.get("/works?items=a7xxlndb");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });

  it("looks up multiple canonical IDs", async () => {
    const response = await api.get("/works?items=a7xxlndb,sr0le4q0");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("looks up source identifiers", async () => {
    // dG0mvvCJtU is an item sourceIdentifier from work.visible.everything.0
    const response = await api.get("/works?items.identifiers=dG0mvvCJtU");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });
});

describe("Integration: Works Filters - Combined Filters", () => {
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

  it("combines multiple filters", async () => {
    // From WorksFiltersTest: genres.label=Uw1LvlTE5c&subjects.label=RGOo9Fg6ic
    // should return work.visible.everything.0
    const response = await api.get(
      "/works?genres.label=Uw1LvlTE5c&subjects.label=RGOo9Fg6ic"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });
});
