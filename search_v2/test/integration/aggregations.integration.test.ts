/**
 * Integration tests for aggregations using real Elasticsearch.
 *
 * These tests use the same test documents as the Scala tests to ensure
 * parity between the TypeScript and Scala implementations.
 *
 * To run these tests:
 * 1. Start Elasticsearch: docker-compose up -d
 * 2. Run tests: npm run test:integration
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

describe("Integration: Works Aggregations", () => {
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
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  describe("workType aggregation", () => {
    beforeAll(async () => {
      // Index format test documents (same as Scala WorksAggregationsTest)
      const formatDocs = [
        "works.formats.0.Books",
        "works.formats.1.Books",
        "works.formats.2.Books",
        "works.formats.3.Books",
        "works.formats.4.Journals",
        "works.formats.5.Journals",
        "works.formats.6.Journals",
        "works.formats.7.Audio",
        "works.formats.8.Audio",
        "works.formats.9.Pictures",
      ];
      await indexTestDocuments(ctx.client, ctx.worksIndex, formatDocs);
    }, 10000);

    it("returns workType aggregation with correct buckets", async () => {
      const response = await api.get("/works?aggregations=workType");

      expect(response.status).toBe(200);
      expect(response.body.aggregations).toBeDefined();
      expect(response.body.aggregations.workType).toBeDefined();

      const buckets = response.body.aggregations.workType.buckets;
      expect(buckets).toHaveLength(4);

      // Verify bucket order (count desc, then key asc)
      expect(buckets[0].data.id).toBe("a");
      expect(buckets[0].data.label).toBe("Books");
      expect(buckets[0].count).toBe(4);

      expect(buckets[1].data.id).toBe("d");
      expect(buckets[1].data.label).toBe("Journals");
      expect(buckets[1].count).toBe(3);

      expect(buckets[2].data.id).toBe("i");
      expect(buckets[2].data.label).toBe("Audio");
      expect(buckets[2].count).toBe(2);

      expect(buckets[3].data.id).toBe("k");
      expect(buckets[3].data.label).toBe("Pictures");
      expect(buckets[3].count).toBe(1);
    });
  });
});

describe("Integration: Works Aggregations - Languages", () => {
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

    // Index language test documents (same as Scala WorksAggregationsTest)
    const languageDocs = [
      "works.languages.0.eng",
      "works.languages.1.eng",
      "works.languages.2.eng",
      "works.languages.3.eng+swe",
      "works.languages.4.eng+swe+tur",
      "works.languages.5.swe",
      "works.languages.6.tur",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, languageDocs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("returns languages aggregation with correct buckets", async () => {
    const response = await api.get("/works?aggregations=languages");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.languages).toBeDefined();

    const buckets = response.body.aggregations.languages.buckets;
    expect(buckets).toHaveLength(3);

    // English: 5 works
    expect(buckets[0].data.id).toBe("eng");
    expect(buckets[0].data.label).toBe("English");
    expect(buckets[0].count).toBe(5);

    // Swedish: 3 works
    expect(buckets[1].data.id).toBe("swe");
    expect(buckets[1].data.label).toBe("Swedish");
    expect(buckets[1].count).toBe(3);

    // Turkish: 2 works
    expect(buckets[2].data.id).toBe("tur");
    expect(buckets[2].data.label).toBe("Turkish");
    expect(buckets[2].count).toBe(2);
  });
});

describe("Integration: Works Aggregations - Genre Labels", () => {
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

    // Index genre test document
    await indexTestDocuments(ctx.client, ctx.worksIndex, ["works.genres"]);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("returns genres.label aggregation with correct buckets", async () => {
    const response = await api.get("/works?aggregations=genres.label");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations["genres.label"]).toBeDefined();

    const buckets = response.body.aggregations["genres.label"].buckets;
    expect(buckets.length).toBeGreaterThan(0);

    // Should have "Electronic books" from works.genres test document
    const electronicBooks = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "Electronic books"
    );
    expect(electronicBooks).toBeDefined();
    expect(electronicBooks.count).toBe(1);
  });
});

// Note: Filtered aggregations tests are in filtered-aggregations.integration.test.ts

describe("Integration: Works Aggregations - Availabilities", () => {
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

    // Index availability test documents (same as Scala WorksAggregationsTest)
    const availabilityDocs = [
      "works.examples.availabilities.open-only",
      "works.examples.availabilities.closed-only",
      "works.examples.availabilities.online-only",
      "works.examples.availabilities.everywhere",
      "works.examples.availabilities.nowhere",
    ];
    await indexTestDocuments(ctx.client, ctx.worksIndex, availabilityDocs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("returns availabilities aggregation with correct buckets", async () => {
    const response = await api.get("/works?aggregations=availabilities");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.availabilities).toBeDefined();

    const buckets = response.body.aggregations.availabilities.buckets;
    // Should have 3 availability types (closed-stores, online, open-shelves)
    // "nowhere" has no availabilities so won't appear
    expect(buckets.length).toBe(3);

    // Each should have count 2 (everywhere doc + specific doc)
    for (const bucket of buckets) {
      expect(bucket.count).toBe(2);
    }
  });
});
