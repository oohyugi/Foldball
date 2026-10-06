# Match sound effects

Original recordings remain in `referensi_assets/sound`. Bundled derivatives in `shared/src/commonMain/composeResources/files/audio` are mono, 48 kHz, 16-bit PCM WAV (approximately 250 KiB total).

| Event | Source | Source trim (seconds) | Runtime volume |
| --- | --- | --- | --- |
| Kick | freesound_community-soccer-kick-6235.mp3 | 0.065–0.745 | 0.45–0.70 with shot power |
| Floor bounce | soccer-ball-bounce-catch-smartsound-fx-1-00-00.mp3 | 0.09–0.51 | 0.09–0.20 with landing strength |
| Player / wall impact | tunetank.com_impact-cardboard-tube-003.wav | 0.28–0.51 | 0.25–0.50 with impact strength |
| Post | Echoto-Sound-Sportsmanship-Football-Hits-Post.mp3 | 0–0.44 | 0.60 |
| Goal / net | Stuart-Duffield-Soccer-Ball-Hitting-Net.mp3 | 0–0.83 | 0.70 |

Each trim was converted to mono, peak-normalized to -3 dBFS, and given a 2 ms fade-in and 20 ms fade-out. Quiet internal texture is retained. Engine counters distinguish discrete contacts from decaying visual strength. Simulation copies never play audio. Goals take precedence over posts, impacts, and landings in the same rendered update. Repeated contacts have an 80 ms cooldown, landings 120 ms, goals 400 ms. Only the latest contact of each type is played when several physics steps occur before rendering.

The shared pause-menu Sound effects toggle persists across navigation for the current app session. Mute, pause, leaving a match, and backgrounding stop active playback. Files load once at app root. Android uses SoundPool (six simultaneous streams); desktop and iOS preload two voices per clip. Sound failures do not block gameplay.

Debug Android playback check: launch `com.yogi.foldball/.SoundCheckActivity` and inspect the `FoldballAudio` log tag. All five native loads must report status 0 and every PLAY must report a nonzero stream. This confirms native decoding and playback acceptance; listening on a physical device remains the check for perceived balance.

## Validation (2026-10-05)

`./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` succeeded. All 35 JVM tests passed, including two dispatcher tests covering single delivery, muted event consumption, goal/post priority, and contact cooldown. On emulator-5554, all five SoundPool loads returned status 0, kick/bounce/impact/post/goal returned stream IDs 1/2/3/4/5, and the runner reached CHECK_COMPLETE. iOS was compiled; iOS and desktop audible playback were not device-tested.

## Opening paper crumple

`crumple.wav` derives from `tunetank.com_paper-crumple.wav`: trim the first 35 ms, convert to mono PCM 48 kHz, slow with atempo=0.6 to approximately 1.2 seconds to match the opening animation, attenuate 1.1 dB, apply short edge fades. It plays once at volume 0.55 when the paper starts crumpling, respecting mute and foreground state. The clip loads first; opening waits for loading to finish, including Android asynchronous decoding. The debug sound check also includes this clip.

## Defeat against AI

`defeat.wav` uses `Omnibit-Sound-Cartoon-Nonsense-Aww-Audience-Disappointed.mp3`, trimmed from 0.10 to 2.23 seconds with 10 ms/50 ms edge fades, mono 48 kHz PCM. Volume is 0.55. It plays once when MATCH_OVER appears and the BLUE AI wins a full VS AI match. It does not play for a lost goal, a human win, local two-player games, or Sandbox. The existing mute, background, and match-exit behavior applies.

## Goal crowd celebration

`cheer.wav` is a 2.5 second mono PCM 48 kHz excerpt from `Airborne-Sound-Crowd-Cheers-and-Applause-Cheering-Horn-Blowing-and-Loud-Clapping.mp3`, with +3 dB source gain, a 25 ms fade-in, and 550 ms fade-out. The existing net sound remains at goal contact. Cheering follows 180 ms later at volume 0.40, for either team. Pause/exit cancels the delayed effect; mute/background/stop invalidate pending playback even if audio is subsequently re-enabled. Muted goals do not schedule cheering.

## Final whistle

`whistle.wav` uses `REFEREE WHISTLE SOUND EFFECT - Venexxus.mp3`, preserving its two whistle blasts and internal pause while trimming leading/trailing silence (source 1.27–3.20 s). Mono 48 kHz PCM with 5 ms/40 ms fades, volume 0.55. Plays once when a full match reaches MATCH_OVER in VS AI and local two-player modes. Remaining crowd playback stops first. If the human lost against AI, the disappointed audience follows after 2050 ms; stop/mute/background invalidates that pending sound. Leaving or restarting the match cancels the sequence.

## Kickoff whistle

`kickoff.wav` uses the Fox Forty single-blow referee reference, trimmed from 0.09–1.25 seconds to remove leading/trailing silence while keeping the full blow, mono 48 kHz PCM with 5 ms/40 ms fades. Plays at volume 0.50 after the opening toss selects the team and again at kickoff after a goal, for VS AI and local matches. A per-match score marker prevents repeats on pause/resume, recomposition, or engine recreation. It does not play on ordinary possession changes or Sandbox. Mute and background rules apply.

## Victory against AI

`victory.wav` derives from `u_xg7ssi08yr-crowd-cheering-379666.mp3`, source 0.78–4.28 seconds, mono 48 kHz PCM. Source attenuation of 6 dB balances the louder recording; 60 ms fade-in and 700 ms fade-out soften its edges. The 3.5 second clip plays at volume 0.50 after the final whistle (2050 ms delay), only when the RED human wins against the BLUE AI. Local matches keep only the final whistle. Existing muted/background/stopped sequence cancellation applies. Goal cheering remains unchanged.

## Time warnings

`timeout.wav` uses `freesound_community-timeout-90320.mp3`, mono 48 kHz PCM, short edge fades, volume 0.35. Human formation/aim phases cue at 3/2/1 seconds; bot turns do not cue. Full match cues once at 10 seconds and at 3/2/1. Concurrent thresholds produce one sound, with both consumed. Muted/background thresholds are consumed without later replay. Pausing stops frame updates and audio; resume does not duplicate thresholds. End-of-match whistle stays separate after the last shot.
