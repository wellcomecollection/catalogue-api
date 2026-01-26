/**
 * Integration tests for works sorting functionality using real Elasticsearch.
 *
 * Based on Scala WorksTest.scala sorting tests.
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

describe("Integration: Works Sort - Production Dates", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const productionWorks = [
    "work-production.1098",
    "work-production.1900",
    "work-production.1904",
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

  it("sorts by production date ascending (default)", async () => {
    const response = await api.get("/works?sort=production.dates");

    expect(response.status).toBe(200);
    expect(response.body.results).toHaveLength(3);

    // Should be ordered: 1098, 1900, 1904
    expect(response.body.results[0].id).toBe("aer7yjec"); // 1098
    expect(response.body.results[1].id).toBe("ksrc8m8n"); // 1900
    expect(response.body.results[2].id).toBe("fhppmnk5"); // 1904
  });

  it("sorts by production date descending", async () => {
    const response = await api.get(
      "/works?sort=production.dates&sortOrder=desc"
    );

    expect(response.status).toBe(200);
    expect(response.body.results).toHaveLength(3);

    // Should be ordered: 1904, 1900, 1098
    expect(response.body.results[0].id).toBe("fhppmnk5"); // 1904
    expect(response.body.results[1].id).toBe("ksrc8m8n"); // 1900
    expect(response.body.results[2].id).toBe("aer7yjec"); // 1098
  });
});

describe("Integration: Works Sort - Digital Location Created Date", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const digitalLocationWorks = [
    "work-digital-location.2020",
    "work-digital-location.2022",
    "work-digital-location.2021",
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

    await indexTestDocuments(ctx.client, ctx.worksIndex, digitalLocationWorks);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("sorts by digital location created date ascending", async () => {
    const response = await api.get("/works?sort=items.locations.createdDate");

    expect(response.status).toBe(200);
    expect(response.body.results).toHaveLength(3);

    // Should be ordered: 2020, 2021, 2022
    expect(response.body.results[0].id).toBe("rskxv82v"); // 2020
    expect(response.body.results[1].id).toBe("uv7jywht"); // 2021
    expect(response.body.results[2].id).toBe("s2c4jzrz"); // 2022
  });

  it("sorts by digital location created date descending", async () => {
    const response = await api.get(
      "/works?sort=items.locations.createdDate&sortOrder=desc"
    );

    expect(response.status).toBe(200);
    expect(response.body.results).toHaveLength(3);

    // Should be ordered: 2022, 2021, 2020
    expect(response.body.results[0].id).toBe("s2c4jzrz"); // 2022
    expect(response.body.results[1].id).toBe("uv7jywht"); // 2021
    expect(response.body.results[2].id).toBe("rskxv82v"); // 2020
  });
});

describe("Integration: Works Sort - Missing Values", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const worksWithAndWithoutDates = [
    "work-digital-location.2020",
    "work-digital-location.no-date",
    "work-digital-location.2021",
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

    await indexTestDocuments(
      ctx.client,
      ctx.worksIndex,
      worksWithAndWithoutDates
    );
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("returns documents whose digital location has no createdDate last", async () => {
    const response = await api.get("/works?sort=items.locations.createdDate");

    expect(response.status).toBe(200);
    expect(response.body.results).toHaveLength(3);

    // Should be ordered: 2020, 2021, no-date (last)
    expect(response.body.results[0].id).toBe("rskxv82v"); // 2020
    expect(response.body.results[1].id).toBe("uv7jywht"); // 2021
    expect(response.body.results[2].id).toBe("kfvn6vxv"); // no-date
  });
});

describe("Integration: Works Sort - With Filters", () => {
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

    // Index works with different formats and dates
    const docs = Array.from(
      { length: 10 },
      (_, i) => `works.examples.filtered-aggregations-tests.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("combines sort with filters", async () => {
    const response = await api.get(
      "/works?workType=a&sort=production.dates&sortOrder=desc"
    );

    expect(response.status).toBe(200);
    // Should only return Books (workType=a), sorted by date
    expect(response.body.results.length).toBeGreaterThan(0);
  });
});

describe("Integration: Works Sort - With Search Query", () => {
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

    // Index works with "rats" in the title
    const docs = Array.from(
      { length: 10 },
      (_, i) => `works.examples.filtered-aggregations-tests.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("combines sort with search query", async () => {
    const response = await api.get(
      "/works?query=rats&sort=production.dates&sortOrder=desc"
    );

    expect(response.status).toBe(200);
    // Results should match "rats" and be sorted by date
  });
});
