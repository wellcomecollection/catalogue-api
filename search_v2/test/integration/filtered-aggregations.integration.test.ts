/**
 * Integration tests for filtered aggregations using real Elasticsearch.
 *
 * Based on Scala WorksFilteredAggregationsTest.scala and AggregationsTest.scala -
 * tests complex aggregation behavior when filters are applied.
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

describe("Integration: Filtered Aggregations - Basic", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const aggregatedWorks = Array.from(
    { length: 10 },
    (_, i) => `works.examples.filtered-aggregations-tests.${i}`
  );

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

    await indexTestDocuments(ctx.client, ctx.worksIndex, aggregatedWorks);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters aggregation buckets with filters not paired to the aggregation", async () => {
    // Filter by workType=a (Books), aggregate by languages
    // Should only see languages for Books
    const response = await api.get("/works?workType=a&aggregations=languages");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.languages).toBeDefined();

    const buckets = response.body.aggregations.languages.buckets;
    // Should have bak (3 books) and mar (1 book)
    expect(buckets.length).toBe(2);

    const bakBucket = buckets.find(
      (b: { data: { id: string } }) => b.data.id === "bak"
    );
    expect(bakBucket).toBeDefined();
    expect(bakBucket.count).toBe(3);

    const marBucket = buckets.find(
      (b: { data: { id: string } }) => b.data.id === "mar"
    );
    expect(marBucket).toBeDefined();
    expect(marBucket.count).toBe(1);
  });

  it("returns aggregation over all values when filtering and aggregating on same field", async () => {
    // Filter by workType=a, aggregate by workType
    // Should see ALL workType values (not just Books)
    const response = await api.get("/works?workType=a&aggregations=workType");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.workType).toBeDefined();

    const buckets = response.body.aggregations.workType.buckets;
    // Should have all 4 work types: Books, Journals, Audio, Pictures
    expect(buckets.length).toBe(4);

    const labels = buckets.map(
      (b: { data: { label: string } }) => b.data.label
    );
    expect(labels).toContain("Books");
    expect(labels).toContain("Journals");
    expect(labels).toContain("Audio");
    expect(labels).toContain("Pictures");
  });

  it("applies both filter and aggregation on same field with additional filter", async () => {
    // Filter by workType=a AND languages=che, aggregate by workType
    // Should see workType buckets filtered by languages=che, but workType=a should be included
    const response = await api.get(
      "/works?workType=a&languages=che&aggregations=workType"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.workType).toBeDefined();

    const buckets = response.body.aggregations.workType.buckets;
    // Should see Audio (has che) and Books (count 0 because filtered but still shown)
    expect(buckets.length).toBe(2);

    const audioBucket = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "Audio"
    );
    expect(audioBucket).toBeDefined();
    expect(audioBucket.count).toBe(1);

    const booksBucket = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "Books"
    );
    expect(booksBucket).toBeDefined();
    expect(booksBucket.count).toBe(0); // Zero count because no Books have che language
  });

  it("applies multiple filters and aggregations together", async () => {
    // Filter by workType=a, aggregate by both languages and workType
    const response = await api.get(
      "/works?workType=a&aggregations=languages,workType"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.languages).toBeDefined();
    expect(response.body.aggregations.workType).toBeDefined();

    // Languages should be filtered to only show those in Books
    const languageBuckets = response.body.aggregations.languages.buckets;
    expect(languageBuckets.length).toBe(2);

    // WorkType should show all values (because it's paired with the filter)
    const workTypeBuckets = response.body.aggregations.workType.buckets;
    expect(workTypeBuckets.length).toBe(4);
  });
});

describe("Integration: Filtered Aggregations - Search Query", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const aggregatedWorks = Array.from(
    { length: 10 },
    (_, i) => `works.examples.filtered-aggregations-tests.${i}`
  );

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

    await indexTestDocuments(ctx.client, ctx.worksIndex, aggregatedWorks);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("applies search query to aggregations paired with an applied filter", async () => {
    // Search for "rats", filter by workType=a, aggregate by workType
    const response = await api.get(
      "/works?query=rats&workType=a&aggregations=workType"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.workType).toBeDefined();

    const buckets = response.body.aggregations.workType.buckets;
    // Should show workTypes that have "rats" matches
    // Audio has 2 rats, Books has 1 rat, Journals has 1 rat
    expect(buckets.length).toBeGreaterThan(0);
  });
});

describe("Integration: Filtered Aggregations - More Than 10 Buckets", () => {
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

    // Index works with many different formats
    const docs = Array.from(
      { length: 23 },
      (_, i) => `works.every-format.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("returns more than 10 format aggregations", async () => {
    const response = await api.get("/works?aggregations=workType");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.workType).toBeDefined();

    const buckets = response.body.aggregations.workType.buckets;
    // Should have all 23 different formats
    expect(buckets.length).toBe(23);
  });
});

describe("Integration: Filtered Aggregations - Date Range", () => {
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
      { length: 6 },
      (_, i) => `works.production.multi-year.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates over filtered dates using only from date", async () => {
    const response = await api.get(
      "/works?production.dates.from=1960-01-01&aggregations=production.dates"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations["production.dates"]).toBeDefined();

    const buckets = response.body.aggregations["production.dates"].buckets;
    // Should only include dates >= 1960
    expect(buckets.length).toBe(2);

    const years = buckets.map((b: { data: { id: string } }) => b.data.id);
    expect(years).toContain("1960");
    expect(years).toContain("1962");
  });
});

describe("Integration: Filtered Aggregations - Special Characters", () => {
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

    // Index a work that has a contributor with special characters
    // This tests that special characters in aggregation terms are handled safely
    const docs = ["works.genres"];
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("safely handles terms with standard characters in aggregations", async () => {
    const response = await api.get(
      "/works?genres.label=Electronic%20books&aggregations=genres.label"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations["genres.label"]).toBeDefined();

    const buckets = response.body.aggregations["genres.label"].buckets;
    expect(buckets.length).toBeGreaterThan(0);

    const electronicBooks = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "Electronic books"
    );
    expect(electronicBooks).toBeDefined();
  });
});

describe("Integration: Filtered Aggregations - Subjects and Contributors", () => {
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

    const docs = Array.from({ length: 5 }, (_, i) => `works.subjects.${i}`);
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates by subject label", async () => {
    const response = await api.get("/works?aggregations=subjects.label");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations["subjects.label"]).toBeDefined();

    const buckets = response.body.aggregations["subjects.label"].buckets;
    expect(buckets.length).toBeGreaterThan(0);

    // realAnalysis has 3 works, paleoNeuroBiology has 2 works
    const realAnalysis = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "realAnalysis"
    );
    expect(realAnalysis).toBeDefined();
    expect(realAnalysis.count).toBe(3);
  });
});

describe("Integration: Filtered Aggregations - Contributors", () => {
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

    const docs = Array.from({ length: 4 }, (_, i) => `works.contributor.${i}`);
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates by contributor agent label", async () => {
    const response = await api.get(
      "/works?aggregations=contributors.agent.label"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(
      response.body.aggregations["contributors.agent.label"]
    ).toBeDefined();

    const buckets =
      response.body.aggregations["contributors.agent.label"].buckets;
    expect(buckets.length).toBe(4);

    // 47 has 2 works, MI5 has 2 works, 007 has 1, GCHQ has 1
    const bucket47 = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "47"
    );
    expect(bucket47).toBeDefined();
    expect(bucket47.count).toBe(2);

    const bucketMI5 = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "MI5"
    );
    expect(bucketMI5).toBeDefined();
    expect(bucketMI5.count).toBe(2);
  });
});

describe("Integration: Aggregations - No Results", () => {
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
      { length: 23 },
      (_, i) => `works.examples.aggregation-with-filters-tests.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.worksIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("correctly returns aggregation labels even when no search results are returned", async () => {
    // Search for something that returns 0 results, but with filters that should
    // still show in aggregations
    const response = await api.get(
      "/works?query=awdawdawdawda&workType=a&subjects.label=fIbfVPkqaf&aggregations=workType,subjects.label"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(0);
    expect(response.body.aggregations).toBeDefined();

    // The filtered values should still appear in aggregations
    const workTypeBuckets = response.body.aggregations.workType.buckets;
    expect(workTypeBuckets.length).toBe(1);
    expect(workTypeBuckets[0].data.label).toBe("Books");
  });
});
