#!/usr/bin/env python3
"""Summarize debug FoldballBotBench logcat samples; standard library only."""
import argparse
import math
import re
import statistics
from pathlib import Path


def summarize(path):
    log = Path(path).read_text()
    if "DONE frames=" not in log:
        raise ValueError(f"Incomplete benchmark: {path}")
    result = {}
    for level in ("EASY", "MEDIUM", "HARD"):
        for kind in ("shot", "defense"):
            values = [float(x) for x in re.findall(
                rf"SAMPLE,{level},{kind},[^,]+,\d+,([\d.]+)", log)]
            if not values:
                raise ValueError(f"Missing {level} {kind} samples")
            ordered = sorted(values)
            result[(level, kind)] = (len(values), statistics.median(values),
                                    ordered[math.ceil(.95 * len(values)) - 1], max(values))
    return log, result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("current")
    parser.add_argument("--baseline")
    args = parser.parse_args()
    log, current = summarize(args.current)
    baseline = summarize(args.baseline)[1] if args.baseline else None
    print("| Level | Decision | n | Before median (ms) | After median (ms) | After p95 (ms) | After max (ms) |")
    print("| --- | --- | ---: | ---: | ---: | ---: | ---: |")
    for (level, kind), (count, median, p95, maximum) in current.items():
        before = f"{baseline[(level, kind)][1]:.1f}" if baseline else "—"
        print(f"| {level.title()} | {kind} | {count} | {before} | {median:.1f} | {p95:.1f} | {maximum:.1f} |")
    for kind in ("CONTROL_FRAME", "FRAME"):
        values = sorted(float(x) for x in re.findall(rf": {kind},([\d.]+)", log))
        if values:
            print(f"\n{kind}: n={len(values)}, median={statistics.median(values):.1f} ms, "
                  f"p95={values[math.ceil(.95 * len(values)) - 1]:.1f} ms, max={max(values):.1f} ms")
    tactical = re.findall(r"METRIC,HARD,shot,[^,]+,\d+,\d+,\d+,(\d+),(?:true|false)", log)
    if tactical:
        print(f"\nHard completed at least two tactical candidates in {sum(int(x) >= 2 for x in tactical)}/{len(tactical)} shot samples.")


if __name__ == "__main__":
    main()
