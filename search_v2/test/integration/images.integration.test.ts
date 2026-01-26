/**
 * Integration tests for images functionality using real Elasticsearch.
 *
 * Based on Scala ImagesAggregationsTest.scala and ImagesFiltersTest.scala.
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

describe("Integration: Images Aggregations - License", () => {
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
      (_, i) => `images.different-licenses.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates by license", async () => {
    const response = await api.get("/images?aggregations=locations.license");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations.license).toBeDefined();

    const buckets = response.body.aggregations.license.buckets;
    expect(buckets.length).toBe(2);

    // cc-by has 5 images, pdm has 2
    const ccByBucket = buckets.find(
      (b: { data: { id: string } }) => b.data.id === "cc-by"
    );
    expect(ccByBucket).toBeDefined();
    expect(ccByBucket.count).toBe(5);

    const pdmBucket = buckets.find(
      (b: { data: { id: string } }) => b.data.id === "pdm"
    );
    expect(pdmBucket).toBeDefined();
    expect(pdmBucket.count).toBe(2);
  });
});

describe("Integration: Images Aggregations - Contributors", () => {
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
      { length: 3 },
      (_, i) => `images.contributors.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates by source contributors agent labels", async () => {
    const response = await api.get(
      "/images?aggregations=source.contributors.agent.label"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(
      response.body.aggregations["source.contributors.agent.label"]
    ).toBeDefined();

    const buckets =
      response.body.aggregations["source.contributors.agent.label"].buckets;
    expect(buckets.length).toBe(2);

    // carrots has 3 images, parrots has 2
    const carrotsBucket = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "carrots"
    );
    expect(carrotsBucket).toBeDefined();
    expect(carrotsBucket.count).toBe(3);

    const parrotsBucket = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "parrots"
    );
    expect(parrotsBucket).toBeDefined();
    expect(parrotsBucket.count).toBe(2);
  });
});

describe("Integration: Images Aggregations - Genres", () => {
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

    const docs = Array.from({ length: 3 }, (_, i) => `images.genres.${i}`);
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates by source genres label", async () => {
    const response = await api.get("/images?aggregations=source.genres.label");

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations["source.genres.label"]).toBeDefined();

    const buckets = response.body.aggregations["source.genres.label"].buckets;
    expect(buckets.length).toBe(3);

    // Carrot counselling has 2, Emu entrepreneurship has 1, Falcon finances has 1
    const carrotBucket = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "Carrot counselling"
    );
    expect(carrotBucket).toBeDefined();
    expect(carrotBucket.count).toBe(2);
  });
});

describe("Integration: Images Aggregations - Subjects", () => {
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
      "images.subjects.screwdrivers-1",
      "images.subjects.screwdrivers-2",
      "images.subjects.sounds",
      "images.subjects.squirrel,screwdriver",
      "images.subjects.squirrel,sample",
    ];
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("aggregates by subject", async () => {
    const response = await api.get(
      "/images?aggregations=source.subjects.label"
    );

    expect(response.status).toBe(200);
    expect(response.body.aggregations).toBeDefined();
    expect(response.body.aggregations["source.subjects.label"]).toBeDefined();

    const buckets = response.body.aggregations["source.subjects.label"].buckets;
    expect(buckets.length).toBe(4);

    // Simple screwdrivers has 3
    const screwdriverBucket = buckets.find(
      (b: { data: { label: string } }) => b.data.label === "Simple screwdrivers"
    );
    expect(screwdriverBucket).toBeDefined();
    expect(screwdriverBucket.count).toBe(3);
  });
});

describe("Integration: Images Filters - License", () => {
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
      (_, i) => `images.different-licenses.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by license", async () => {
    const response = await api.get("/images?locations.license=cc-by");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(5);
  });
});

describe("Integration: Images Filters - Contributors", () => {
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
      { length: 3 },
      (_, i) => `images.examples.contributor-filter-tests.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by contributors from the canonical source work", async () => {
    const response = await api.get(
      '/images?source.contributors.agent.label="Machiavelli,%20Niccolo"'
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });

  it("filters by multiple contributors", async () => {
    const response = await api.get(
      '/images?source.contributors.agent.label="Machiavelli,%20Niccolo",Edward%20Said'
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });
});

describe("Integration: Images Filters - Genres", () => {
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
      { length: 3 },
      (_, i) => `images.examples.genre-filter-tests.${i}`
    );
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by genres from the canonical source work", async () => {
    const response = await api.get(
      "/images?source.genres.label=Carrot%20counselling"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });

  it("does not filter by genres from the redirected source work", async () => {
    const response = await api.get(
      "/images?source.genres.label=Dodo%20divination"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(0);
  });

  it("filters by multiple genres", async () => {
    const response = await api.get(
      "/images?source.genres.label=Carrot%20counselling,Emu%20entrepreneurship"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("does not apply the filter if there are no values provided", async () => {
    const response = await api.get("/images?source.genres=");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);
  });

  it("filters by one concept id", async () => {
    const response = await api.get("/images?source.genres=baadf00d");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(1);
  });

  it("filters containing multiple concept ids return documents containing ANY", async () => {
    const response = await api.get("/images?source.genres=g00dcafe,baadf00d");

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });
});

describe("Integration: Images Filters - Subjects", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const images = [
    "images.subjects.screwdrivers-1",
    "images.subjects.screwdrivers-2",
    "images.subjects.sounds",
    "images.subjects.squirrel,sample",
    "images.subjects.squirrel,screwdriver",
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

    await indexTestDocuments(ctx.client, ctx.imagesIndex, images);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by subjects", async () => {
    const response = await api.get(
      "/images?source.subjects.label=Simple%20screwdrivers"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);
  });

  it("filters by multiple subjects", async () => {
    const response = await api.get(
      "/images?source.subjects.label=Square%20sounds,Struck%20samples"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });
});

describe("Integration: Images Filters - Date Range", () => {
  let ctx: IntegrationTestContext;
  let api: supertest.Agent;

  const productionImages = [
    "image-production.1098",
    "image-production.1900",
    "image-production.1904",
    "image-production.1976",
    "image-production.2020",
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

    await indexTestDocuments(ctx.client, ctx.imagesIndex, productionImages);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("filters by date range", async () => {
    const response = await api.get(
      "/images?source.production.dates.from=1900-01-01&source.production.dates.to=1960-01-01"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(2);
  });

  it("filters by from date", async () => {
    const response = await api.get(
      "/images?source.production.dates.from=1900-01-01"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(4);
  });

  it("filters by to date", async () => {
    const response = await api.get(
      "/images?source.production.dates.to=1960-01-01"
    );

    expect(response.status).toBe(200);
    expect(response.body.totalResults).toBe(3);
  });
});

describe("Integration: Images Filters - Color", () => {
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
      "images.examples.color-filter-tests.red",
      "images.examples.color-filter-tests.even-less-red",
      "images.examples.color-filter-tests.slightly-less-red",
      "images.examples.color-filter-tests.blue",
    ];
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("scores by nearest neighbour", async () => {
    const response = await api.get("/images?color=e02020");

    expect(response.status).toBe(200);
    expect(response.body.results.length).toBe(4);

    // Results should be ordered by color similarity to red (e02020)
    // red -> slightly-less-red -> even-less-red -> blue
    const ids = response.body.results.map((r: { id: string }) => r.id);
    expect(ids[0]).toBe("q7qgmzry"); // red
    expect(ids[1]).toBe("vj25vq95"); // slightly-less-red
    expect(ids[2]).toBe("vxdgpgja"); // even-less-red
    expect(ids[3]).toBe("nf44hzdb"); // blue
  });
});

describe("Integration: Images Filters - Color and Query", () => {
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
      "images.examples.color-filter-tests.even-less-blue-foot",
      "images.examples.color-filter-tests.blue-foot",
      "images.examples.color-filter-tests.blue",
      "images.examples.color-filter-tests.orange-foot",
      "images.examples.color-filter-tests.slightly-less-blue-foot",
    ];
    await indexTestDocuments(ctx.client, ctx.imagesIndex, docs);
  }, 30000);

  afterAll(async () => {
    await teardownIntegrationTest(ctx);
  }, 10000);

  it("combines query and colour filter", async () => {
    const response = await api.get("/images?query=foot&color=22bbff");

    expect(response.status).toBe(200);
    // Should return only "foot" images, sorted by blue similarity
    expect(response.body.results.length).toBe(4);

    const ids = response.body.results.map((r: { id: string }) => r.id);
    // blue-foot -> slightly-less-blue-foot -> even-less-blue-foot -> orange-foot
    expect(ids[0]).toBe("wdbmb54f"); // blue-foot
    expect(ids[1]).toBe("swvctkcs"); // slightly-less-blue-foot
    expect(ids[2]).toBe("xnb8kwmw"); // even-less-blue-foot
    expect(ids[3]).toBe("cppz76j6"); // orange-foot
  });

  it("combines multi-token query terms with AND", async () => {
    const response = await api.get("/images?query=green+dye&color=22bbff");

    expect(response.status).toBe(200);
    // Should only return images that have both "green" AND "dye"
    expect(response.body.results.length).toBe(1);
    expect(response.body.results[0].id).toBe("nf44hzdb"); // blue has Green + Dye subjects
  });
});
