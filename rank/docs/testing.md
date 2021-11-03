# Testing

## CLI

To run tests from the CLI, run `yarn test --queryEnv=[production|staging|candidate]`.

N.B. this command is typically invoked to test the functionality of an app.  
Here, the `test` command does not test the rank app functionality; It runs the actual rank tests for queries/mappings/indices etc.

## Next app

To run tests in the app, run `yarn dev`, and go to `http://localhost:3000/dev`.

## CI

TODO: tests should automatically run against the pipeline cluster in CI when we deploy a new pipeline to stage.
