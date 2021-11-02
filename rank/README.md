# Rank 🎯

Rank allows us to confidently test incremental changes to search relevance on [wellcomecollection.org/collections](https://wellcomecollection.org/collections), without degrading the existing experience.

It uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html) to measure how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL, and more illustrative examples mean more classes of search behaviour/intention are met.

## Getting started

Clone this repo and run:

- `yarn` to install packages
- `yarn env` to populate a local `.env` file.  
   This assumes you're part of the Wellcome Collection team on Vercel. You'll be asked to link your local repo to the project in order to fetch the necessary secrets. The project name is `rank`.

### Developing

To start affecting search relevance, you might want to

- [develop the queries](./docs/developing.md#queries)
- [develop the mappings](./docs/developing.md#mappings)
- [create new tests](./docs/developing.md#test-cases)

### Evaluating relevance

See [the docs](./docs/testing.md) for instructions on running the rank tests.
