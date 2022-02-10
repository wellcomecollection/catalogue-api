# internal_model_tool

We want to make sure the API only ever reads from an index with a compatible version of `internal_model`.
If it reads from an incompatible index, it may be unable to deserialise the index as Works, and the public API will start crashing.

The API checks the internal model version at startup; this additionally checks for compatibility in CI.
This means we're less likely to try to deploy an API with an incompatible version, because it won't pass Buildkite.
