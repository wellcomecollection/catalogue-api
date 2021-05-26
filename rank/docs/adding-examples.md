# Adding examples to rank

Known examples for rank eval are stored in `/data/ratings`, with image examples in `images.ts` and works in `works.ts`.

The file is broken into sections, each of which contains a set of `examples` and a `metric`.

The sections should names which describe the search intention which groups them together, eg `languages`.

Each example within a section takes the form:

```json
{
  "query": "everest chest",
  "ratings": [
    "bt9yvss2",
    "erth8sur",
    "fddgu7pe",
    "qbvq42t6",
    "u6ejpuxu",
    "xskq2fsc",
    "prrq5ajp",
    "zw53jx3j"
  ]
}
```

where `query` contains the search term(s), and `ratings` contains the IDs of the expected results.

The `examples` in a section are assessed according to their `metric`. The list of elasticsearch's built in metrics and how to use them is [here](https://www.elastic.co/guide/en/elasticsearch/reference/master/search-rank-eval.html#_available_evaluation_metrics).

## Augmenting metrics

Where they're found to be insufficient, the built in metrics can be tweaked to suit the specific needs of the examples.

(WIP https://github.com/wellcomecollection/rank/pull/50)
