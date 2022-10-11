# Rank 🎯

Rank allows us to confidently test incremental changes to search relevance on [wellcomecollection.org/collections](https://wellcomecollection.org/collections), without degrading the existing experience.

It uses [elasticsearch's `rank_eval` API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html) to measure how well a query returns the "expected" results for a set of known search terms. If the queries return the results we expect, we know we're meeting a baseline performance requirement. Better scores on those examples should mean better search satisfaction IRL, and more illustrative examples mean more classes of search behaviour/intention are met.

## Running locally

Clone this repo, `cd` into `rank`, and run:

- `yarn` to install packages
- `yarn rank` to run the relevance tests

<details>
  <summary>Matching the CI environment with docker</summary>

The following command will match the environment used in CI exactly, explicitly using the AWS credentials in your `~/.aws` directory to fetch credentials for the ES cluster.

```sh
docker run -it \
  -v $HOME/.aws:/root/.aws \
  -v $(git rev-parse --show-toplevel):/catalogue-api \
  --workdir /catalogue-api/rank \
  --env AWS_PROFILE=platform-dev \
  public.ecr.aws/docker/library/node:14-slim \
  yarn rank
```

</details>

## Docs 📖

Rank documentation lives alongside the rest of the search docs in gitbook. You can see the markdown docs [here](../docs/search/rank/README.md).

### Developing

To start affecting search relevance, you might want to

- [develop the queries](../docs/search/rank/developing.md#queries)
- [develop the mappings](../docs/search/rank/developing.md#mappings)
- [create new tests](../docs/search/rank/developing.md#test-cases)

### Evaluating relevance

See [the instructions](../docs/search/rank/testing.md) for running the rank tests.
