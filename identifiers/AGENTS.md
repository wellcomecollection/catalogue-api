# identifiers conventions

The Identifiers API: a read-only lookup that translates between our canonical catalogue
ids and the ids source systems use. It ships as a Python Lambda behind API Gateway.

Start with [README.md](README.md) for what the API does, how it is layered, and how to run
it locally. Don't duplicate that material here.

Always `cd identifiers/` before any `uv run ...` command: this project has its own
`uv.lock`, `.python-version` (3.12) and virtualenv.

## Layout cheat sheet

- `src/core/` holds validation, models, the `Repository` protocol and every lookup rule
  (ordering, `isAlias`, ETag, cache policy, the 404 rules). It imports no web framework and
  no database driver.
- `src/adapters/` holds everything touching the outside world: `sqlite_repo.py` and
  `rds_data_repo.py` (two implementations of the same `Repository` protocol),
  `handler.py` (the Lambda proxy handler, which picks the backend) and `run_local.py`
  (a stdlib HTTP invoker for local runs).
- `spec/openapi.yaml` is the authoritative contract. `src/adapters/db/` holds the schema
  and the seed fixtures, next to the module that reads them so that copying `src/` into
  the image carries them along.
- `tests/` mirrors `src/`. `src/` is packaged via `[tool.uv] package = true`, so import as
  `from core.service ...` and `from adapters.handler ...`, **not** `from src.core ...`.

## Before finalising a change

Run from inside `identifiers/`:

```bash
uv run pytest
uv run mypy .
uv run ruff format
uv run ruff check --fix
```

These are the four checks CI runs, through the shared
`wellcomecollection/.github/.github/actions/python_check@main` action. CI pins ruff to a
fixed version while the `dev` group leaves it unpinned, so if a formatting result
disagrees with CI, reproduce it with `uvx ruff@<version>` using the version that action
names.

## Dependencies

- Always use `uv`, never pip or poetry. Add new deps with `uv add ...`, dev-only deps with
  `uv add --dev ...`. Dev dependencies live in `[dependency-groups]` in `pyproject.toml`.
- `requires-python` is capped below 3.13 to match catalogue_graph.
- `rds` is an optional group holding boto3, needed only to run against the real Aurora ID
  Registry (`IDENTIFIERS_BACKEND=rds`). The SQLite path and the whole test suite run
  without it, so don't move boto3 into the default group.
- Don't commit `uv.lock` changes unless you intentionally changed dependencies.

## Testing

- `pytest` config lives in `pyproject.toml`. The whole suite is hermetic: no network, no
  boto3, no external stub. The seeded in-memory SQLite store is the fixture, and
  `tests/test_rds_data_repo.py` uses a fake Data API client.
- The contract tests synthesize API Gateway proxy events, call the same `handler` a Lambda
  would, and validate the response against `spec/openapi.yaml` with openapi-core. If you
  change a response shape, the spec is what has to agree.
- Shared fixtures (`invoke`, `assert_contract`, `body`) live in `tests/conftest.py`.

## Type checking

mypy runs with catalogue_graph's settings (`disallow_untyped_defs`,
`disallow_untyped_calls`, `warn_return_any`, ...) over `tests/` as well as `src/`. New and
changed functions need full annotations, test functions included. External libraries
without stubs belong in a `[[tool.mypy.overrides]]` entry in `pyproject.toml`, rather than
scattered `# type: ignore` comments.

## Don'ts

- Don't import from `src.` The `src/` directory is packaged, so imports start at `core.`
  and `adapters.`
- Don't put HTTP or storage detail into `core/`. The point of the split is that changing
  the backend touches nothing there.
- Don't add write statements to `adapters/rds_data_repo.py`. This API is a read-only
  projection and all writes belong to the ID Minter (RFC 083). The `_assert_read_only`
  guard enforces that at runtime and should stay.
- Don't introduce another Python tool (black, isort, flake8, poetry): ruff and mypy cover
  formatting, linting and typing.
