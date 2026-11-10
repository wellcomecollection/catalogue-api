# Items API v3

The items of a work, with their current availability. Items come from the
index. Those a reader could request, or that are on hold for another
reader, are refreshed from FOLIO.

The service does not exist yet. This contract is
[platform#6607](https://github.com/wellcomecollection/platform/issues/6607),
and it replaces the v2 items service in [`items/`](../items), which reads
Sierra. The contract is [`spec/openapi.yaml`](spec/openapi.yaml). This file
explains what it leaves implicit.

## What a request does

`GET /catalogue/v3/works/{workId}/items` returns the same shape as v2 at the
same path, with `v3` in place of `v2`. The website is the only consumer, and
will move from one to the other by changing that segment
([platform#6638](https://github.com/wellcomecollection/platform/issues/6638)).
It is served at `api.wellcomecollection.org` and
`api-stage.wellcomecollection.org`.

Three paths run in parallel:

- **The index path** reads the work and its items from the index. This is
  the data the response is built from.
- **The availability path** makes three calls, each needing the one before
  it. FOLIO only answers to its own ids, so the first translates the canonical
  work id into FOLIO's id for the instance. The second asks FOLIO for the
  current state of the items on that instance: status, location and whether
  each is suppressed. The third asks for the open requests against those
  items, which is how the service learns that someone else holds an item.
  How the first call is made is discussed below.
- **The opening times path** reads the library's opening times from the
  Content API. This call does not depend on the work, so it can start
  immediately. v2 makes it after the Sierra response.

When all three have finished, the service combines them. Stale items take
their access condition from FOLIO. An item is stale if the index says a
reader could request it, or that it is temporarily unavailable for a reason
other than conservation or digitisation. The test is v2's, ported as it is
([platform#6654](https://github.com/wellcomecollection/platform/issues/6654)).
Items that are then requestable gain the dates on which they could be
viewed, worked out from the opening times and from whether the item is on
site or in deep store. Other items are left as indexed. The list is returned
in catalogue order.

The response takes as long as the slowest path, not the sum of all the calls.

## When an upstream fails

The index is the one dependency the response cannot do without. If it cannot
be read, the request fails with a `500`, as it does in v2. Every other
upstream degrades in a set way.

**FOLIO, or the lookup that finds FOLIO ids.** If either fails, the service
returns the items as indexed, with the access conditions from the last
indexing run and no `availableDates`, and sets `Cache-Control: no-store`.
The body has the same shape as a full response, so the page still renders.
The access conditions may be out of date, so an item shown as requestable
may have been taken since the last indexing run. A reader cannot act on
that: without `availableDates` the website does not let a request be
confirmed, and shows an error message instead, as it does in v2.

The client cannot tell a degraded response from a full one, so the service
must make it visible. It logs a warning with the work id and the upstream
that failed, and counts degraded responses in a metric separate from
successful refreshes, with the failed upstream as a dimension. An outage
should be visible on a dashboard, and the two causes should be distinguishable.

**The Content API.** If opening times cannot be read, no item carries
`availableDates`. The access conditions are still current, so the response
is cached as normal.

A `500` therefore means the index could not be read or the service itself
failed.

## Caching

The edge already caches `/catalogue/*` for ten seconds, as a guard against
request floods. That is the behaviour's maximum TTL, and CloudFront clamps
whatever the origin asks for to it, so a longer `max-age` from the service
would still be cached for ten seconds. Raising it means changing the edge
configuration in platform-infrastructure. The contract therefore says
`public, max-age=10` on a full response, which records what happens today,
and `no-store` on a degraded response, which is new. A degraded v2
response is cached like any other.

A cached response can be up to ten seconds old. The change most likely in
that time is a hold placed by someone else. A reader who then requests the
item is refused by FOLIO, and sees an error message as they would from a
degraded response.

## Timeouts

API Gateway stops waiting after 29 seconds, and a request cut off there is a
failure, not a degraded response. The service must give up on its upstreams
before then and answer with what it has.

One deadline covers all three paths, since they run in parallel and there is
no point in abandoning one while still waiting for another. Provisionally it
is 5 seconds. When it passes, the service builds the response from whatever
has arrived: items whose availability has not come back keep their indexed
access conditions without `availableDates` and the response carries
`no-store`, opening times that have not come back leave every item without
`availableDates`, and an index read that has not come back fails the request
with a `500`. The Lambda's own timeout must be a little longer than the
deadline, or the function is terminated before it can answer.

The figure is provisional. The three calls on the availability path were
measured from a laptop. The Identifiers API runs in `eu-west-1`, so a Lambda
there will reach it faster:

- Identifiers API: ten warm lookups had a median of about 160 ms, and cold
  starts took between one and three seconds.
- FOLIO items: fifteen `items-and-holdings` lookups for single instances
  had a median of about 95 ms, between 79 and 157 ms, and a call for eight
  instances cost the same as a call for one.
- FOLIO open requests: ten lookups for single items had a median of about
  95 ms, between 78 and 165 ms, and a call for five items took 127 ms. The
  cost is per call, not per item.

A warm request on the availability path therefore costs about 350 ms, and a
cold Identifiers API start is the one thing the deadline has to absorb.

## Assumptions

- **The endpoint is not patron-aware.** "Could be requested" means by a reader in
  general, not by the caller. Whether per-reader requestability belongs here
  is [platform#6622](https://github.com/wellcomecollection/platform/issues/6622).
- **v3 is a new route on the gateway that serves v2.** The website's toggle
  changes only the version segment of a URL whose hostname is fixed per
  environment, so v3 has to answer where v2 does. The shape is confirmed in
  [platform#6637](https://github.com/wellcomecollection/platform/issues/6637).
- **A merged work is a `410`.** The catalogue API redirects a merged work
  with a `302`. v2 has no case for that status and returns a `500`. The
  website follows the redirect before it asks for items, so the case is rare.
- **`availableDates` stays.** The website uses it to offer viewing dates. The
  v2 rule that tells on site from deep store reads Sierra location codes, and
  their FOLIO equivalent is an open question with library operations
  ([platform#6655](https://github.com/wellcomecollection/platform/issues/6655)).
- **The manifest follows v2.** The service serves `/management/manifest` and
  the gateway exposes it at `/catalogue/management/items-v3/manifest`, as
  v2's is at `/catalogue/management/items/manifest`. The route is part of
  platform#6637.

## Where the FOLIO ids come from

This document assumes the service asks the Identifiers API for the FOLIO
instance id of the work, which is the design recorded in the planning
repository. The alternative is to read the ids from the index: the FOLIO
pipeline writes a `folio-instance` identifier on each work and a `folio-item`
identifier on each item. This option has not been considered yet.

Reading from the index is likely faster and removes a dependency. The
availability path then starts after the index read instead of beside it, but
the parallel design only wins if the Identifiers API answers faster than the
index does, and the measurements above show that it does not: a warm lookup
takes about 160 ms against roughly 130 ms for a work from the catalogue API,
and a cold start takes seconds. It also removes one of the two failure
cases: there is no lookup to fail, and a work without FOLIO identifiers has
nothing to refresh, so the only upstream that can fail on the availability
path is FOLIO itself.

Asking the Identifiers API keeps the two FOLIO services consistent. The
requesting service will resolve item ids through it
([platform#6633](https://github.com/wellcomecollection/platform/issues/6633)),
so the items service would resolve work ids the same way.

The contract is the same either way. The degradation rules above treat the
lookup and FOLIO as one chain, so a failure in either gives the same response.

## Checking the spec

```sh
yarn install
yarn lint:openapi
```

CI runs the same check on every change under `items_v3/`.
