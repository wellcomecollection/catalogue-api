# API reference

`catalogue.yaml` is the OpenAPI 3.1 description of the public catalogue API. It is the
source of truth for the reference docs on
[developers.wellcomecollection.org](https://developers.wellcomecollection.org).

When it changes on `main`, `.github/workflows/sync-openapi-spec.yml` opens a pull
request against
[developers.wellcomecollection.org](https://github.com/wellcomecollection/developers.wellcomecollection.org)
to update its copy at `reference/catalogue.yaml`. Don't edit that copy by hand; it will
be overwritten by the next sync.

## What it covers

One file, two services, because both are served under
`https://api.wellcomecollection.org/catalogue/v2`:

| Paths                                              | Served by                |
| -------------------------------------------------- | ------------------------ |
| `/works`, `/works/{id}`, `/images`, `/images/{id}` | `search/` (Scala)        |
| `/concepts`, `/concepts/{id}`                      | `concepts/` (TypeScript) |

Some endpoints are left undocumented on purpose: `/management/*`,
`/search-templates.json`, `/_elasticConfig`, and the `elasticCluster` query parameter.
They are internal.

## How the spec stays in sync with the code

Both services return a pre-rendered `display` document straight from Elasticsearch, so
the response schemas here are written by hand and cannot be generated from this repo.
The request parameters are defined in code, and those are what drifted in the past. For
several years the spec documented an image colour filter named `colors`, which the API
ignored.

Four tests now check this file against the code. Change one without the other and the
build fails.

| Test                                             | Asserts                                                                                                                                                                                 |
| ------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `search/…/openapi/OpenApiSpecEnumTest.scala`     | The closed enums (`include`, `aggregations`, `sort`, `sortOrder`, the access status filter) match the decoders in `search/…/rest/`, and the pagination bounds match `PaginationLimits`. |
| `search/…/openapi/OpenApiSpecEndpointTest.scala` | Every works and images endpoint documented here is routable, and the internal endpoints stay undocumented.                                                                              |
| `search/…/openapi/OpenApiSpecResponseTest.scala` | The `Work` and `Image` schemas accept every display document in `test_documents/`, and document every field those documents contain.                                                    |
| `concepts/test/openapi.test.ts`                  | The concepts endpoints served match those documented here exactly, and the pagination limits agree with the search API's.                                                               |

The two services support different checks. A Pekko route is an opaque function, so the
search test can only ask whether a documented path exists. A new public route added to
`SearchApi` and never written down would pass. Express can enumerate its own routes, so
the concepts test compares both directions.

### The response schemas

The pipeline generates the documents in
`common/search/src/test/resources/test_documents/`, and `copy_test_documents.py` copies
them here from a local clone of the pipeline repo. Each one's `document.display` object
is exactly what the API returns for that record, so `OpenApiSpecResponseTest` validates
all of them against the `Work` and `Image` schemas.

Two limits to that. The fixtures are a sample, not every field the pipeline can emit,
so the test proves the schemas accept what we have rather than everything that exists.
The schemas were missing `Genre`-typed concepts for a while because no fixture contained
one. And because `copy_test_documents.py` runs by hand, a change to the pipeline's
display model is only caught once someone refreshes the fixtures.

## Checking your changes

```
yarn lint:openapi                                   # validate the spec
sbt "search/testOnly weco.api.search.openapi.*"     # enums, endpoints, responses
yarn --cwd concepts test openapi                    # concepts endpoints + pagination
```
