# Developing the mappings

We often want to test against indices that have new or altered analyzers, mappings, or settings. To create and populate a new index:

- Run `yarn getIndexConfig` to fetch mappings and other config from existing indices in the rank cluster. The config for your chosen indices will be written to [`./data/indices/`](./data/indices/).
- Edit the file(s) to your needs, using existing mappings as a starting point.
- Run `yarn createIndex` to create the new index in the rank cluster from the edited mappings. Add the `--reindex` flag to immediately start a reindex.
- If you need to monitor the state of a reindex, run `yarn checkTask [TASK_ID]`.
- If you need to delete a candidate index, run `yarn deleteIndex`
- If you need to update a candidate index, run `yarn updateIndex`

To see the results of your changes, select your new index on `/dev` or `/search`.

You might need to edit the query to fit the new mapping, following [these instructions](./queries.md).

Before deploying your changes, you'll need to make sure the scala version of the mapping used by the pipeline matches the JSON version you've been testing. You should copy your JSON mapping over to [the catalogue pipeline repo](catalogue-pipeline-mappings), and edit the scala until [the tests](search-tests) pass.

[search-tests]: https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/test/scala/weco/catalogue/internal_model/index/SearchIndexConfigJsonTest.scala
[catalogue-pipeline-mappings]: https://github.com/wellcomecollection/catalogue-pipeline/tree/main/common/internal_model/src/test/resources
