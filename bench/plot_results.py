#!/usr/bin/env python3
"""Render the figures used in BENCHMARK_REPORT.md from bench/results/summary.csv."""
from __future__ import annotations

import csv
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RESULTS = ROOT / "bench" / "results"
MPL = ROOT / "bench" / ".mpl"
MPL.mkdir(exist_ok=True)
os.environ.setdefault("MPLCONFIGDIR", str(MPL))

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

BASELINE = "#4C78A8"
NEW = "#E45756"
GRID = "#D9D9D9"


def load():
    with (RESULTS / "summary.csv").open(encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def number(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def setup():
    plt.rcParams.update(
        {
            "figure.dpi": 120,
            "savefig.dpi": 200,
            "font.size": 9,
            "axes.grid": True,
            "grid.color": GRID,
            "grid.linewidth": 0.6,
            "axes.spines.top": False,
            "axes.spines.right": False,
        }
    )


def fig_read_time(rows):
    setup()
    fig, ax = plt.subplots(figsize=(5.2, 3.4))
    for impl, color, label in [
        ("baseline", BASELINE, "Baseline (pre-optimization)"),
        ("new", NEW, "Optimized"),
    ]:
        data = [
            r for r in rows
            if r["impl"] == impl and r["mode"] == "read" and number(r["read_ms"]) is not None
        ]
        data.sort(key=lambda r: int(r["rows"]))
        ax.plot(
            [int(r["rows"]) / 1000 for r in data],
            [number(r["read_ms"]) for r in data],
            marker="o",
            markersize=4,
            linewidth=1.8,
            color=color,
            label=label,
        )
    oom = [r for r in rows if r["impl"] == "baseline" and r["status"] == "oom"]
    for row in oom:
        ax.axvline(int(row["rows"]) / 1000, color=BASELINE, linestyle=":", linewidth=1)
        ax.annotate(
            "Baseline OOM",
            xy=(int(row["rows"]) / 1000, ax.get_ylim()[0] + 200),
            xytext=(int(row["rows"]) / 1000 - 55, ax.get_ylim()[1] * 0.55),
            color=BASELINE,
            fontsize=8,
            arrowprops=dict(arrowstyle="->", color=BASELINE, lw=0.8),
        )
    ax.set_xlabel("Rows (thousands, 15 columns)")
    ax.set_ylabel("CSV read time (ms)")
    ax.set_title("CSV read time (JVM, 3-run median)")
    ax.legend(frameon=False, fontsize=8)
    fig.tight_layout()
    fig.savefig(RESULTS / "fig_read_time.png")
    plt.close(fig)


def fig_memory(rows):
    setup()
    fig, axes = plt.subplots(1, 2, figsize=(8.4, 3.3))
    for ax, key, title in [
        (axes[0], "used_mb", "Retained heap after GC"),
        (axes[1], "peak_mb", "Sampled peak heap"),
    ]:
        for impl, color, label in [
            ("baseline", BASELINE, "Baseline"),
            ("new", NEW, "Optimized"),
        ]:
            data = [
                r for r in rows
                if r["impl"] == impl and r["mode"] == "read" and number(r[key]) is not None
            ]
            data.sort(key=lambda r: int(r["rows"]))
            ax.plot(
                [int(r["rows"]) / 1000 for r in data],
                [number(r[key]) for r in data],
                marker="o",
                markersize=4,
                linewidth=1.8,
                color=color,
                label=label,
            )
        ax.set_xlabel("Rows (thousands)")
        ax.set_ylabel("Heap (MB)")
        ax.set_title(title)
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", ncol=2, frameon=False, fontsize=8)
    fig.tight_layout(rect=(0, 0, 1, 0.9))
    fig.savefig(RESULTS / "fig_read_memory.png")
    plt.close(fig)


def fig_ops(rows):
    setup()
    order = ["sum", "mean", "std", "describe", "sort", "filter", "groupby", "merge"]
    labels = {
        "sum": "sum", "mean": "mean", "std": "std", "describe": "describe",
        "sort": "sort", "filter": "filter", "groupby": "groupBySum", "merge": "merge",
    }
    baseline, optimized, speedups = [], [], []
    for mode in order:
        base = next(
            (r for r in rows if r["impl"] == "baseline" and r["mode"] == mode), None
        )
        new = next((r for r in rows if r["impl"] == "new" and r["mode"] == mode), None)
        if not base or not new:
            continue
        baseline.append(number(base["op_ms"]))
        optimized.append(number(new["op_ms"]))
        speedups.append(
            number(base["op_ms"]) / number(new["op_ms"]) if number(new["op_ms"]) else 0
        )

    fig, ax = plt.subplots(figsize=(6.6, 3.6))
    x = range(len(baseline))
    width = 0.38
    ax.bar([i - width / 2 for i in x], baseline, width, color=BASELINE, label="Baseline")
    ax.bar([i + width / 2 for i in x], optimized, width, color=NEW, label="Optimized")
    ax.set_yscale("log")
    ax.set_ylabel("Operation time (ms, log scale)")
    ax.set_xticks(list(x))
    ax.set_xticklabels([labels[m] for m in order], rotation=20, ha="right")
    for i, speedup in enumerate(speedups):
        ax.annotate(
            f"{speedup:.1f}x",
            xy=(i, max(baseline[i], optimized[i]) * 1.25),
            ha="center",
            fontsize=8,
            color="#333333",
        )
    ax.legend(frameon=False, fontsize=8)
    ax.set_title("50k rows / 15 columns (merge: 5k x 5k)")
    fig.tight_layout()
    fig.savefig(RESULTS / "fig_ops_time.png")
    plt.close(fig)


def fig_speedup(rows):
    setup()
    order = ["sum", "mean", "std", "describe", "sort", "filter", "groupby", "merge"]
    labels = {
        "sum": "sum", "mean": "mean", "std": "std", "describe": "describe",
        "sort": "sort", "filter": "filter", "groupby": "groupBySum", "merge": "merge",
    }
    values, names = [], []
    for mode in order:
        base = next((r for r in rows if r["impl"] == "baseline" and r["mode"] == mode), None)
        new = next((r for r in rows if r["impl"] == "new" and r["mode"] == mode), None)
        if not base or not new:
            continue
        speed = number(base["op_ms"]) / number(new["op_ms"])
        values.append(speed)
        names.append(labels[mode])
    fig, ax = plt.subplots(figsize=(5.4, 3.2))
    bars = ax.barh(names[::-1], values[::-1], color=NEW, height=0.6)
    ax.set_xscale("log")
    ax.set_xlabel("Speedup (baseline / optimized, log scale)")
    ax.set_title("Operation speedup at 50k rows")
    for bar, value in zip(bars, values[::-1]):
        ax.annotate(f"{value:.1f}x", xy=(value * 1.05, bar.get_y() + bar.get_height() / 2),
                    va="center", fontsize=8)
    fig.tight_layout()
    fig.savefig(RESULTS / "fig_ops_speedup.png")
    plt.close(fig)



def parse_android(path):
    data = {"ok": [], "oom": []}
    if not Path(path).exists():
        return data
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if not line.startswith("read rows="):
            continue
        parts = dict(part.split("=", 1) for part in line.split() if "=" in part)
        rows = int(parts["rows"])
        if parts.get("status") == "ok":
            data["ok"].append((rows, float(parts["ms"]), float(parts["retainedMb"])))
        else:
            data["oom"].append((rows, float(parts["ms"])))
    return data


def fig_android():
    setup()
    new = parse_android(RESULTS / "android_new.log")
    base = parse_android(RESULTS / "android_baseline.log")
    if not new["ok"] or not base["ok"]:
        return
    fig, ax = plt.subplots(figsize=(5.2, 3.4))
    ax.plot([r / 1000 for r, _, _ in base["ok"]], [t for _, t, _ in base["ok"]],
            marker="o", color=BASELINE, linewidth=1.8, label="Baseline")
    ax.plot([r / 1000 for r, _, _ in new["ok"]], [t for _, t, _ in new["ok"]],
            marker="o", color=NEW, linewidth=1.8, label="Optimized")
    for rows, ms in base["oom"]:
        ax.axvline(rows / 1000, color=BASELINE, linestyle=":", linewidth=1)
        ax.scatter([rows / 1000], [17600], marker="x", color=BASELINE, zorder=5)
        ax.annotate(
            f"OOM ({ms / 1000:.1f}s)",
            xy=(rows / 1000, 17600),
            xytext=(rows / 1000 - 8, 16000),
            color=BASELINE,
            fontsize=7,
            ha="right",
            arrowprops=dict(arrowstyle="-", color=BASELINE, lw=0.6),
        )
    ax.set_ylim(0, 18500)
    ax.set_xlabel("Rows (thousands, 15 columns)")
    ax.set_ylabel("CSV read time (ms)")
    ax.set_title("Android emulator (192 MB heap, arm64)")
    ax.legend(frameon=False, fontsize=8)
    fig.tight_layout()
    fig.savefig(RESULTS / "fig_android_read.png")
    plt.close(fig)


def fig_android_memory():
    setup()
    new = parse_android(RESULTS / "android_new.log")["ok"]
    base = parse_android(RESULTS / "android_baseline.log")["ok"]
    fig, ax = plt.subplots(figsize=(5.0, 3.2))
    ax.plot([r / 1000 for r, _, _ in base], [m for _, _, m in base],
            marker="o", color=BASELINE, linewidth=1.8, label="Baseline")
    ax.plot([r / 1000 for r, _, _ in new], [m for _, _, m in new],
            marker="o", color=NEW, linewidth=1.8, label="Optimized")
    ax.set_xlabel("Rows (thousands)")
    ax.set_ylabel("Retained heap after GC (MB)")
    ax.set_title("Android emulator memory footprint")
    ax.legend(frameon=False, fontsize=8)
    fig.tight_layout()
    fig.savefig(RESULTS / "fig_android_memory.png")
    plt.close(fig)


def main():
    rows = load()
    fig_read_time(rows)
    fig_memory(rows)
    fig_ops(rows)
    fig_speedup(rows)
    fig_android()
    fig_android_memory()
    print("figures written to", RESULTS)


if __name__ == "__main__":
    main()
