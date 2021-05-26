# Rank

Rank is our source of truth for the goodness of search on wellcomecollection.org.

Rank uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html), which measures how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL.

## Developing

Clone the repo, and run:

- `yarn install` to install packages
- `yarn link` to link your repo with the project in vercel
- `yarn env` to populate a .env file with secrets
- `yarn dev` to get the local server running

Take a look at the [docs](/docs) to understand how the service works, or how to run experiments with new queries, mappings, or examples.
