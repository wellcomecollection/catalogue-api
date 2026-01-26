# Integration Tests

These tests run against a real Elasticsearch instance to ensure the TypeScript API behaves identically to the Scala API.

## Quick Start

```bash
# Run integration tests with Docker (recommended)
npm run test:integration:docker

# Or manually:
npm run docker:up                # Start ES
npm run test:integration         # Run tests
npm run docker:down              # Stop ES
```

## Test Files

| File                                        | Scala Equivalent                                                | Purpose                                    |
| ------------------------------------------- | --------------------------------------------------------------- | ------------------------------------------ |
| `aggregations.integration.test.ts`          | `WorksAggregationsTest.scala`                                   | Basic aggregation behavior                 |
| `filtered-aggregations.integration.test.ts` | `WorksFilteredAggregationsTest.scala`, `AggregationsTest.scala` | Complex filtered aggregation scenarios     |
| `works-query.integration.test.ts`           | `WorksQueryTest.scala`                                          | Free-text search, ID lookups, label search |
| `works-filters.integration.test.ts`         | `WorksFiltersTest.scala`                                        | Filter behavior with real ES               |
| `works-sort.integration.test.ts`            | `WorksTest.scala` (sorting tests)                               | Sorting by dates, missing values           |
| `images.integration.test.ts`                | `ImagesAggregationsTest.scala`, `ImagesFiltersTest.scala`       | Image-specific functionality               |

## Test Structure

The integration tests use the **same test documents** as the Scala tests, located in:

```
common/search/src/test/resources/test_documents/
```

This ensures:

1. **Parity testing** - TypeScript and Scala APIs are tested with identical data
2. **Realistic data** - Test documents reflect actual indexed document structure
3. **Shared maintenance** - Changes to test data benefit both implementations

## Test Documents

Test documents are JSON files containing:

- `id` - Document identifier
- `document.display` - The display representation returned by the API
- `document.aggregatableValues` - Values used for aggregation buckets
- `document.filterableValues` - Values used for filtering
- `document.query` - Values used for text search

## Index Configuration

The tests use the same ES index mappings as production:

- `common/search/src/test/resources/WorksIndexConfig.json`
- `common/search/src/test/resources/ImagesIndexConfig.json`

## Unit vs Integration Tests

| Aspect          | Unit Tests (`npm test`)                | Integration Tests (`npm run test:integration`) |
| --------------- | -------------------------------------- | ---------------------------------------------- |
| **Speed**       | Fast (~250ms/test)                     | Slower (~500-1000ms/test)                      |
| **ES Required** | No (mocked)                            | Yes (real ES)                                  |
| **Purpose**     | Parameter validation, response shaping | Query correctness, parity with Scala           |
| **When to Run** | Always, in CI                          | Before releases, after ES-related changes      |

### What belongs in Unit Tests

- Parameter validation (400 errors for invalid inputs)
- Response structure and shaping
- Pagination logic
- Include parameter handling
- Error responses

### What belongs in Integration Tests

- Aggregation bucket counts and ordering
- Filter query behavior
- Full-text search scoring and matching
- Sorting behavior
- Filtered aggregation interactions

## CI Integration

In CI, these tests can be run using:

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.4
    ports:
      - 9200:9200
    env:
      discovery.type: single-node
      xpack.security.enabled: false

steps:
  - run: npm run test:integration
```

## NPM Scripts

```bash
npm test                      # Unit tests only
npm run test:integration      # Integration tests (requires ES)
npm run test:integration:docker  # Start ES, run tests, stop ES
npm run test:all              # All tests (unit + integration)
npm run docker:up             # Start ES
npm run docker:down           # Stop ES
```
