# API reference

`catalogue.yaml` is the OpenAPI 3.1 description of the public catalogue API, and it is
the source of truth for the reference docs on
[developers.wellcomecollection.org](https://developers.wellcomecollection.org).

When it changes on `main`, `.github/workflows/sync-openapi-spec.yml` opens a pull
request against
[developers.wellcomecollection.org](https://github.com/wellcomecollection/developers.wellcomecollection.org)
to update its copy at `reference/catalogue.yaml`. Don't edit that copy by hand — it will
be overwritten by the next sync.

## What it covers

One file, two services, because both are served under
`https://api.wellcomecollection.org/catalogue/v2`:

| Paths                                              | Served by                |
| -------------------------------------------------- | ------------------------ |
| `/works`, `/works/{id}`, `/images`, `/images/{id}` | `search/` (Scala)        |
| `/concepts`, `/concepts/{id}`                      | `concepts/` (TypeScript) |

Internal endpoints — `/management/*`, `/search-templates.json`, `/_elasticConfig`, and
the `elasticCluster` query parameter — are deliberately left undocumented.

## Keeping it honest

Both services return a pre-rendered `display` document straight from Elasticsearch, so
the response schemas here are hand-written and cannot be generated from this repo. The
request parameters, though, are defined in code, and that is what drifted in the past:
the spec once documented an image colour filter named `colors`, which the API silently
ignored.

Three contract tests tie this file to the code. Change one without the other and the
build fails.

| Test                                             | Asserts                                                                                                                                                                                           |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `search/…/openapi/OpenApiSpecEnumTest.scala`     | Every closed enum here — `include`, `aggregations`, `sort`, `sortOrder`, the access status filter — matches the decoders in `search/…/rest/`, and the pagination bounds match `PaginationLimits`. |
| `search/…/openapi/OpenApiSpecEndpointTest.scala` | Every works/images endpoint documented here is actually routable, and the internal endpoints stay undocumented.                                                                                   |
| `concepts/test/openapi.test.ts`                  | The concepts endpoints served match those documented here **exactly**, and the pagination limits agree with the search API's.                                                                     |

The asymmetry is worth knowing. A Pekko route is an opaque function, so the search test
can only ask "does this documented path exist?" — a brand new public route added to
`SearchApi` and never written down would slip past it. Express can enumerate its own
routes, so the concepts test checks both directions.

Nothing enforces the response schemas. If you change them, validate against the
fixtures in `search/src/test/resources/expected_responses/` before you push. Be aware
those fixtures are a biased sample: `Genre`-typed concepts were missing from the schema
for a while because no fixture happened to contain one.

## Checking your changes

```
yarn lint:openapi                                   # validate the spec
sbt "search/testOnly weco.api.search.openapi.*"     # enums + endpoints
yarn --cwd concepts test openapi                    # concepts endpoints + pagination
```
