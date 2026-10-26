Translates between Wellcome Collection's public catalogue ids and the ids our
source systems use, in both directions.

## This API needs a key

This is an integration API for services that work with our catalogue, not a
public API. Every request must carry an `x-api-key` header, and a request
without a valid key gets `403 {"message":"Forbidden"}`.

Keys are issued to integrating services by the Wellcome Collection digital
platform team. To ask for one, contact
[digital@wellcomecollection.org](mailto:digital@wellcomecollection.org). Each
consumer has its own key, and a consumer that goes over its rate limit gets
`429`.

## Lookups

- **Canonical to source.** `GET /identifiers/v1/{canonicalId}` returns every
  source identifier that shares a canonical id, most recent first.
- **Source to canonical.** `GET /identifiers/v1/by-source/{sourceSystem}/{value}`
  returns the canonical id for a source identifier. Add `include=siblings` to
  get the other source identifiers as well. `type` selects `Work` (the
  default), `Image` or `Item`.
- `GET /identifiers/v1/source-systems` lists the source systems you can name
  in a by-source lookup.

## Errors

Errors from the API have a JSON body with a stable machine-readable `error`
code and a human-readable `message`. A `404` means there is no public mapping,
whether the id is unknown, not yet assigned, or not a Work, Image or Item.
Errors raised by the gateway itself, such as a missing key or a throttled
request, have a `message` only.

## Caching

Every successful response carries `Cache-Control`. The forward lookup and
`include=siblings` may be cached for 5 minutes, because a canonical id can gain
source identifiers while records migrate between systems. The bare reverse
lookup never changes once assigned, and it and the source systems list may be
cached for a day. Responses other than the bare reverse lookup also carry an
`ETag`: send it back in `If-None-Match` to get a `304` when nothing has
changed.
