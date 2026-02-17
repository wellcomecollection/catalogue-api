#!/usr/bin/env python3
"""
loadtest — Load testing toolkit for comparing cluster backends vs default
search on the Wellcome Collection catalogue API using Vegeta.
"""

import argparse
import json
import math
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
from datetime import datetime

from loadtest.live_graph import (
    parse_duration,
    run_fallback,
    run_plotext,
    run_single_plotext,
    run_single_fallback,
)

OUTPUT_DIR = "results"

ENV_HOSTS = {
    "dev": "api-dev.wellcomecollection.org",
    "stage": "api-stage.wellcomecollection.org",
    "prod": "api.wellcomecollection.org",
}


def env_to_base_url(env: str) -> str:
    host = ENV_HOSTS.get(env)
    if not host:
        print(f"Error: ENV must be dev, stage, or prod (got '{env}')")
        sys.exit(1)
    return f"https://{host}/catalogue/v2/works"


def check_vegeta():
    if not shutil.which("vegeta"):
        print("Error: vegeta not found. Install with: brew install vegeta")
        sys.exit(1)


def read_queries(queries_file: str) -> list[str]:
    if not os.path.isfile(queries_file):
        print(f"Error: queries file '{queries_file}' not found.")
        sys.exit(1)
    queries = []
    with open(queries_file) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            queries.append(line)
    return queries


def build_targets(
    queries: list[str], base_url: str, semantic: bool, cluster: str = "elser"
) -> str:
    """Write vegeta target lines to a temp file. Returns the file path."""
    fd, path = tempfile.mkstemp(suffix=".txt", prefix="vegeta-targets-")
    with os.fdopen(fd, "w") as f:
        for query in queries:
            encoded = urllib.parse.quote(query)
            if semantic:
                f.write(f"GET {base_url}?elasticCluster={cluster}&query={encoded}\n")
            else:
                f.write(f"GET {base_url}?query={encoded}\n")
    return path


def compute_duration(num_targets: int, rate: int) -> str:
    return f"{math.ceil(num_targets / rate)}s"


def run_attack(targets_file: str, rate: int, duration: str, output_bin: str):
    """Run vegeta attack, writing binary output to output_bin."""
    cmd = [
        "vegeta",
        "attack",
        f"-targets={targets_file}",
        f"-rate={rate}/s",
        f"-duration={duration}",
    ]
    with open(output_bin, "wb") as out:
        subprocess.run(cmd, stdout=out, check=True)


def run_parallel_attacks(
    sem_targets: str,
    default_targets: str,
    rate: int,
    duration: str,
    sem_bin: str,
    default_bin: str,
    sem_jsonl: str,
    default_jsonl: str,
):
    """Launch two vegeta attack pipelines in parallel, producing both .bin and .jsonl."""
    # semantic: vegeta attack | tee sem.bin | vegeta encode --to json > sem.jsonl
    sem_jsonl_f = open(sem_jsonl, "w")
    sem_attack = subprocess.Popen(
        [
            "vegeta",
            "attack",
            f"-targets={sem_targets}",
            f"-rate={rate}/s",
            f"-duration={duration}",
        ],
        stdout=subprocess.PIPE,
    )
    sem_tee = subprocess.Popen(
        ["tee", sem_bin],
        stdin=sem_attack.stdout,
        stdout=subprocess.PIPE,
    )
    sem_attack.stdout.close()
    sem_encode = subprocess.Popen(
        ["vegeta", "encode", "--to", "json"],
        stdin=sem_tee.stdout,
        stdout=sem_jsonl_f,
    )
    sem_tee.stdout.close()

    # default: vegeta attack | tee default.bin | vegeta encode --to json > default.jsonl
    default_jsonl_f = open(default_jsonl, "w")
    default_attack = subprocess.Popen(
        [
            "vegeta",
            "attack",
            f"-targets={default_targets}",
            f"-rate={rate}/s",
            f"-duration={duration}",
        ],
        stdout=subprocess.PIPE,
    )
    default_tee = subprocess.Popen(
        ["tee", default_bin],
        stdin=default_attack.stdout,
        stdout=subprocess.PIPE,
    )
    default_attack.stdout.close()
    default_encode = subprocess.Popen(
        ["vegeta", "encode", "--to", "json"],
        stdin=default_tee.stdout,
        stdout=default_jsonl_f,
    )
    default_tee.stdout.close()

    return (sem_attack, sem_tee, sem_encode, sem_jsonl_f), (
        default_attack,
        default_tee,
        default_encode,
        default_jsonl_f,
    )


def wait_pipelines(sem_pipeline, default_pipeline):
    for proc in sem_pipeline[:3]:
        proc.wait()
    sem_pipeline[3].close()
    for proc in default_pipeline[:3]:
        proc.wait()
    default_pipeline[3].close()


def generate_report(bin_path: str, label: str):
    print(f"\n--- {label} ---")
    subprocess.run(["vegeta", "report", bin_path])


def generate_histogram(bin_path: str, label: str):
    print(f"\n{label}:")
    subprocess.run(
        ["vegeta", "report", "-type=hist[0,200ms,500ms,1s,2s,5s,10s]", bin_path]
    )


def generate_plot(bin_path: str, html_path: str):
    try:
        with open(html_path, "w") as f:
            subprocess.run(["vegeta", "plot", bin_path], stdout=f, check=True)
        print(f"Plot saved to {html_path}")
    except subprocess.CalledProcessError:
        pass


def generate_html_report(sem_jsonl: str, default_jsonl: str, output_html: str):
    """Build a self-contained Plotly.js comparison HTML report."""

    def load_jsonl(path):
        results = []
        try:
            with open(path) as f:
                for line in f:
                    if line.strip():
                        try:
                            results.append(json.loads(line))
                        except json.JSONDecodeError:
                            pass
        except FileNotFoundError:
            pass
        return results

    def ts(s):
        return datetime.fromisoformat(s.replace("Z", "+00:00")).timestamp()

    sem = load_jsonl(sem_jsonl)
    default_results = load_jsonl(default_jsonl)

    if sem or default_results:
        all_results = sem + default_results
        t0 = min(ts(r["timestamp"]) for r in all_results)
        sem_t = [ts(r["timestamp"]) - t0 for r in sem]
        sem_l = [r["latency"] / 1e6 for r in sem]
        default_t = [ts(r["timestamp"]) - t0 for r in default_results]
        default_l = [r["latency"] / 1e6 for r in default_results]
        data_js = (
            f"var semT = {json.dumps(sem_t)};\n"
            f"var semL = {json.dumps(sem_l)};\n"
            f"var defaultT = {json.dumps(default_t)};\n"
            f"var defaultL = {json.dumps(default_l)};\n"
        )
    else:
        data_js = "var semT=[],semL=[],defaultT=[],defaultL=[];\n"

    html = f"""<!DOCTYPE html>
<html><head><title>Vegeta Load Test Comparison</title>
<script src="https://cdn.plot.ly/plotly-2.27.0.min.js"></script>
<style>body{{font-family:system-ui;margin:20px;background:#1a1a2e;color:#eee}}h1{{color:#e94560}}.chart{{width:100%;height:500px;margin:20px 0}}</style>
</head><body>
<h1>Semantic vs Default Search &mdash; Load Test Comparison</h1>
<div id="latency" class="chart"></div>
<div id="histogram" class="chart"></div>
<script>
{data_js}
Plotly.newPlot('latency', [
  {{x:semT,y:semL,mode:'markers',name:'Semantic (ELSER)',marker:{{size:5,color:'#e94560',opacity:0.7}}}},
  {{x:defaultT,y:defaultL,mode:'markers',name:'Default',marker:{{size:5,color:'#0f3460',opacity:0.7}}}}
],{{title:'Response Latency Over Time',xaxis:{{title:'Time (s)'}},yaxis:{{title:'Latency (ms)'}},
   paper_bgcolor:'#1a1a2e',plot_bgcolor:'#16213e',font:{{color:'#eee'}}}});
Plotly.newPlot('histogram', [
  {{x:semL,type:'histogram',name:'Semantic (ELSER)',opacity:0.7,marker:{{color:'#e94560'}}}},
  {{x:defaultL,type:'histogram',name:'Default',opacity:0.7,marker:{{color:'#0f3460'}}}}
],{{title:'Latency Distribution',barmode:'overlay',xaxis:{{title:'Latency (ms)'}},yaxis:{{title:'Count'}},
   paper_bgcolor:'#1a1a2e',plot_bgcolor:'#16213e',font:{{color:'#eee'}}}});
</script></body></html>"""

    with open(output_html, "w") as f:
        f.write(html)


# ── Subcommand handlers ──────────────────────────────────────────────


def run_single_attack_pipeline(targets_file, rate, duration, bin_path, jsonl_path):
    """Run vegeta attack | tee bin | vegeta encode --to json > jsonl, returning pipeline procs."""
    jsonl_f = open(jsonl_path, "w")
    attack = subprocess.Popen(
        [
            "vegeta",
            "attack",
            f"-targets={targets_file}",
            f"-rate={rate}/s",
            f"-duration={duration}",
        ],
        stdout=subprocess.PIPE,
    )
    tee = subprocess.Popen(
        ["tee", bin_path],
        stdin=attack.stdout,
        stdout=subprocess.PIPE,
    )
    attack.stdout.close()
    encode = subprocess.Popen(
        ["vegeta", "encode", "--to", "json"],
        stdin=tee.stdout,
        stdout=jsonl_f,
    )
    tee.stdout.close()
    return (attack, tee, encode, jsonl_f)


def wait_single_pipeline(pipeline):
    for proc in pipeline[:3]:
        proc.wait()
    pipeline[3].close()


def cmd_search(args):
    check_vegeta()
    base_url = env_to_base_url(args.env)
    queries = read_queries(args.queries)
    semantic = args.cluster is not None
    targets = build_targets(
        queries, base_url, semantic=semantic, cluster=args.cluster or "elser"
    )

    duration = args.duration
    if duration == "0":
        duration = compute_duration(len(queries), args.rate)
        print(f"Duration: {duration} (one pass through all queries)")

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    mode_tag = f"semantic_{args.cluster}" if semantic else "default"
    results_bin = os.path.join(OUTPUT_DIR, f"results_{mode_tag}_{timestamp}.bin")

    mode_label = f"semantic ({args.cluster})" if semantic else "default"
    print(f"Loaded {len(queries)} queries from {args.queries}")
    print(f"Environment: {args.env} ({ENV_HOSTS[args.env]})")
    print(f"Mode: {mode_label}")
    print(f"Rate: {args.rate}/s")
    if duration != args.duration:
        pass  # already printed above
    else:
        print(f"Duration: {duration}")

    if args.live:
        jsonl_path = os.path.join(OUTPUT_DIR, f"results_{mode_tag}_{timestamp}.jsonl")
        pipeline = run_single_attack_pipeline(
            targets, args.rate, duration, results_bin, jsonl_path
        )

        dur_secs = parse_duration(duration)
        print()
        print("Streaming live results... (Ctrl+C to stop early)")
        print()

        try:
            import plotext  # noqa: F401

            run_single_plotext(jsonl_path, dur_secs, label=mode_label)
        except ImportError:
            run_single_fallback(jsonl_path, dur_secs, label=mode_label)

        wait_single_pipeline(pipeline)
        os.unlink(targets)
    else:
        try:
            run_attack(targets, args.rate, duration, results_bin)
        finally:
            os.unlink(targets)

    print(f"\n=== {mode_label.title()} Search Summary ===")
    subprocess.run(["vegeta", "report", results_bin])

    print("\n=== Latency Histogram ===")
    subprocess.run(
        ["vegeta", "report", "-type=hist[0,200ms,500ms,1s,2s,5s,10s]", results_bin]
    )

    html_path = os.path.join(OUTPUT_DIR, f"plot_{mode_tag}_{timestamp}.html")
    generate_plot(results_bin, html_path)

    print(f"\nRaw results saved to {results_bin}")


def cmd_compare(args):
    check_vegeta()
    base_url = env_to_base_url(args.env)
    queries = read_queries(args.queries)
    sem_targets = build_targets(queries, base_url, semantic=True, cluster=args.cluster)
    default_targets = build_targets(queries, base_url, semantic=False)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    sem_bin = os.path.join(OUTPUT_DIR, f"semantic_{timestamp}.bin")
    default_bin = os.path.join(OUTPUT_DIR, f"default_{timestamp}.bin")
    sem_jsonl = os.path.join(OUTPUT_DIR, f"semantic_{timestamp}.jsonl")
    default_jsonl = os.path.join(OUTPUT_DIR, f"default_{timestamp}.jsonl")

    print("=== Parallel Load Test ===")
    print(f"Environment: {args.env} ({ENV_HOSTS[args.env]})")
    print(f"Queries: {len(queries)} from {args.queries}")
    print(f"Cluster: {args.cluster}")
    print(f"Rate: {args.rate}/s per test ({args.rate}x2 total)")
    print(f"Duration: {args.duration}")
    print()

    print("Starting semantic search attack...")
    print("Starting default search attack...")

    sem_pipeline, default_pipeline = run_parallel_attacks(
        sem_targets,
        default_targets,
        args.rate,
        args.duration,
        sem_bin,
        default_bin,
        sem_jsonl,
        default_jsonl,
    )

    dur_secs = parse_duration(args.duration)

    if args.live:
        print()
        print("Streaming live results... (Ctrl+C to stop early)")
        print()

        try:
            import plotext  # noqa: F401

            run_plotext(sem_jsonl, default_jsonl, dur_secs)
        except ImportError:
            run_fallback(sem_jsonl, default_jsonl, dur_secs)

    wait_pipelines(sem_pipeline, default_pipeline)

    # Clean up target files
    os.unlink(sem_targets)
    os.unlink(default_targets)

    print()
    print("============================================")
    print("=== Final Reports ===")
    print("============================================")

    generate_report(sem_bin, "Semantic Search")
    generate_report(default_bin, "Default Search")

    print("\n--- Latency Histograms ---")
    generate_histogram(sem_bin, "Semantic")
    generate_histogram(default_bin, "Default")

    combined_html = os.path.join(OUTPUT_DIR, f"comparison_{timestamp}.html")
    generate_html_report(sem_jsonl, default_jsonl, combined_html)

    print()
    print("Results saved to:")
    print(f"  Semantic:     {sem_bin}")
    print(f"  Default:      {default_bin}")
    print(f"  HTML report:  {combined_html}")


# ── CLI ───────────────────────────────────────────────────────────────


def add_common_args(parser, default_duration="30s"):
    parser.add_argument(
        "queries",
        nargs="?",
        default="queries.txt",
        help="queries file (default: queries.txt)",
    )
    parser.add_argument(
        "--rate", type=int, default=5, help="requests per second (default: 5)"
    )
    parser.add_argument(
        "--duration",
        default=default_duration,
        help=f"test duration e.g. 30s, 1m (default: {default_duration})",
    )
    parser.add_argument(
        "--env",
        default="dev",
        choices=["dev", "stage", "prod"],
        help="API environment (default: dev)",
    )


def main():
    parser = argparse.ArgumentParser(
        prog="loadtest",
        description="Load testing toolkit for Wellcome Collection catalogue API",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    # compare
    p_compare = subparsers.add_parser(
        "compare", help="Run semantic + default in parallel"
    )
    add_common_args(p_compare)
    p_compare.add_argument(
        "--cluster",
        required=True,
        choices=["elser", "openai"],
        help="semantic cluster to compare against default search",
    )
    p_compare.add_argument(
        "--live", action="store_true", help="show live terminal graph during test"
    )
    p_compare.set_defaults(func=cmd_compare)

    # search
    p_search = subparsers.add_parser(
        "search", help="Search load test (default search, use --cluster for semantic)"
    )
    add_common_args(p_search, default_duration="0")
    p_search.add_argument(
        "--cluster",
        default=None,
        choices=["elser", "openai"],
        help="semantic cluster (omit for default search)",
    )
    p_search.add_argument(
        "--live", action="store_true", help="show live terminal graph during test"
    )
    p_search.set_defaults(func=cmd_search)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
