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

To start working on some query improvements:

- For works edit [`WorksMultiMatcherQuery.json`](/search/src/test/resources/WorksMultiMatcherQuery.json)
- For images edit [`ImagesMultiMatcherQuery.json`](/search/src/test/resources/ImagesMultiMatcherQuery.json)
- Check homepage for results
- Edit [images](images-scala-file) or [works](works-scala-file) scala files to get [the tests](scala-tests) to pass

[works-scala-file]: /search/src/main/scala/weco/api/search/elasticsearch/WorksMultiMatcher.scala
[images-scala-file]: /search/src/test/scala/weco/api/search/images/ImagesSimilarityTest.scala
[scala-tests]: /search/src/test/scala/weco/api/search/elasticsearch/SearchQueryJsonTest.scala

Results will show up on the homepage.

### Developing the mappings

If we new mappings, we'll need a new index.

- `yarn getIndexConfigs --name {name:=candidate}` will dump the index config into [`./data/indices/{namespace}-{name}`](./data/indices/)
- Edit [`./data/indices/{namespace}-{index}.json`](./data/indices/)
- `yarn putIndexConfig --name {namespace}-{name} --reindex` will craete the new index with the mappings, and start a reindex

[Mappings are currently in the catalogue pipeline repo](catalogue-pipeline-mappings) - so you'll need to copy the JSON there, and get [the tests](search-tests) to pass.

[catalogue-pipeline-mappings]: https://github.com/wellcomecollection/catalogue-pipeline/tree/main/common/internal_model/src/test/resources

[search-tests]: https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/test/scala/weco/catalogue/internal_model/index/SearchIndexConfigJsonTest.scala