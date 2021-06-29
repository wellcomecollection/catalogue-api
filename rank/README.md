# Rank

Allows us to confidently make small, incremental changes to our search, while maintaining
an overall satisfying search experience.

## How?

Rank uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html), which measures how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL.

## Get started

### Running the app

- Clone the repo, and run:
- `yarn` to install packages
- `yarn env` to populate a .env file with secrets
- `yarn dev` to get the local server running

### Developing the query

After a clean install and run, you will be able to see how our prod query is doing.

To start working on some improvements:

- `yarn devInit` this will copy our prod index config and queries into `/data/{indices|queries}`
- Edit `/data/queries/ccr--{prodIndexName}`. This tests results will be available on the index page
- `yarn devPublish {namespace} {queryName}` publishes the JSON to the scala app in [`/search/src/test/resources/`](/search/src/test/resources/)
- Go to [`SearchQueryJsonTest.scala`](/search/src/test/scala/uk/ac/wellcome/platform/api/search/elasticsearch/SearchQueryJsonTest.scala), and update the relevant scala query to match the newly updated JSON

### Developing the mappings

If we new mappings, we'll need a new index.

When running the `devInit` script, you will be asked a if you need new mappings.
If so:

- A new mappings file will be copied from the original index into `/data/indices/{index}`
- Edit `/data/indices/{index}`
- `yarn createIndex --from {index} --reindex` will craete the new index with the mappings, and start a reindex

[Mappings are currently in the catalogue pipeline repo](catalogue-pipeline-mappings) - so you'll need to copy the JSON there,
and get [the tests](https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/test/scala/weco/catalogue/internal_model/index/SearchIndexConfigJsonTest.scala) to pass.

[catalogue-pipeline-mappings]: https://github.com/wellcomecollection/catalogue-pipeline/tree/main/common/internal_model/src/test/resources