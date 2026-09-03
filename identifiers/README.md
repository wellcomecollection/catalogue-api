# Identifiers API

A read-only lookup over the catalogue **ID Registry** (the RFC 083 store the ID
Minter writes to): it resolves a **canonical** identifier to its **source**
identifier(s) and back. The mapping is **one-to-many** — a single canonical id
can carry several source identifiers (an original plus "predecessor" aliases
inherited when records migrate between source systems). It never mints and never
writes.

**It is not deployed yet.** There is no Lambda, no gateway, no API keys and no
cache: that work is tracked on
[platform#6403](https://github.com/wellcomecollection/platform/issues/6403), and
the first callable URL arrives with
[platform#6531](https://github.com/wellcomecollection/platform/issues/6531).
Until then, run it locally as below.

It also stands as the proposed **"service" answer** to the identifier-translation
open question in the `folio-api` requesting prototype (see
[Requesting integration](#requesting-integration)).

The authoritative contract is [`spec/openapi.yaml`](spec/openapi.yaml).

**See also**: [RFC 089](https://github.com/wellcomecollection/docs/tree/main/rfcs/089-identifiers-api),
which carries the problem and consumers
([Context](https://github.com/wellcomecollection/docs/tree/main/rfcs/089-identifiers-api#context)),
the AWS shape and data model
([Proposed architecture](https://github.com/wellcomecollection/docs/tree/main/rfcs/089-identifiers-api#proposed-architecture)),
API auth
([Authentication and cost](https://github.com/wellcomecollection/docs/tree/main/rfcs/089-identifiers-api#authentication-and-cost))
and the caching question
([Caching](https://github.com/wellcomecollection/docs/tree/main/rfcs/089-identifiers-api#caching)).

## Quick start

```bash
cd identifiers

# Run the test suite (contract + highlights + validation + ETag/304)
uv run pytest

# Start the API against the seeded in-memory DB (defaults to port 8000)
uv run python src/adapters/run_local.py
# PORT=8731 uv run python src/adapters/run_local.py   # to pick a port
```

The local server is a thin stdlib invoker: it turns each HTTP request into an
API-Gateway Lambda-proxy event, calls the same `handler` a Lambda would, and
writes the proxy response back as HTTP. No web framework, so local runs stay
faithful to the production shape (Python Lambda behind API Gateway).

## Architecture (the seam that matters)

```
src/
  core/         framework- AND storage-agnostic
    validation.py   canonicalId regex + type/include enums
    models.py       SourceRow, SourceIdentifier, IdentifierSet, CanonicalIdRef
    repository.py   Repository Protocol (get_by_canonical, get_by_source)
    service.py      ordering, isAlias, ETag, cache policy, 404 rules
  adapters/
    sqlite_repo.py    Repository impl over the seeded SQLite DB (default)
    rds_data_repo.py  Repository impl over Aurora via the RDS Data API (read-only)
    handler.py        Lambda proxy handler: HTTP <-> core; picks the backend
    run_local.py      stdlib HTTP invoker for the live demo
    db/
      schema.sql      portable rendering of the RFC 083 two-table model
      seed.sql        fixtures exercising every interesting case
```

The SQL sits next to the one module that reads it, so copying `src/` into the
image carries it along.

`core` has **no** FastAPI and **no** SQLite imports. The production target is a
Python Lambda reading Aurora via the **RDS Data API**; that swap is just a second
`Repository` implementation — nothing in `core` changes. Both backends ship here
(see [AWS backend](#aws-backend-rds-data-api)).

## AWS backend (RDS Data API)

By default the API runs against the seeded SQLite store. Set
`IDENTIFIERS_BACKEND=rds` to read the **real Aurora ID Registry**
(`identifiers-v2-serverless-test`) over the RDS Data API instead —
`adapters/rds_data_repo.py`, selected by `adapters/handler.py`.

> **READ-ONLY. NEVER WRITE OR SEED THIS DATABASE.** This API is a read-only
> projection; all writes belong to the ID Minter (RFC 083). `RdsDataRepository`
> issues **only `SELECT`** statements and refuses anything else via an
> `_assert_read_only` guard (covered by tests). Do not add write methods.

**Easiest:** copy the env template, then run the wrapper script:

```bash
cp .env.rds.local.example .env.rds.local   # ARNs are pre-filled; adjust if needed
./run-local-rds.sh                          # PORT=8731 ./run-local-rds.sh to pick a port
```

`.env.rds.local` is gitignored (the repo's `.env*.local` convention);
`run-local-rds.sh` sources it, forces `IDENTIFIERS_BACKEND=rds`, and starts the
local invoker via `uv run --group rds`.

Equivalent manual form:

```bash
export AWS_PROFILE=platform-developer            # IAM-gated; dev account 760097843905
export AWS_REGION=eu-west-1
export IDENTIFIERS_BACKEND=rds
export RDS_RESOURCE_ARN="arn:aws:rds:eu-west-1:760097843905:cluster:identifiers-v2-serverless-test"
export RDS_SECRET_ARN="<cluster managed master-user secret ARN>"   # Secrets Manager, user 'wellcome'
export RDS_DATABASE=identifiers

uv run --group rds python src/adapters/run_local.py
```

The ARNs are infrastructure identifiers, not secrets; access is gated by IAM
(the secret value is fetched by the Data API service, never by this code).

### Real-data findings

Discovered by read-only inspection of `identifiers-v2-serverless-test`:

- **Column case.** Table names are snake_case (`canonical_ids`, `identifiers`)
  but the **columns are PascalCase** per RFC 083 (`CanonicalId`, `OntologyType`,
  `SourceSystem`, `SourceId`, `CreatedAt`, `Status`). The Data API repo uses an
  explicit PascalCase column list; the portable SQLite DDL uses snake_case.
- **Timestamp shape.** `CreatedAt` returns as MySQL `"YYYY-MM-DD HH:MM:SS"`
  (tz-naive); the repo normalises it to the contract's ISO-8601 `…TZ` so
  ordering / `isAlias` / `ETag` match the SQLite store.
- **Ontology types are broader than the contract enum.** The live registry holds
  types beyond `Work`/`Image`/`Item` (e.g. `Concept`). The reverse lookup rejects
  non-enum `type` with `400`, but a forward lookup on a canonical id whose rows
  include such a type would emit a `type` value outside the `SourceIdentifier`
  enum. Tracked as
  [platform#6537](https://github.com/wellcomecollection/platform/issues/6537).
- **Indexed lookups only.** The table is large — a full `COUNT(*)` times out on
  the serverless cluster. The repo issues only point/`idx_canonical` reads
  (matching the contract's two operations), which return promptly.

### Verified live (read-only)

```text
GET /v1/identifiers/r5ky3c4e
  → 200  ETag: W/"1-2026-05-11T15:35:48Z"
    {"canonicalId":"r5ky3c4e","type":"Work","sourceIdentifiers":[
      {"type":"Work","sourceSystem":"axiell-guid",
       "value":"0002acb1-5945-4ffa-9b7f-2e5f226636e9","isAlias":false,
       "createdAt":"2026-05-11T15:35:48Z"}]}

GET /v1/identifiers/by-source/axiell-guid/0002acb1-5945-4ffa-9b7f-2e5f226636e9?type=Work
  → 200  {"canonicalId":"r5ky3c4e"}        # reverse round-trips to the same id
```

## Endpoints

| Endpoint | Returns |
|----------|---------|
| `GET /v1/identifiers/{canonicalId}` | Full `IdentifierSet` (always — no aliases toggle). |
| `GET /v1/identifiers/by-source/{sourceSystem}/{value}?type=Work` | Bare `{ "canonicalId": "..." }`. |
| `…?type=Work&include=siblings` | The same full `IdentifierSet`. |

`type` is `Work` \| `Image` \| `Item`, defaults to `Work`, and is a real key
component. The set is ordered by `createdAt` descending, most recent first.
`isAlias` is `false` for the earliest-`createdAt` row (the original, so last in
the set) and `true` for later (inherited) rows. An `IdentifierSet` also carries a top-level `type`,
copied from the original (`isAlias=false`) row, so a caller can read the
canonical id's type without scanning the set. Element shape (`SourceIdentifier`)
is identical across both endpoints; the queried tuple is always included in any
returned set.

## Transcripts

Captured against the running prototype (`src/adapters/db/seed.sql`).

### Forward lookup — full set, ordered, with `isAlias`

```http
GET /v1/identifiers/a2345bcd
```
```http
HTTP/1.0 200 OK
Cache-Control: public, max-age=300
ETag: W/"2-2026-02-10T12:00:00Z"
Content-Type: application/json

{"canonicalId": "a2345bcd", "type": "Work", "sourceIdentifiers": [
  {"type": "Work", "sourceSystem": "axiell-collections-id", "value": "12345",     "isAlias": true,  "createdAt": "2026-02-10T12:00:00Z"},
  {"type": "Work", "sourceSystem": "sierra-system-number",  "value": "b1161044x", "isAlias": false, "createdAt": "2019-03-04T10:14:22Z"}
]}
```

### Reverse lookup — bare (immutable, long TTL, no ETag)

```http
GET /v1/identifiers/by-source/sierra-system-number/b1161044x?type=Work
```
```http
HTTP/1.0 200 OK
Cache-Control: public, max-age=86400
Content-Type: application/json

{"canonicalId": "a2345bcd"}
```

### Reverse lookup — `include=siblings` (same set as forward)

```http
GET /v1/identifiers/by-source/sierra-system-number/b1161044x?type=Work&include=siblings
```
```json
{"canonicalId": "a2345bcd", "type": "Work", "sourceIdentifiers": [
  {"type": "Work", "sourceSystem": "axiell-collections-id", "value": "12345",     "isAlias": true,  "createdAt": "2026-02-10T12:00:00Z"},
  {"type": "Work", "sourceSystem": "sierra-system-number",  "value": "b1161044x", "isAlias": false, "createdAt": "2019-03-04T10:14:22Z"}
]}
```

### Reverse lookup — the requesting case (FOLIO item UUID → canonical item id)

```http
GET /v1/identifiers/by-source/folio-item/3f2a...uuid?type=Item
```
```json
{"canonicalId": "ka345678"}
```

### Conditional GET — `If-None-Match` → `304`

```http
GET /v1/identifiers/a2345bcd
If-None-Match: W/"2-2026-02-10T12:00:00Z"
```
```http
HTTP/1.0 304 Not Modified
Cache-Control: public, max-age=300
ETag: W/"2-2026-02-10T12:00:00Z"
```

### Not found / bad request

```http
GET /v1/identifiers/abcdefgh        →  404  {"error": "notFound",   "message": "no mapping found"}
GET /v1/identifiers/zzz             →  400  {"error": "badRequest", "message": "canonicalId does not match the required format"}
```

A canonical id that the registry has pre-generated but not yet assigned has no
`identifiers` rows either, so it collapses into the same `404` as an unknown id.

## Status codes

- `200` — mapping found.
- `304` — conditional GET, unchanged since the supplied `ETag`.
- `400` — malformed `canonicalId` (regex `^[a-hjkmnp-z][a-hjkmnp-z2-9]{7}$`) or
  an unsupported `type`/`include` enum value. `sourceSystem`/`value` are **not**
  pattern-validated (formats are heterogeneous) — unknowns fall to `404`.
- `404` — no mapping (unknown id, unknown tuple, or unassigned canonical id).

## Caching / ETag

`ETag` is a weak validator derived from `(row_count, max(createdAt))`, so it
changes exactly when an alias is added — revalidation is a cheap `304` until the
set actually grows. The bare reverse lookup is immutable once minted, so it is
cached hard (long TTL, no ETag); forward and `include=siblings` carry the mutable
set (bounded TTL + ETag).

## Prototype defaults (chosen to run; **not** contract decisions)

The design doc leaves these open; the prototype picks a value and flags it so
they aren't mistaken for committed contract:

- **TTLs** — `max-age=300` for forward / `include=siblings`; `max-age=86400` for
  the immutable bare reverse lookup. (`core/service.py`)
- **Weak `ETag`** form `W/"{row_count}-{max(createdAt)}"`.
- **`createdAt` tie-break** — order by `createdAt` then `sourceSystem`, both
  descending; the earliest row is the original (`isAlias=false`).
- **DB** — a fresh in-memory SQLite seeded from `adapters/db/` on each run, for
  reproducibility.
- **`If-None-Match`** — exact-token match (the client echoes the issued ETag); a
  production gateway applies full weak-comparison semantics.

## Notes & findings (for the RFC)

- **Error body shape.** This API's contract uses `{"error": "...", "message":
  "..."}` (per its `Error` schema). That differs from `folio-api`'s `{"message":
  "..."}`; the new OpenAPI is this API's source of truth and the contract tests
  validate against it.
- **Spec clarification.** The reverse `200` is `oneOf: [CanonicalIdRef,
  IdentifierSet]`. As originally written, `CanonicalIdRef` had no
  `additionalProperties: false`, so an `IdentifierSet` body also validated
  against it — `oneOf` then matches *both* and machine-validation of the
  `include=siblings` response fails. The prototype's spec copy pins
  `CanonicalIdRef` to exactly `{canonicalId}` (commented in `spec/openapi.yaml`).
  **Feedback for the spec authors.**

### Validating the running app against the spec (bonus)

```bash
uv run python src/adapters/run_local.py        # in one shell, on :8000
uvx schemathesis run spec/openapi.yaml --url http://127.0.0.1:8000 \
  --checks response_schema_conformance,status_code_conformance,content_type_conformance
# → 151 generated, all passed (responses conform to the contract)
```

The `apiKey` security checks are intentionally skipped: API keys and throttling are
an API Gateway deployment concern, **out of scope** for this prototype (see
below), so the running app does not enforce them.

## Requesting integration

This API is the proposed resolution to the identifier-translation open question
in `folio-api` (see [RFC 088](https://github.com/wellcomecollection/docs/tree/main/rfcs/088-folio-identity-requesting-migration)),
whose requesting routes currently stub translation with a hard-coded
`WORK_ID_BY_ITEM` table. The consumption pattern — **without changing `folio-api`
in this pass**:

- **`POST …/item-requests`** receives a **canonical** `itemId`. To place the hold
  on FOLIO it needs the FOLIO item UUID, so it calls the **forward** lookup
  `GET /v1/identifiers/{itemId}` and picks the `folio-item` source row.
- **`GET …/item-requests`** lists FOLIO holds carrying FOLIO item UUIDs. It calls
  the **reverse** lookup
  `GET /v1/identifiers/by-source/folio-item/{uuid}?type=Item` to recover the
  canonical item id, then queries the **catalogue API in canonical** for `workId`
  / `workTitle` (explicitly **not** this API's job — this API does id translation
  only).

This demonstrates the **"service"** option for the open question's access
mechanism (vs a direct DB read or a sync); which mechanism production adopts
remains the platform workstream's decision. It also carries the unchanged
dependency: FOLIO item identifiers only reach the registry once the catalogue
pipeline ingests FOLIO-sourced items (item-level predecessor inheritance).

## Out of scope (deliberately)

No AWS deployment; no API keys / throttling / Auth0 / IAM / WAF;
no `workId` / `workTitle` (catalogue API's job); no `aliases` toggle; no writes /
minting; no bare-value reverse lookup without `sourceSystem` (an RFC 085 wish,
not the committed contract). `Cache-Control` / `ETag` are demonstrated as
response headers only.
