#!/usr/bin/env python3
"""Build compact two-panel figures for the LaTeX report."""
from __future__ import annotations

import csv
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parent
FIG = ROOT / "figures"
RESULTS = ROOT.parent / "bench" / "results"
BASELINE = "#4C78A8"
NEW = "#E45756"
GRID = "#D9D9D9"


def setup():
    plt.rcParams.update(
        {
            "font.size": 10.5,
            "axes.titlesize": 11,
            "axes.labelsize": 10.5,
            "legend.fontsize": 9,
            "xtick.labelsize": 9.5,
            "ytick.labelsize": 9.5,
            "axes.grid": True,
            "grid.color": GRID,
            "grid.linewidth": 0.5,
            "axes.spines.top": False,
            "axes.spines.right": False,
            "figure.dpi": 120,
            "savefig.dpi": 240,
        }
    )


def load_summary():
    with (RESULTS / "summary.csv").open(encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def find(rows, impl, mode, n):
    for row in rows:
        if row["impl"] == impl and row["mode"] == mode and int(row["rows"]) == n:
            return row
    return None


def jvm_figure():
    rows = load_summary()
    sizes = [10_000, 25_000, 50_000, 100_000, 200_000]
    base_time, new_time, base_mem, new_mem = [], [], [], []
    for n in sizes:
        b = find(rows, "baseline", "read", n)
        o = find(rows, "new", "read", n)
        base_time.append(float(b["read_ms"]) / 1000 if b and b["status"] == "ok" else None)
        new_time.append(float(o["read_ms"]) / 1000 if o and o["status"] == "ok" else None)
        base_mem.append(float(b["used_mb"]) if b and b["status"] == "ok" else None)
        new_mem.append(float(o["used_mb"]) if o and o["status"] == "ok" else None)

    fig, axes = plt.subplots(1, 2, figsize=(6.6, 2.7))
    ax = axes[0]
    ax.plot([n / 1000 for n in sizes], base_time, marker="o", ms=3.5,
            color=BASELINE, lw=1.6, label="Baseline")
    ax.plot([n / 1000 for n in sizes], new_time, marker="o", ms=3.5,
            color=NEW, lw=1.6, label="Optimized")
    ax.axvline(200, color=BASELINE, ls=":", lw=0.9)
    ax.text(198, 0.18, "OOM", color=BASELINE, fontsize=7, ha="right")
    ax.set_xlabel("Rows (thousands)")
    ax.set_ylabel("Read time (s)")
    ax.set_title("(a) CSV read time")
    ax.legend(frameon=False)

    ax = axes[1]
    ax.plot([n / 1000 for n in sizes], base_mem, marker="o", ms=3.5,
            color=BASELINE, lw=1.6, label="Baseline")
    ax.plot([n / 1000 for n in sizes], new_mem, marker="o", ms=3.5,
            color=NEW, lw=1.6, label="Optimized")
    ax.set_xlabel("Rows (thousands)")
    ax.set_ylabel("Retained heap (MB)")
    ax.set_title("(b) Heap after GC")
    ax.legend(frameon=False)
    fig.tight_layout(w_pad=2.0)
    fig.savefig(FIG / "fig_report_jvm.png")
    plt.close(fig)


def parse_android(path):
    data = {"ok": [], "oom": []}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if not line.startswith("read rows="):
            continue
        parts = dict(part.split("=", 1) for part in line.split() if "=" in part)
        rows = int(parts["rows"])
        if parts.get("status") == "ok":
            data["ok"].append((rows, float(parts["ms"]) / 1000, float(parts["retainedMb"])))
        else:
            data["oom"].append((rows, float(parts["ms"]) / 1000))
    return data


def android_figure():
    new = parse_android(RESULTS / "android_new.log")
    base = parse_android(RESULTS / "android_baseline.log")
    fig, axes = plt.subplots(1, 2, figsize=(6.6, 2.7))

    ax = axes[0]
    ax.plot([r / 1000 for r, _, _ in base["ok"]], [t for _, t, _ in base["ok"]],
            marker="o", ms=3.5, color=BASELINE, lw=1.6, label="Baseline")
    ax.plot([r / 1000 for r, _, _ in new["ok"]], [t for _, t, _ in new["ok"]],
            marker="o", ms=3.5, color=NEW, lw=1.6, label="Optimized")
    for rows, seconds in base["oom"]:
        ax.axvline(rows / 1000, color=BASELINE, ls=":", lw=0.9)
        ax.text(rows / 1000 - 2, 14.5, f"OOM\n({seconds:.1f}s)", color=BASELINE,
                fontsize=6.5, ha="right")
    ax.set_ylim(0, 18)
    ax.set_xlabel("Rows (thousands)")
    ax.set_ylabel("Read time (s)")
    ax.set_title("(a) CSV read time")
    ax.legend(frameon=False, loc="upper left")

    ax = axes[1]
    ax.plot([r / 1000 for r, _, _ in base["ok"]], [m for _, _, m in base["ok"]],
            marker="o", ms=3.5, color=BASELINE, lw=1.6, label="Baseline")
    ax.plot([r / 1000 for r, _, _ in new["ok"]], [m for _, _, m in new["ok"]],
            marker="o", ms=3.5, color=NEW, lw=1.6, label="Optimized")
    ax.set_xlabel("Rows (thousands)")
    ax.set_ylabel("Retained heap (MB)")
    ax.set_title("(b) Heap after GC")
    ax.legend(frameon=False)
    fig.tight_layout(w_pad=2.0)
    fig.savefig(FIG / "fig_report_android.png")
    plt.close(fig)


if __name__ == "__main__":
    setup()
    jvm_figure()
    android_figure()
    print("report figures written")
