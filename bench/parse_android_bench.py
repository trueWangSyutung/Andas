#!/usr/bin/env python3
"""Parse `ANDAS_BENCH` logcat lines into CSV."""
import csv
import re
import sys
from pathlib import Path

log = Path(sys.argv[1])
out = Path(sys.argv[2]) if len(sys.argv) > 2 else log.with_suffix(".csv")
rows = []
meta = {}
for line in log.read_text(errors="ignore").splitlines():
    if "ANDAS_BENCH" not in line:
        continue
    payload = line.split("ANDAS_BENCH", 1)[1].lstrip(": ").strip()
    if payload.startswith("device="):
        for key, value in re.findall(r"(\w+)=([^ ]+)", payload):
            meta[key] = value
    elif payload.startswith("read "):
        fields = dict(re.findall(r"(\w+)=([^ ]+)", payload))
        shape_match = re.search(r"shape=(\([^)]*\))", payload)
        rows.append({
            "model": meta.get("device", ""),
            "sdk": meta.get("sdk", ""),
            "max_heap_mb": meta.get("maxHeapMb", ""),
            "rows": fields.get("rows", ""),
            "ms": fields.get("ms", ""),
            "retained_mb": fields.get("retainedMb", ""),
            "status": fields.get("status", ""),
            "shape": shape_match.group(1) if shape_match else "",
        })

out.parent.mkdir(parents=True, exist_ok=True)
with out.open("w", newline="", encoding="utf-8") as handle:
    writer = csv.DictWriter(handle, fieldnames=list(rows[0]) if rows else ["model"])
    writer.writeheader()
    writer.writerows(rows)
print(f"parsed {len(rows)} rows -> {out}")
