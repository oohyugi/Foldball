# Bot benchmark — 5 October 2026

Measured on emulator-5554, sdk_gphone64_arm64, Android API 36, arm64, debug APK. Physics/planning dimensions are 360 × 580 dp. Three measured passes over four scenarios per level produce 12 samples per decision type; warm-up is excluded. These are emulator results on one host, not a claim about physical phones or production release performance.

Baseline used the preceding planner (650 ms shared limit, snapshots each physics frame, fixed-formation reply assumptions). Current uses per-level limits, snapshot-free stepping, and a smaller tactical comparison pool with defensive repositioning. Search quality and time allocation changed together: this is an end-to-end decision comparison, not an isolated microbenchmark of the snapshot optimization. Inputs, physics and scenario generation are unchanged.

| Level | Decision | n | Before median (ms) | After median (ms) | After p95 (ms) | After max (ms) |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Easy | shot | 12 | 167.4 | 122.0 | 136.2 | 136.2 |
| Easy | defense | 12 | 11.4 | 11.7 | 17.5 | 17.5 |
| Medium | shot | 12 | 382.1 | 252.0 | 265.2 | 265.2 |
| Medium | defense | 12 | 12.9 | 13.1 | 21.0 | 21.0 |
| Hard | shot | 12 | 652.6 | 520.2 | 630.6 | 630.6 |
| Hard | defense | 12 | 562.9 | 256.8 | 307.3 | 307.3 |

CONTROL_FRAME: n=36, median=66.7 ms, p95=183.3 ms, max=233.3 ms

FRAME: n=303, median=66.7 ms, p95=100.0 ms, max=183.3 ms

Hard completed at least two tactical candidates in 11/12 shot samples.


The final run completed two tactical candidate comparisons on 11/12 Hard shot samples; the other sample selected a simulated goal early. The timing maxima illustrate that cooperative deadlines can overshoot on scheduling/GC; the final maxima were 136.2/265.2/630.6 ms for Easy/Medium/Hard shots. The app also limits thinking by the remaining phase time and verifies phase/kicker identity before applying a shot.

Frame intervals are already slow without search: median 66.7 ms in both the short control window and the search window. Search-window p95 was 100 ms versus 183.3 ms in the control, but the windows differ in duration and the Sandbox animation changes over time. This does not establish causation or prove smooth 60 FPS. Background execution and cancellation are verified; frame smoothness and low-end physical phones remain unverified.

## Evidence and reproduction

- [Baseline log](benchmarks/2026-10-05-baseline.txt)
- [Current log](benchmarks/2026-10-05-current.txt)
- [Runner and algorithm](Bot%20AI.md)
- Summarizer: `python3 tools/summarize_bot_benchmark.py docs/benchmarks/2026-10-05-current.txt --baseline docs/benchmarks/2026-10-05-baseline.txt`
- `:shared:jvmTest`: 33 tests passed, including full-rally equality between `step` and `advanceSimulation`, cancellation, short-budget fallback, complete tactical comparisons, movement legality, scoring and existing physics regressions.
- `:shared:compileAndroidMain` and `:androidApp:assembleDebug`: passed.

- Local 2 Players UI verified on emulator at 360 dp: THAI defense → INDO attack, readable formation/aim/hold/release instructions, manual Siap action, and identical Green Paper Field bounds across the phase change. Screenshots inspected for clipping and touch-control placement.
