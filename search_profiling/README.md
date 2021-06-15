# Search query profiling

This provides a script which can run an elasticsearch query many times, using different search terms each time to overcome caching, and return statistics on the performance of the queries.

## Usage

1. Run `yarn` to install dependencies.
2. Run `yarn refresh-templates` to get template files from the prod/stage search-templates.json endpoints.
3. Create a `.env` file to look like `.env.example` but with valid credentials.
4. Run a test using an index name that has a corresponding file in `query-templates`:
  ```
  yarn profile --index works-2021-06-03 --nSamples 123
  ```
