# Developing test cases

We collect test cases directly from the stakeholders and feedback channels for wellcomecollection.org.

Each test should represent a _search intention_ - a class of search which we see real users performing. For example

## Test cases

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
