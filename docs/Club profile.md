# Club profile and badges

Welcome requires a team name (trimmed, nonempty, at most 20 characters) and one of 16 curated badges. Jersey abbreviation uses up to four alphanumeric characters with CLUB fallback for symbol-only names. The profile persists through Preferences DataStore on all targets, behind `TeamProfileRepository`. Existing Android SharedPreferences, iOS NSUserDefaults, and JVM Preferences are imported once without deleting the old data. The editor belongs to `feature/team` and uses `TeamViewModel`; see [Architecture refactor](Architecture%20refactor.md). Home exposes Edit. No logo creation or upload exists.

Both VS AI and local two-player mode keep the saved club on RED; BLUE uses a randomly named fictional club and an ID selected from the other 15 badges once per new match. The scoreboard displays both badges. Rematch selects a new opponent; it may repeat an earlier opponent, but never uses the current human badge.

Source PNG files in referensi_assets/logo remain intact. Runtime resources use WebP at 256×256, quality 82 and alpha quality 100 (112,670 bytes total), converted with cwebp. docs/club-logo-assets.json maps stable resource IDs to original filenames. Distinct IDs may have visually similar designs; current exclusion guarantees distinct selected assets.

Validation: Android build, JVM tests, and iOS simulator source compilation pass. Emulator checks cover empty-name Continue disabled, badge selection, profile save, reopening the app, and both badges/names appearing in a local match. iOS/JVM persistence and UI were compiled but not runtime-tested.
