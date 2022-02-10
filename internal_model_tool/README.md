# internal_model_tool

We want to make sure the API only ever reads from an index with a compatible version of `internal_model`.
If it reads from an incompatible index, it may be unable to deserialise the index as Works, and the public API will start crashing.
The API

These tools help us manage this compatibility.

## Updating the version of internal_model

To bump to the latest version of internal_model which is supported by the current index:

```console
$ python3 bump.py
```

This saves you looking it up manually.

## Checking the version of internal_model is compatible

To check the configured version of internal_model is compatible with the current index:

```console
$ python3 check_compatibility.py
```

The API checks the internal model version at startup; this check is run in CI to make it less likely that we'll deploy an API that's incompatible with its index.
