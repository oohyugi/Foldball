# Foldball bot

BotPlanner runs on isolated copies of GameEngine through Dispatchers.Default. Human input, timers and rendering remain on the UI thread. Local 2 Players and Sandbox never dispatch bot decisions.

## Difficulty

| Level | Thinking budget | Defense | Attack |
| --- | ---: | --- | --- |
| Easy | 120 ms | Up to one greedy legal move | Coarse candidates, random top-five selection and aim/power error |
| Medium | 250 ms | Up to two greedy legal moves | Direct-goal and wall-bank candidates, possession and field-position scoring |
| Hard | 650 ms | Beam search of legal two-move formations, tested against reply shots | Refines promising shots, then compares two candidates against legal defensive repositioning and the next kick |

The budget also leaves 300 ms of the phase clock for applying the action. It is a cooperative deadline, not a hard real-time guarantee: scheduling and garbage collection can overshoot. Search checks cancellation/time every 32 physics frames and discards incomplete simulations. On exhaustion it uses the best complete candidate, or a legal fallback if none completed. Animation delays also shrink when the live clock is almost exhausted.

All simulation uses the same 120 Hz physics as the match. `advanceSimulation` skips presentation snapshots, but shares the exact stepping implementation with `step`. A test compares full state across repeated rallies.

## Tactical search

Goal-lane angles and mirrored wall targets seed the search, followed by a sampled angle/power grid. Mirrored targets are approximate because wall rebounds lose energy; a candidate is scored only after the real physics simulation. A simulated goal is terminal and immediately selected for Medium/Hard.

Hard spends approximately 30% of its budget on initial candidates and 10% on local refinement. Remaining time is reserved for next-turn tactics. The next kicker approaches the ball, the defending side tries legal one/two-player repositioning, and the attacker tries a bounded reply set. Bot-controlled defense maximizes the bot's outcome; opposing defense minimizes it. The bot compares only complete tactical evaluations and needs two before replacing its immediate best choice.

Lookahead covers one reply and a small formation beam, not every legal future. It can still miss a human tactic. Scoring rewards goals and next possession, penalizes own goals and giving opponents the ball near the bot's goal, and values useful field position. No difficulty changes collision rules, scoring, power response or move limits.

## Profiling on Android

BotBenchmarkActivity exists only in `androidApp/src/debug`, with no entry point in release builds. It renders Sandbox while invoking the same planner on Dispatchers.Default. Samples cover opening/live states from both kickoff sides. Each level has a warm-up and three measured passes, producing 12 samples each for attack and defense. A short no-search frame window acts as a control; Choreographer intervals measure delivered UI frames, not isolated GPU cost or input latency.

```sh
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb logcat -c
adb shell am start -n com.yogi.foldball/.BotBenchmarkActivity --ei samples 3
# After FoldballBotBench logs DONE:
adb logcat -d -s FoldballBotBench:I '*:S' > /tmp/foldball-bot.log
python3 tools/summarize_bot_benchmark.py /tmp/foldball-bot.log
```

Results and limitations for the measured emulator are in [Bot Benchmark.md](Bot%20Benchmark.md).

## Rebound tuning

Walls retain 72% tangential speed (normal restitution remains 45%). Airborne landings also remove up to 18% planar speed, proportional to landing speed with saturation at 600 dp/s; gentle hops lose less. No contact-count goal limit is imposed. Bot shot evaluation now compares goal candidates rather than returning the first predicted goal, penalizes wall/player contacts by level (Easy 180, Medium 80, Hard 35), and penalizes repeats resembling the last three chosen angle/power pairs by 90 per repeat. Medium samples up to three candidates within 40 utility points of the best. Goal value stays dominant, so legal bank goals remain available. History only records completed live shot choices, not tactical reply searches.

## Live aiming execution

Bot tactics still predict ideal shots, but live execution interpolates aim over up to 1050 ms and charges via the same 1.8-second triangular power meter used by player gestures. It releases the current meter value instead of setting an ideal power instantly. Execution angle error is bounded at ±0.09/±0.045/±0.018 rad for Easy/Medium/Hard; power error comes from ±120/±90/±60 ms reaction jitter on the shared meter. Engine angle and power limits are unchanged; the live attack clock continues during planning, aiming, and charging. Pause/route changes cancel the bot coroutine.

## Tactical styles

Style is separate from difficulty and defaults to Balanced. Parkir Bus weights goal-lane coverage and deeper defense, safe possession, and lower-contact shots. Kick & Rush weights high power, forward progress, and defenders near the ball for the next contest. Total Football weights future possession, nearby support, defensive width, and ball-aware positioning. All retain the same legal zone moves, budgets, aim errors, clocks, and physics. These are turn-based adaptations, not simultaneous full-team movement. Goal utility remains dominant for every style.

## Human-like execution and uncertainty

Thinking tempo is separate from search: 1.1–2.6 s initial consideration (capped to leave 2.2 s), up to 1.05 s aim movement with small signed overshoot, and occasional consideration of another ranked candidate before correction. Defensive moves are separated by 400–850 ms, and non-Hard bots sometimes make only one planned move. All delays respect the live turn clock.

Direct power noise and Easy planner execution noise are removed. Player and AI use ShotPowerMeter; AI release reaction is ±120 ms Easy, ±90 ms Medium, ±60 ms Hard. A late release near full power can pass the peak and become weaker. Angle error remains level-dependent. Charging cancellation still applies on pause/exit.

Initial shot search is count-limited (30/58/44); Hard refinement stops at 62 simulations. Total simulation caps are 48/88/140 for Easy/Medium/Hard, with the existing wall-clock cutoff as a safety fallback. Top four available candidates receive three deterministic perturbation samples using the same angle and meter-reaction model. Only fully sampled candidates are compared; mean utility is penalized by 12% of outcome spread. If no sampling completes before cutoff, use the completed initial candidates. Search remains cancellable; tactical reply evaluation is retained for Hard.

Score lead mildly favors safe possession, while a deficit favors forward progress. Medium/Hard defense adds proximity-to-ball value alongside goal coverage. This estimates likely control from current position; it is not a new perfect prediction of every possible opposing shot.

## Power and shot loft

Launch vertical speed now follows power^5 × 3200 dp/s instead of a fixed 120 dp/s baseline plus a mild cubic term. Low-power touches remain near the ground; full power produces substantial loft. Goal checks interpolate height at the exact swept goal-line crossing and keep the 36 dp opening-height limit. The same physics and uncertainty sampling applies to human and AI shots. No distance-based automatic miss or special AI exception is used.

GoalGeometry now owns the 36 dp opening height and rendering projection. Goal clearance requires the entire ball below the crossbar (lift + diameter < opening height). Vertical overlap reflects off the crossbar and emits the existing post sound/event; a ball fully above the opening is not a goal or post contact.
