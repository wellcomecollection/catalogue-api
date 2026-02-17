#!/usr/bin/env python3
"""
Live terminal graph comparing two Vegeta JSON result streams.

Usage: python3 live_graph.py <semantic.jsonl> <default.jsonl> <duration>
"""

import sys
import json
import time
import os
from datetime import datetime, timezone

def read_new_results(filepath, file_pos):
    results = []
    try:
        with open(filepath, "r") as f:
            f.seek(file_pos)
            for line in f:
                line = line.strip()
                if line:
                    try:
                        results.append(json.loads(line))
                    except json.JSONDecodeError:
                        pass
            new_pos = f.tell()
    except FileNotFoundError:
        new_pos = file_pos
    return results, new_pos


def rolling_avg(times, values, window=5):
    if len(values) < window:
        return times, values
    avg_t, avg_v = [], []
    for i in range(window - 1, len(values)):
        avg_t.append(times[i])
        avg_v.append(sum(values[i - window + 1 : i + 1]) / window)
    return avg_t, avg_v


def parse_timestamp(ts):
    """Parse RFC 3339 timestamp string to seconds since epoch."""
    return datetime.fromisoformat(ts.replace("Z", "+00:00")).timestamp()


def parse_duration(duration_str):
    if duration_str.endswith("s"):
        return int(duration_str[:-1])
    elif duration_str.endswith("m"):
        return int(duration_str[:-1]) * 60
    return 30


def run_plotext(sem_file, default_file, dur_secs):
    import plotext as plt

    sem_latencies, default_latencies = [], []
    sem_times, default_times = [], []
    sem_errors = default_errors = 0
    sem_pos = default_pos = 0
    start_time = None
    prev_frame_lines = 0

    for tick in range(dur_secs + 10):
        time.sleep(1)

        new_sem, sem_pos = read_new_results(sem_file, sem_pos)
        new_default, default_pos = read_new_results(default_file, default_pos)

        for r in new_sem:
            lat_ms = r["latency"] / 1_000_000
            ts = parse_timestamp(r["timestamp"])
            if start_time is None:
                start_time = ts
            sem_latencies.append(lat_ms)
            sem_times.append(ts - start_time)
            if r["code"] != 200:
                sem_errors += 1

        for r in new_default:
            lat_ms = r["latency"] / 1_000_000
            ts = parse_timestamp(r["timestamp"])
            if start_time is None:
                start_time = ts
            default_latencies.append(lat_ms)
            default_times.append(ts - start_time)
            if r["code"] != 200:
                default_errors += 1

        if not sem_latencies and not default_latencies:
            continue

        plt.clear_figure()
        plt.theme("dark")
        plt.plot_size(width=100, height=25)
        plt.title("Live Latency Comparison (ms)")
        plt.xlabel("Time (s)")
        plt.ylabel("Latency (ms)")

        if sem_times:
            plt.scatter(
                sem_times,
                sem_latencies,
                label=f"Semantic (n={len(sem_latencies)}, err={sem_errors})",
                marker="dot",
                color="red",
            )
        if default_times:
            plt.scatter(
                default_times,
                default_latencies,
                label=f"Default (n={len(default_latencies)}, err={default_errors})",
                marker="dot",
                color="cyan",
            )

        if len(sem_latencies) > 5:
            rt, rv = rolling_avg(sem_times, sem_latencies)
            plt.plot(rt, rv, label="Semantic avg", color="red+")
        if len(default_latencies) > 5:
            rt, rv = rolling_avg(default_times, default_latencies)
            plt.plot(rt, rv, label="Default avg", color="cyan+")

        graph = plt.build()

        sem_avg = sum(sem_latencies) / len(sem_latencies) if sem_latencies else 0
        default_avg = sum(default_latencies) / len(default_latencies) if default_latencies else 0
        sem_p99 = sorted(sem_latencies)[int(len(sem_latencies) * 0.99)] if sem_latencies else 0
        default_p99 = sorted(default_latencies)[int(len(default_latencies) * 0.99)] if default_latencies else 0

        stats = (
            f"  Semantic:     avg={sem_avg:>8.1f}ms  p99={sem_p99:>8.1f}ms  errors={sem_errors}\n"
            f"  Default:      avg={default_avg:>8.1f}ms  p99={default_p99:>8.1f}ms  errors={default_errors}\n"
            f"  Ratio: semantic is {sem_avg / default_avg if default_avg > 0 else 0:.1f}x "
            f"{'slower' if default_avg == 0 or sem_avg / default_avg > 1 else 'faster'} than default"
        )

        frame = graph + "\n" + stats
        frame_lines = frame.count("\n") + 1

        # Move cursor up to overwrite the previous frame
        if prev_frame_lines > 0:
            sys.stdout.write(f"\033[{prev_frame_lines}A\033[J")

        sys.stdout.write(frame + "\n")
        sys.stdout.flush()
        prev_frame_lines = frame_lines

        if tick > dur_secs + 5:
            break


def run_single_plotext(jsonl_file, dur_secs, label="Search"):
    import plotext as plt

    latencies, times = [], []
    errors = 0
    file_pos = 0
    start_time = None
    prev_frame_lines = 0

    for tick in range(dur_secs + 10):
        time.sleep(1)

        new_results, file_pos = read_new_results(jsonl_file, file_pos)

        for r in new_results:
            lat_ms = r["latency"] / 1_000_000
            ts = parse_timestamp(r["timestamp"])
            if start_time is None:
                start_time = ts
            latencies.append(lat_ms)
            times.append(ts - start_time)
            if r["code"] != 200:
                errors += 1

        if not latencies:
            continue

        plt.clear_figure()
        plt.theme("dark")
        plt.plot_size(width=100, height=25)
        plt.title(f"Live Latency — {label} (ms)")
        plt.xlabel("Time (s)")
        plt.ylabel("Latency (ms)")

        plt.scatter(
            times,
            latencies,
            label=f"{label} (n={len(latencies)}, err={errors})",
            marker="dot",
            color="red",
        )

        if len(latencies) > 5:
            rt, rv = rolling_avg(times, latencies)
            plt.plot(rt, rv, label="Rolling avg", color="red+")

        graph = plt.build()

        avg = sum(latencies) / len(latencies)
        p99 = sorted(latencies)[int(len(latencies) * 0.99)]

        stats = f"  avg={avg:>8.1f}ms  p99={p99:>8.1f}ms  errors={errors}"

        frame = graph + "\n" + stats
        frame_lines = frame.count("\n") + 1

        if prev_frame_lines > 0:
            sys.stdout.write(f"\033[{prev_frame_lines}A\033[J")

        sys.stdout.write(frame + "\n")
        sys.stdout.flush()
        prev_frame_lines = frame_lines

        if tick > dur_secs + 5:
            break


def run_single_fallback(jsonl_file, dur_secs, label="Search"):
    print("(plotext not available — falling back to text output)")
    print(f"{'Time':>6}  {'Latency':>10}  {'Status':>8}")
    print("-" * 30)

    file_pos = 0
    start_time = None

    for tick in range(dur_secs + 10):
        time.sleep(1)
        new_results, file_pos = read_new_results(jsonl_file, file_pos)

        for r in new_results:
            lat = r["latency"] / 1_000_000
            ts = parse_timestamp(r["timestamp"])
            if start_time is None:
                start_time = ts
            t = ts - start_time
            print(f"{t:>6.1f}  {lat:>8.1f}ms  HTTP {r['code']}")

        if tick > dur_secs + 5:
            break


def run_fallback(sem_file, default_file, dur_secs):
    print("(plotext not available — falling back to text output)")
    print(f"{'Time':>6}  {'Latency':>10}  {'Type':>14}  {'Status':>8}")
    print("-" * 46)

    sem_pos = default_pos = 0
    start_time = None

    for tick in range(dur_secs + 10):
        time.sleep(1)
        new_sem, sem_pos = read_new_results(sem_file, sem_pos)
        new_default, default_pos = read_new_results(default_file, default_pos)

        for label, results in [("Semantic", new_sem), ("Default", new_default)]:
            for r in results:
                lat = r["latency"] / 1_000_000
                ts = parse_timestamp(r["timestamp"])
                if start_time is None:
                    start_time = ts
                t = ts - start_time
                print(f"{t:>6.1f}  {lat:>8.1f}ms  {label:>14}  HTTP {r['code']}")

        if tick > dur_secs + 5:
            break


def main():
    if len(sys.argv) < 4:
        print(f"Usage: {sys.argv[0]} <semantic.jsonl> <default.jsonl> <duration>")
        sys.exit(1)

    sem_file = sys.argv[1]
    default_file = sys.argv[2]
    dur_secs = parse_duration(sys.argv[3])

    try:
        import plotext
        run_plotext(sem_file, default_file, dur_secs)
    except ImportError:
        run_fallback(sem_file, default_file, dur_secs)


if __name__ == "__main__":
    main()