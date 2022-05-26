# Testing

## CLI

To run tests from the CLI, run `yarn test --queryEnv=[production|staging|candidate]`.

N.B. this command is typically invoked to test the functionality of an app.  
Here, the `test` command does not test the rank app functionality; It runs the actual rank tests for queries/mappings/indices etc.

## Next app

To run tests in the app, run `yarn dev`, and go to `http://localhost:3000/dev`.

## CI

Tests are automatically run against the pipeline cluster in CI when we deploy a new pipeline to stage.

## Performance

Rank allows us to test the performance (precision, recall, etc) of new candidate queries and mappings, but real-world performance is based on more than just delivering trustworthy results. We also test the speed at which a query runs to make sure that each iteration is faster than the last.

We use real-world search terms from our own analytics systems to assess the performance of our queries. To fetch a set of search terms for testing, run:

```
yarn getSearchTerms
```

To compare the speed of candidate/production queries using those search terms, run:

```
yarn compareQuerySpeed
```
