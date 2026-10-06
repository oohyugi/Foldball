# Production state and team feature

The app stays in the existing `shared` module. Production state now follows the architecture guideline:

- `app/navigation/AppViewModel` owns the typed route, retained profile, chosen AI setup and navigation entry lifecycle. Home receives that state and emits callbacks; its difficulty dialog is temporary UI state.
- `feature/team/presentation/TeamSetupRoute` obtains `TeamViewModel` from Koin. Its immutable `TeamUiState` exposes loading, editable fields, saving and recoverable errors. All edits enter through `onIntent`; a buffered effect is emitted only after saving succeeds. The stateless screen uses shared Foldball components and spacing.
- `domain/team` owns profile validation, curated badge IDs, fictional opponent selection and team identities. Match consumes a mapped identity; team customization does not belong in the match screen.
- `domain/repository/TeamProfileRepository` exposes the profile flow and suspend save. `data/repository/DataStoreTeamProfileRepository` implements transactional storage. No screen accesses platform preferences or repositories.
- `feature/match/presentation/viewmodel/MatchViewModel` owns the live engine, match clock, pause, kickoff, rematch and opponent selection. Input commits use `MatchIntent`; physics formulas stay in `GameEngine`. The UI supplies frame time, reads immutable render snapshots and owns animation/gesture state. Bot searches receive isolated copies through ViewModel planning methods; cosmetic aiming and charge animation stay in presentation.
- App navigation clears the previous feature ViewModel store. The store is retained by `AppViewModel` across an Activity recreation and cleared when its owner ends. Each rematch resets per-session animation state through `sessionId`.
- Koin starts from the Android Application, iOS controller factory and JVM main function, before composing. Recomposition does not start another graph. IDE previews render pure screen components without accessing storage.

## Persistence and migration

One Preferences DataStore instance is created by the application DI graph. Name, short name and badge are written in a single edit. Platform paths are private Android files, Application Support on iOS, and Application Support/Foldball on macOS (`.foldball` on other JVM systems).

The first read imports the existing Android `club_profile` SharedPreferences, iOS `club_*` NSUserDefaults, or JVM `com/yogi/foldball/club` Preferences. The migration marker is stored atomically with the imported fields. An existing DataStore profile wins. Legacy data remains intact for rollback; subsequent edits never read from it again. Read failures show Retry rather than opening an empty form that could overwrite a profile. Save failures preserve the draft.

`MatchTimingConfig.matchSeconds` supplies the total duration (180 seconds by default). Toss, pause and turn announcements block the clock; an in-flight shot still completes through the existing engine expiry rule. Physics parameters and field geometry are unchanged by this architecture refactor.

## Repository checks

```sh
./gradlew formatKotlin
./gradlew checkKotlinStyle checkArchitecture
./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlin
```

The formatter covers Kotlin application/test/build sources, excluding generated files and reference assets. `.editorconfig` uses Android Studio style and preserves the established Compose/token naming conventions. `tools/check_architecture.py` checks import boundaries for domain, data, screen and reusable GameKit layers. Developer labs keep local preview animation controls, as permitted by the guideline.

Validation includes profile import/reopening, existing-data precedence, load/save failures, cancellation, duplicate-save prevention, feature cleanup, pause, toss, turn blocking, rematch, duration expiry and engine movement/Undo commands. iOS persistence is source-compiled, not device-tested.
