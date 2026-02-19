# loadtest

Load testing toolkit for comparing **Elasticsearch cluster backends** against **default** search on the [Wellcome Collection catalogue API](https://developers.wellcomecollection.org/docs/getting-started) using [Vegeta](https://github.com/tsenart/vegeta).

## Prerequisites

- **Python 3.12+**
- **[Vegeta](https://github.com/tsenart/vegeta)** HTTP load testing tool
  ```sh
  brew install vegeta          # macOS
  go install github.com/tsenart/vegeta/v12@latest  # Go
  ```
- **[uv](https://docs.astral.sh/uv/)** (recommended) or pip for Python dependencies

## Setup

```sh
uv sync
```

## Usage

All commands are run via `uv run loadtest`.

### Compare cluster vs default

Runs both search types in parallel, then generates an HTML report. Add `--live` for a live terminal graph.

```sh
uv run loadtest compare [queries_file] --cluster {elser,openai} [--rate N] [--duration D] [--env ENV] [--live]
```

| Option         | Default       | Description                                 |
| -------------- | ------------- | ------------------------------------------- |
| `queries_file` | `queries.txt` | File containing search queries              |
| `--cluster`    | _(required)_  | ES cluster backend: `elser` or `openai`     |
| `--rate`       | `5`           | Requests per second (per test, so 2x total) |
| `--duration`   | `30s`         | Test duration (e.g. `30s`, `1m`)            |
| `--env`        | `dev`         | API environment: `dev`, `stage`, `prod`     |
| `--live`       | off           | Show live terminal graph during test        |

**Examples:**

```sh
uv run loadtest compare queries.txt --cluster elser --rate 10 --duration 60s --env stage
uv run loadtest compare queries.txt --cluster openai --live
```

**Output:**

- Final summary report with latency stats and histograms
- HTML comparison report with interactive Plotly charts in `results/`
- Live terminal scatter plot with rolling averages (with `--live`, requires `plotext`)

### Search (single mode)

Load test a single search type. Without `--cluster`, runs default search. With `--cluster`, runs search against the specified ES cluster backend.

```sh
uv run loadtest search [queries_file] [--cluster {elser,openai}] [--rate N] [--duration D] [--env ENV] [--live]
```

Duration defaults to a single pass through all queries.

**Examples:**

```sh
# Default search
uv run loadtest search queries.txt --rate 5 --duration 30s --env dev

# ELSER cluster with live graph
uv run loadtest search queries.txt --cluster elser --live

# OpenAI cluster
uv run loadtest search queries.txt --cluster openai --env stage
```

### Live graph (standalone)

If you've already generated JSONL result files, you can view the live graph independently:

```sh
python live_graph.py <cluster.jsonl> <default.jsonl> <duration>
```

## Queries file format

One query per line. Blank lines and lines starting with `#` are ignored.

```text
# Broad topic queries
cheeses of the world
history of vaccination

# Medical/scientific queries
genome sequencing techniques
```

## Results

All output is saved to the `results/` directory:

| File pattern                  | Description                         |
| ----------------------------- | ----------------------------------- |
| `<cluster>_<timestamp>.jsonl` | Raw cluster search results          |
| `default_<timestamp>.jsonl`   | Raw default search results          |
| `comparison_<timestamp>.html` | Interactive HTML comparison report  |
| `plot_<timestamp>.html`       | Vegeta HTML plot (single test)      |
| `results_<timestamp>.bin`     | Vegeta binary results (single test) |
