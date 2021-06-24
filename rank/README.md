# Rank

Allows us to confidently make small, incremental changes to our search, while maintaining 
an overall satifying search experience.

## How?

Rank uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html), which measures how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL.

## Get started

### Running the app
- Clone the repo, and run:
- `yarn install` to install packages
- `yarn link` to link your repo with the project in vercel
- `yarn env` to populate a .env file with secrets
- `yarn dev` to get the local server running

### Developing the query
After a clean install and run, you will be able to see how our prod query is doing.

To start working on some improvements:
- `yarn devInit` this will copy our prod index config and queries into `/data/{indices|queries}`
- You can then edit the JSON `/data/queries/ccr--{prodIndexName}`. These changes will be tested the results visible on the `tests` page
- Once you're happy `yarn devPublish {namespace} {queryName}`. This publishes the JSON to the scala app in [`/search/src/test/resources/`](/search/src/test/resources/)
- You'll need to now go into [`SearchQueryJsonTest.scala`](/search/src/test/scala/uk/ac/wellcome/platform/api/search/elasticsearch/SearchQueryJsonTest.scala), and update the relevant scala query to match the newly updated JSON