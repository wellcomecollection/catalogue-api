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

Three tests now check this file against the code. Change one without the other and the
build fails.

| Test                                             | Asserts                                                                                                                                                                                 |
| ------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `search/…/openapi/OpenApiSpecEnumTest.scala`     | The closed enums (`include`, `aggregations`, `sort`, `sortOrder`, the access status filter) match the decoders in `search/…/rest/`, and the pagination bounds match `PaginationLimits`. |
| `search/…/openapi/OpenApiSpecEndpointTest.scala` | Every works and images endpoint documented here is routable, and the internal endpoints stay undocumented.                                                                              |
| `concepts/test/openapi.test.ts`                  | The concepts endpoints served match those documented here exactly, and the pagination limits agree with the search API's.                                                               |

The two services support different checks. A Pekko route is an opaque function, so the
search test can only ask whether a documented path exists. A new public route added to
`SearchApi` and never written down would pass. Express can enumerate its own routes, so
the concepts test compares both directions.

Nothing checks the response schemas. If you change them, validate against the fixtures
in `search/src/test/resources/expected_responses/` before you push. Those fixtures are
a biased sample: the schema was missing `Genre`-typed concepts for a while because no
fixture contained one.

## Checking your changes

```
yarn lint:openapi                                   # validate the spec
sbt "search/testOnly weco.api.search.openapi.*"     # enums + endpoints
yarn --cwd concepts test openapi                    # concepts endpoints + pagination
```
