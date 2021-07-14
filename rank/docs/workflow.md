# Workflow

There is no fixed workflow to rank, but after having used it for a while, some patterns have come up.

## Test cases

We generally collect test cases from the different [feedback channels](feedback-channels) for wellcomecollection.org.

The properties of a test case are

- `query` - the search terms a researcher uses
- `ratings` - IDs of documents that we want to evaluate against the results
- `metric` - an [elasticesarch metric](elasticsearch-metrics) to run the ratings against
- `eval` - an evaluation method to run against the metric score returned by elastic

Often `ratings` are documents you expect in the results. There are occasions though,
like testing for false positives, where you would want to have ratings that you don't
expect in the results, hence the ability to write a custom `eval` method.

[elasticsearch-metrics]: (https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html#_available_evaluation_metrics)
