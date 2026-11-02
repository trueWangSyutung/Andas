#!/usr/bin/env python3
"""Sync repository documentation into the GitBook source tree.

Single source of truth:
  doc/                      -> site/docs/guide/
  QUICK_START.md            -> site/docs/quickstart.md
  API_REFERENCE.md          -> site/docs/api.md
  changelog/CHANGELOG_0.2.1.md -> site/docs/changelog.md
  BENCHMARK_REPORT.md       -> site/docs/benchmark/report.md
  bench/README.md           -> site/docs/benchmark/reproduce.md
  report/*.pdf|*.tex        -> site/docs/assets/
  bench/results/fig_*.png   -> site/docs/assets/images/
  bench/results/*.csv       -> site/docs/assets/data/
"""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SITE = ROOT / "site"
DOCS = SITE / "docs"
GUIDE = DOCS / "guide"
BENCH = DOCS / "benchmark"
ASSETS = DOCS / "assets"
IMAGES = ASSETS / "images"
DATA = ASSETS / "data"


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def copy(path: Path, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, target)


def sync_guide() -> None:
    chapters = {
        "charter1/init.md": "init.md",
        "charter2/series.md": "series.md",
        "charter3/dataframe.md": "dataframe.md",
        "charter4/async.md": "async.md",
        "charter5/jni.md": "jni.md",
        "charter6/performance.md": "performance.md",
        "numpy/ndarray.md": "ndarray.md",
        "scipy/stats.md": "scipy.md",
        "plot/plotting.md": "plotting.md",
        "annotation/extensions.md": "extensions.md",
        "demo/showcase.md": "demo.md",
    }
    for source, target in chapters.items():
        text = (ROOT / "doc" / source).read_text(encoding="utf-8")
        text = text.replace("(images/", "(../assets/images/demo/")
        # Normalize the first heading to H1 so the page title is consistent.
        lines = text.splitlines()
        for index, line in enumerate(lines):
            if line.startswith("## "):
                lines[index] = "# " + line[3:]
                break
        text = "\n".join(lines) + "\n"
        write(GUIDE / target, text)


def sync_flat_docs() -> None:
    quick = (ROOT / "QUICK_START.md").read_text(encoding="utf-8")
    quick = quick.replace("(API_REFERENCE.md)", "(api.md)").replace("(README.md)", "(README.md)")
    write(DOCS / "quickstart.md", quick)

    copy(ROOT / "API_REFERENCE.md", DOCS / "api.md")
    changelog = (ROOT / "changelog/CHANGELOG_0.2.1.md").read_text(encoding="utf-8")
    changelog = changelog.replace("(../bench/NUMPY_SCIPY_BENCHMARK.md)", "(benchmark/numeric.md)")
    changelog = changelog.replace("(../report/andas-technical-report.pdf)", "(assets/andas-technical-report.pdf)")
    (DOCS / "changelog.md").write_text(changelog, encoding="utf-8")


def sync_benchmark() -> None:
    report = (ROOT / "BENCHMARK_REPORT.md").read_text(encoding="utf-8")
    report = report.replace("(bench/NUMPY_SCIPY_BENCHMARK.md)", "(numeric.md)")
    report = report.replace("bench/results/", "../assets/data/")
    report = report.replace("](bench/README.md)", "](reproduce.md)")
    report = report.replace("`bench/results/`", "`../assets/data/`")
    # Images live under assets/images, not assets/data.
    report = report.replace("../assets/data/fig_", "../assets/images/fig_")
    write(BENCH / "report.md", report)
    copy(ROOT / "bench/NUMPY_SCIPY_BENCHMARK.md", BENCH / "numeric.md")

    reproduce = (ROOT / "bench/README.md").read_text(encoding="utf-8")
    reproduce = reproduce.replace("(../BENCHMARK_REPORT.md)", "(report.md)")
    reproduce = reproduce.replace("bench/results/", "../assets/data/")
    reproduce = reproduce.replace("../report/andas-technical-report.pdf", "../assets/andas-technical-report.pdf")
    write(BENCH / "reproduce.md", reproduce)

    for png in (ROOT / "bench/results").glob("fig_*.png"):
        copy(png, IMAGES / png.name)
    for png in (ROOT / "report/figures").glob("fig_report_*.png"):
        copy(png, IMAGES / png.name)
    for csv in (ROOT / "bench/results").glob("*.csv"):
        copy(csv, DATA / csv.name)
    copy(ROOT / "bench/results/raw.json", DATA / "raw.json")
    for name in [
        "consistency_pandas.json",
        "consistency_jvm.json",
        "consistency_summary.json",
        "android_consistency.log",
    ]:
        source = ROOT / "bench/results" / name
        if source.exists():
            copy(source, DATA / name)
    copy(ROOT / "bench/results/android_new.log", DATA / "android_new.log")
    copy(ROOT / "bench/results/android_baseline.log", DATA / "android_baseline.log")

    for image in (ROOT / "doc/demo/images").glob("*.png"):
        copy(image, IMAGES / "demo" / image.name)
    copy(ROOT / "report/andas-technical-report.pdf", ASSETS / "andas-technical-report.pdf")
    copy(ROOT / "report/andas-technical-report.tex", ASSETS / "andas-technical-report.tex")


def main() -> None:
    sync_guide()
    sync_flat_docs()
    sync_benchmark()
    print("site docs synced")


if __name__ == "__main__":
    main()
