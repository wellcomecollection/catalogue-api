import { Client } from "@elastic/elasticsearch";
import * as fs from "fs";
import * as path from "path";

// Path to shared test documents (from Scala tests)
const TEST_DOCUMENTS_PATH = path.join(
  __dirname,
  "../../../common/search/src/test/resources/test_documents"
);

const WORKS_INDEX_CONFIG_PATH = path.join(
  __dirname,
  "../../../common/search/src/test/resources/WorksIndexConfig.json"
);

const IMAGES_INDEX_CONFIG_PATH = path.join(
  __dirname,
  "../../../common/search/src/test/resources/ImagesIndexConfig.json"
);

// ============================================================================
// Elasticsearch Client for Integration Tests
// ============================================================================

export function createIntegrationTestClient(): Client {
  return new Client({
    node: process.env.ES_URL || "http://localhost:9200",
  });
}

// ============================================================================
// Index Management
// ============================================================================

export async function createWorksIndex(
  client: Client,
  indexName: string
): Promise<void> {
  const indexConfig = JSON.parse(
    fs.readFileSync(WORKS_INDEX_CONFIG_PATH, "utf-8")
  );

  // Check if index exists and delete it
  const exists = await client.indices.exists({ index: indexName });
  if (exists) {
    await client.indices.delete({ index: indexName });
  }

  await client.indices.create({
    index: indexName,
    ...indexConfig,
  });

  // Wait for index to be ready
  await client.cluster.health({
    index: indexName,
    wait_for_status: "yellow",
  });
}

export async function createImagesIndex(
  client: Client,
  indexName: string
): Promise<void> {
  const indexConfig = JSON.parse(
    fs.readFileSync(IMAGES_INDEX_CONFIG_PATH, "utf-8")
  );

  const exists = await client.indices.exists({ index: indexName });
  if (exists) {
    await client.indices.delete({ index: indexName });
  }

  await client.indices.create({
    index: indexName,
    ...indexConfig,
  });

  await client.cluster.health({
    index: indexName,
    wait_for_status: "yellow",
  });
}

export async function deleteIndex(
  client: Client,
  indexName: string
): Promise<void> {
  try {
    await client.indices.delete({ index: indexName });
  } catch {
    // Ignore if index doesn't exist
  }
}

// ============================================================================
// Test Document Loading
// ============================================================================

type TestDocument = {
  id: string;
  document: Record<string, unknown>;
};

export function loadTestDocument(documentId: string): TestDocument {
  const filePath = path.join(TEST_DOCUMENTS_PATH, `${documentId}.json`);
  const content = JSON.parse(fs.readFileSync(filePath, "utf-8"));
  return {
    id: content.id,
    document: content.document,
  };
}

export function listTestDocuments(): string[] {
  return fs
    .readdirSync(TEST_DOCUMENTS_PATH)
    .filter((f) => f.endsWith(".json"))
    .map((f) => f.replace(".json", ""));
}

// ============================================================================
// Document Indexing
// ============================================================================

export async function indexTestDocuments(
  client: Client,
  indexName: string,
  documentIds: string[]
): Promise<void> {
  const operations = documentIds.flatMap((docId) => {
    const { id, document } = loadTestDocument(docId);
    return [{ index: { _index: indexName, _id: id } }, document];
  });

  if (operations.length > 0) {
    await client.bulk({ refresh: true, operations });
  }
}

// ============================================================================
// Helper to get display document from test document
// ============================================================================

export function getDisplayFromTestDocument(
  documentId: string
): Record<string, unknown> {
  const { document } = loadTestDocument(documentId);
  return document.display as Record<string, unknown>;
}

// ============================================================================
// Integration Test Fixture
// ============================================================================

export type IntegrationTestContext = {
  client: Client;
  worksIndex: string;
  imagesIndex: string;
};

let testCounter = 0;

export async function setupIntegrationTest(): Promise<IntegrationTestContext> {
  const client = createIntegrationTestClient();
  testCounter++;
  const suffix = `${Date.now()}-${testCounter}`;
  const worksIndex = `test-works-${suffix}`;
  const imagesIndex = `test-images-${suffix}`;

  await createWorksIndex(client, worksIndex);
  await createImagesIndex(client, imagesIndex);

  return { client, worksIndex, imagesIndex };
}

export async function teardownIntegrationTest(
  ctx: IntegrationTestContext
): Promise<void> {
  await deleteIndex(ctx.client, ctx.worksIndex);
  await deleteIndex(ctx.client, ctx.imagesIndex);
  await ctx.client.close();
}
