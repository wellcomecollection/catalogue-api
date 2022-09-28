# Rank 🎯

Rank allows us to confidently test incremental changes to search relevance on [wellcomecollection.org/collections](https://wellcomecollection.org/collections), without degrading the existing experience.

It uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html) to measure how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL, and more illustrative examples mean more classes of search behaviour/intention are met.

## Getting started

Clone this repo, and run:

- `cd rank`
- `yarn` to install packages
- `yarn env` to populate a local `.env` file. You'll need to be signed into a Wellcome Collection AWS account with access to the `platform-dev` role to do this.
- `yarn rank` to run the relevance tests

## Docs 📖

Rank documentation lives alongside the rest of the search docs in gitbook. You can see the markdown docs [here](../docs/search/rank/).

### Developing

To start affecting search relevance, you might want to

- [develop the queries](../docs/search/rank/developing.md#queries)
- [develop the mappings](../docs/search/rank/developing.md#mappings)
- [create new tests](../docs/search/rank/developing.md#test-cases)

### Evaluating relevance

See [the instructions](../docs/search/rank/testing.md) for running the rank tests.
