# Developing with rank

## Queries

Queries are the easiest part of the search-relevance puzzle to modify and test.

- Make your changes to [`WorksMultiMatcherQuery.json`](/search/src/test/resources/WorksMultiMatcherQuery.json) or [`ImagesMultiMatcherQuery.json`](/search/src/test/resources/ImagesMultiMatcherQuery.json).
- Use the `candidate` queryEnv on `/dev` or `/search` to see the results.
- When you're happy with the effect of your changes on the rank tests, you'll need to make the scala used by the API match the JSON used by rank. Edit the [images](images-scala-file) and/or [works](works-scala-file) scala files until [the tests](scala-tests) pass.

[works-scala-file]: /search/src/main/scala/weco/api/search/elasticsearch/WorksMultiMatcher.scala
[images-scala-file]: /search/src/test/scala/weco/api/search/images/ImagesSimilarityTest.scala
[scala-tests]: /search/src/test/scala/weco/api/search/elasticsearch/SearchQueryJsonTest.scala

## Mappings

We often want to test against indices that have new or altered analyzers, mappings, or settings. To create and populate a new index:

- Run `yarn getIndexConfig` to fetch mappings and other config from existing indices in the rank cluster. The config for your chosen indices will be written to [`./data/indices/`](./data/indices/).
- Edit the file(s) to your needs, using existing mappings as a starting point.
- Run `yarn createIndex` to create the new index in the rank cluster from the edited mappings. Add the `--reindex` flag to immediately start a reindex.
- If you need to monitor the state of a reindex, run `yarn checkTask`.
- If you need to delete a candidate index, run `yarn deleteIndex`
- If you need to update a candidate index, run `yarn updateIndex`

To see the results of your changes, select your new index on `/dev` or `/search`.

You might need to edit the query to fit the new mapping, following [these instructions](#queries).

Before deploying your changes, you'll need to make sure the scala version of the mapping used by the pipeline matches the JSON version you've been testing. You should copy your JSON mapping over to [the catalogue pipeline repo](catalogue-pipeline-mappings), and edit the scala until [the tests](search-tests) pass.

[search-tests]: https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/test/scala/weco/catalogue/internal_model/index/SearchIndexConfigJsonTest.scala
[catalogue-pipeline-mappings]: https://github.com/wellcomecollection/catalogue-pipeline/tree/main/common/internal_model/src/test/resources

## Test cases

We collect test cases directly from the stakeholders and feedback channels for wellcomecollection.org.

Each test should represent a _search intention_ - a class of search which we see real users performing. For example

Tests should be grouped according to the following structure:

- `id`, `label`, and `description` - describing what each group of cases is testing
- `metric` - an [Elasticsearch metric](elasticsearch-metrics) to run the cases against
- `eval` - an optional, alternative evaluation method to apply to the metric score returned by elastic
- `searchTemplateAugmentation` - an optional augmentation to the query, eg a filter
- `cases` - the list of search terms and corresponding results to be tested

Each test case in that list should contain:

- `query` - the search terms a researcher uses
- `ratings` - IDs of documents that we want to evaluate against the results
- `description` - a description of the search intention which is embodied by the test

[elasticsearch-metrics]: (https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html#_available_evaluation_metrics)
