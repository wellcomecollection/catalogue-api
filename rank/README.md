# Rank 🎯

Rank allows us to confidently test incremental changes to search relevance on [wellcomecollection.org/collections](https://wellcomecollection.org/collections), without degrading the existing experience.

It uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html) to measure how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL, and more illustrative examples mean more classes of search behaviour/intention are met.

## Getting started

### Running the app

- Clone this repo and run:
- `yarn` to install packages
- `yarn env` to populate a `.env` file with secrets
- `yarn dev` to get the local server running

### Developing

To improve search relevance, you might want to

- [develop the queries](./docs/developing.md#query)
- [develop the mappings](./docs/developing.md#mapping)
- [create new tests](./docs/developing.md#tests)
