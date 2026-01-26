# Search API v2

A TypeScript/Express rewrite of the Wellcome Collection catalogue search API.

## Prerequisites

- Node.js 22+ (use `fnm use` or `nvm use` to automatically select the correct version)
- Yarn
- Access to Elasticsearch cluster (via AWS Secrets Manager)

## Getting Started

### Install dependencies

```bash
yarn install
```

### Run locally (development mode)

```bash
yarn dev
```

This starts the server on `http://localhost:3002` with:

- AWS profile: `platform-developer`
- Hot reloading via nodemon
- Development logging

### Run in production mode

```bash
yarn start
```

### Run tests

```bash
yarn test
```

## Configuration

The API is configured via environment variables:

| Variable      | Description                                      | Default |
| ------------- | ------------------------------------------------ | ------- |
| `PORT`        | Server port                                      | `3001`  |
| `NODE_ENV`    | Environment (`development` or `production`)      | -       |
| `AWS_PROFILE` | AWS profile for Secrets Manager access           | -       |
| `ES_HOST`     | Elasticsearch host (bypasses Secrets Manager)    | -       |
| `ES_APIKEY`   | Elasticsearch API key (bypasses Secrets Manager) | -       |

In production, Elasticsearch credentials are fetched from AWS Secrets Manager using the secret `elasticsearch/catalogue_api/credentials`.

## API Endpoints

### Works

#### `GET /works`

Search and list works from the catalogue.

**Query Parameters:**

| Parameter      | Type    | Description                                                   |
| -------------- | ------- | ------------------------------------------------------------- |
| `page`         | integer | Page number (min: 1)                                          |
| `pageSize`     | integer | Results per page (1-100)                                      |
| `query`        | string  | Full-text search query                                        |
| `sort`         | string  | Sort field: `production.dates`, `items.locations.createdDate` |
| `sortOrder`    | string  | Sort order: `asc` or `desc`                                   |
| `include`      | string  | Comma-separated fields to include (see below)                 |
| `aggregations` | string  | Comma-separated aggregations to return (see below)            |

**Include options:** `identifiers`, `items`, `holdings`, `subjects`, `genres`, `contributors`, `production`, `languages`, `notes`, `formerFrequency`, `designation`, `images`, `parts`, `partOf`, `precededBy`, `succeededBy`

**Aggregation options:** `workType`, `genres.label`, `genres`, `production.dates`, `subjects.label`, `subjects`, `languages`, `contributors.agent.label`, `contributors.agent`, `items.locations.license`, `availabilities`

**Filter Parameters:**

| Parameter                                 | Description                                          |
| ----------------------------------------- | ---------------------------------------------------- |
| `workType`                                | Filter by work format IDs (comma-separated)          |
| `type`                                    | Filter by work type                                  |
| `production.dates.from`                   | Filter by production date (YYYY-MM-DD)               |
| `production.dates.to`                     | Filter by production date (YYYY-MM-DD)               |
| `languages`                               | Filter by language IDs                               |
| `genres.label`                            | Filter by genre labels                               |
| `genres`                                  | Filter by genre concept IDs                          |
| `subjects.label`                          | Filter by subject labels                             |
| `subjects`                                | Filter by subject concept IDs                        |
| `contributors.agent.label`                | Filter by contributor labels                         |
| `contributors.agent`                      | Filter by contributor concept IDs                    |
| `identifiers`                             | Filter by identifier values                          |
| `items`                                   | Filter by item IDs                                   |
| `items.identifiers`                       | Filter by item identifier values                     |
| `items.locations.license`                 | Filter by license IDs                                |
| `items.locations.locationType`            | Filter by location type IDs                          |
| `items.locations.accessConditions.status` | Filter by access status (prefix with `!` to exclude) |
| `partOf`                                  | Filter by parent work ID                             |
| `partOf.title`                            | Filter by parent work title                          |
| `availabilities`                          | Filter by availability IDs                           |

**Example:**

```bash
curl "http://localhost:3002/works?query=darwin&pageSize=10&include=subjects,contributors"
```

#### `GET /works/:id`

Get a single work by ID.

**Query Parameters:**

| Parameter | Type   | Description                       |
| --------- | ------ | --------------------------------- |
| `include` | string | Comma-separated fields to include |

**Example:**

```bash
curl "http://localhost:3002/works/abc123?include=items,subjects"
```

---

### Images

#### `GET /images`

Search and list images from the catalogue.

**Query Parameters:**

| Parameter      | Type    | Description                                        |
| -------------- | ------- | -------------------------------------------------- |
| `page`         | integer | Page number (min: 1)                               |
| `pageSize`     | integer | Results per page (1-100)                           |
| `query`        | string  | Full-text search query                             |
| `color`        | string  | Hex color for similarity search (e.g., `ff0000`)   |
| `sort`         | string  | Sort field: `source.production.dates`              |
| `sortOrder`    | string  | Sort order: `asc` or `desc`                        |
| `include`      | string  | Comma-separated fields to include (see below)      |
| `aggregations` | string  | Comma-separated aggregations to return (see below) |

**Include options:** `withSimilarFeatures`, `source.contributors`, `source.languages`, `source.genres`, `source.subjects`

**Aggregation options:** `locations.license`, `source.contributors.agent.label`, `source.contributors.agent`, `source.genres.label`, `source.genres`, `source.subjects.label`, `source.subjects`

**Filter Parameters:**

| Parameter                         | Description                            |
| --------------------------------- | -------------------------------------- |
| `locations.license`               | Filter by license IDs                  |
| `source.contributors.agent.label` | Filter by contributor labels           |
| `source.contributors.agent`       | Filter by contributor concept IDs      |
| `source.genres.label`             | Filter by genre labels                 |
| `source.genres`                   | Filter by genre concept IDs            |
| `source.subjects.label`           | Filter by subject labels               |
| `source.subjects`                 | Filter by subject concept IDs          |
| `source.production.dates.from`    | Filter by production date (YYYY-MM-DD) |
| `source.production.dates.to`      | Filter by production date (YYYY-MM-DD) |

**Example:**

```bash
curl "http://localhost:3002/images?query=portrait&color=8B4513"
```

#### `GET /images/:id`

Get a single image by ID.

**Query Parameters:**

| Parameter | Type   | Description                       |
| --------- | ------ | --------------------------------- |
| `include` | string | Comma-separated fields to include |

**Example:**

```bash
curl "http://localhost:3002/images/xyz789?include=source.contributors"
```

---

### Management & Diagnostics

#### `GET /management/healthcheck`

Returns API health status.

```json
{
  "status": "healthy",
  "pipelineDate": "2022-02-22",
  "message": "ok"
}
```

#### `GET /management/clusterhealth`

Returns Elasticsearch cluster health status.

#### `GET /management/_workTypes`

Returns a tally of work types in the index.

#### `GET /_searchTemplates`

Returns information about the search query templates in use.

#### `GET /_elasticConfig`

Returns the current Elasticsearch configuration (index names, pipeline date).

---

## Response Format

### Success (list)

```json
{
  "type": "ResultList",
  "pageSize": 10,
  "totalPages": 5,
  "totalResults": 42,
  "results": [...],
  "prevPage": "http://api.wellcomecollection.org/catalogue/v2/works?page=1",
  "nextPage": "http://api.wellcomecollection.org/catalogue/v2/works?page=3",
  "aggregations": {...}
}
```

### Success (single item)

Returns the work or image object directly with a `type` field (`"Work"` or `"Image"`).

### Error

```json
{
  "type": "Error",
  "httpStatus": 400,
  "label": "Bad Request",
  "description": "page: Number must be greater than or equal to 1"
}
```

## Architecture

```
search_v2/
├── config.ts              # Application configuration
├── server.ts              # Entry point
├── src/
│   ├── app.ts             # Express app setup
│   ├── types.ts           # TypeScript type definitions
│   ├── controllers/       # Route handlers
│   ├── services/          # Business logic
│   │   ├── elasticsearch.ts      # ES client factory
│   │   ├── query-params.ts       # Zod validation schemas
│   │   ├── filter-builder.ts     # ES filter construction
│   │   ├── request-builder.ts    # ES request building
│   │   ├── aggregations-builder.ts # RFC 37 aggregations
│   │   └── aggregation-parser.ts # Parse ES aggregation responses
│   └── resources/         # Query templates (JSON)
└── test/                  # Jest tests
```

## Docker

Build and run with Docker:

```bash
# From repo root
docker build -f search_v2/Dockerfile -t search-api-v2 .
docker run -p 3001:3001 search-api-v2
```
