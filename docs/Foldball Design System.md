# Foldball Design System

This document is the source of truth for Foldball's visual language and shared UI. Read it before proposing or implementing any UI change. The design system lives in the existing `:shared` module; there is no separate `core:designsystem` Gradle module.

## 1. Art Direction

Foldball should feel like a paper football game played on a school desk:

- Warm wood is the background surface.
- Torn paper is used for cards, buttons, score panels, and dialogs.
- Dark ink carries text, outlines, and icon strokes.
- Yellow tape highlights important labels and the current selection.
- Cardboard and green paper support the arena materials.
- Small changes in angle and a firm downward shadow make paper elements feel placed on a surface.

Keep gameplay readable. Texture, grain, rotation, and decoration must not obscure scores, timers, instructions, or touch targets. Styling work must preserve field geometry, gesture areas, physics, and player hitboxes.

## 2. Source Locations

All shared UI is implemented in `shared`:

| Responsibility | Location |
| --- | --- |
| Color, typography, spacing, and Material theme | `shared/src/commonMain/kotlin/com/yogi/foldball/core/ui/theme/FoldballTokens.kt` |
| Paper shapes and reusable UI components | `shared/src/commonMain/kotlin/com/yogi/foldball/core/ui/theme/FoldballComponents.kt` |
| Bundled fonts | `shared/src/commonMain/composeResources/font/` |
| Runtime images and vectors | `shared/src/commonMain/composeResources/drawable/` |
| Shared ball, field, and player rendering | `shared/src/commonMain/kotlin/com/yogi/foldball/gamekit/presentation/` |
| Match-specific UI composition | `shared/src/commonMain/kotlin/com/yogi/foldball/feature/match/presentation/` |

Keep original/source material in `referensi_assets/`. Copy only assets used at runtime into Compose Resources. The Markdown file is the authoritative guideline; the HTML file is a visual companion.

## 3. Foundations

### Color

Use `FoldballColor` instead of defining screen-local equivalents.

| Token | Hex | Intended use |
| --- | --- | --- |
| `Wood` | `#9A6A3C` | Main desk surface |
| `WoodDark` | `#6F4524` | Wood grain and dark material details |
| `Paper` | `#F6F1E4` | Paper surfaces and light text on ink |
| `PaperLine` | `#C9D4EA` | Ruled paper lines |
| `Ink` | `#1C1C22` | Text, outlines, and dark controls |
| `InkBlue` | `#1F4FC4` | Primary action and blue team |
| `TeamRed` | `#D64545` | Red team and danger actions |
| `Tape` | `#F4D34A` | Emphasis, focus, and selection |
| `Grass` | `#3F8F43` | Green paper/field accents |
| `Cardboard` | `#C99A62` | Arena walls and cardboard details |
| `InkMuted` | `#777A80` | Secondary technical labels |
| `FieldPaper` | `#E8E1D4` | Neutral paper arena used by visual labs |
| `FieldLine` | `#C7BFAF` | Arena linework |
| `FieldHighlight` | `#F1EBE0` | Light arena highlights |
| `BallHighlight` | `#F9F6F0` | Ball and field highlight details |
| `ShadowBrown` | `#51483D` | Contact shadows in presentation previews |
| `ShadowGrass` | `#0B2A0E` | Projected player silhouettes on the field |
| `Shadow` | `#352213` | Paper shadow base color; opacity comes from `FoldballShadow` |
| `ShadowBlue` | `#173C94` | Primary action shadow |

Add a semantic token when a recurring material or state cannot be represented by these values. Record it here and in `FoldballTokens.kt` together.

### Typography

Use the local Compose Resource fonts. Do not fetch fonts at runtime or use an Android-only Google Fonts provider in `commonMain`.

| Style | Typeface | Size and treatment | Use |
| --- | --- | --- | --- |
| Display | Permanent Marker | 58sp, letter spacing -1sp | Product name and opening hero |
| Large title | Permanent Marker | 40sp | Major celebration or title |
| Title | Permanent Marker | 26sp | Primary actions and panel headings |
| Label | Patrick Hand | 20sp, uppercase, about 0.06em tracking | Team names and section labels |
| Body | Patrick Hand | 17sp, 24sp line height | Instructions and dialog copy |
| Caption | Patrick Hand | 14sp, uppercase, about 0.08em tracking | Short metadata and secondary labels |

Use `FoldballType`. Smaller variants may be used in the match HUD when space is constrained, while preserving the same font family and readable contrast.

### Spacing, rotation, and shadows

Use `FoldballSpace` for shared spacing: `xs` 4dp, `sm` 8dp, `md` 12dp, `lg` 16dp, and `xl` 24dp. Use `FoldballTilt` for recurring paper rotations and `FoldballShadow` for paper shadow offset, opacity, and colors. A local size is acceptable for a one-off alignment detail; recurring values belong in tokens.

- Banner tilt: -1.6 degrees.
- Menu card tilts, reading left to right and top to bottom: +1.0, -1.2, -0.8, +1.3 degrees.
- Paper shadow: `FoldballShadow.offsetY` (4dp) and `FoldballShadow.paperOpacity` (45%), with no large blur.
- Primary action shadow: `FoldballShadow.offsetY` (4dp) and `FoldballShadow.primaryColor`, with no blur.
- Pressed state: scale to 0.97 over 120ms.
- Interactive target: at least 48dp in both dimensions. A smaller icon may sit inside a larger target.

Centralize recurring tilt and shadow values rather than scattering magic numbers across screens.

### Torn paper shapes

Use `FoldballShapes.Card`, `Button`, `Chip`, and `Banner` for Foldball paper surfaces. They are normalized polygons and should scale with their component. Do not substitute rounded Material surfaces for paper components when an existing Foldball component fits.

Cards and banners must have a small, non-zero tilt. Keep the rotation modest enough to avoid clipped content or reduced touch targets.

## 4. Shared Components

Use the shared components before writing screen-local `Box`, `clip`, shadow, or click behavior.

| Component | Purpose and usage |
| --- | --- |
| `FoldballTheme` | Applies the light Material color scheme and Foldball theme to the app. |
| `FoldballScaffold` | Draws the wood gradient and grain and provides the common screen content frame. Home and Sandbox receive safe drawing insets from the app root. The full-screen Match route can draw behind transparent system bars and applies insets to its interactive content. Do not apply a second inset inside nested screens. |
| `PaperCard` | Torn-paper base for cards, banners, and dialog surfaces. Set a non-zero tilt. Use ruled lines for notebook-like content and disable them for clean display surfaces. |
| `PaperButton` | Primary, secondary, danger, and disabled actions. Primary actions use the hard blue shadow. Use `compact = true` for dense bars; it keeps a 48dp minimum touch target with a 20sp label and reduced horizontal padding. |
| `PaperIconButton` | Icon-only action with an accessible description and a minimum 48dp target. |
| `PaperChip` | Text action or selection chip. Provide a useful content description and selection state. |
| `TapeLabel` | Short emphasis label on yellow tape. Use sparingly. Use `compact = true` for narrow controls such as the power meter label so text stays on one line. |
| `MenuCard` | Large menu tile with a title and optional art. Locked cards are disabled, described as locked to assistive technology, and covered by a dark visual overlay. |
| `PaperToggle` | Settings switch styled as a hand-drawn checkbox. Keep the full row interactive. |
| `CirclePickerItem` | Team or ball picker item. Show selected state with the blue hand-drawn oval and expose selection semantics. |
| `PaperDialog` | Confirmation and result dialog on a torn banner. Optional content can provide shared selection controls between the title and message. Use secondary action for cancel and danger style for destructive confirmation. |
| `InkToast` | Short-lived feedback on an ink-colored chip. `FoldballSnackbarHost` renders Material `SnackbarHost` messages with this component at the app root. UI can use `LocalFoldballSnackbarHostState.current.showSnackbar(...)` to send a message. |

`PressableSurface` is the shared base for interactive paper surfaces. Keep press scale, clipping, shadow, enabled state, role, and semantics there so controls behave consistently.

## 5. Screen and Match Patterns

- **Home:** Use `FoldballScaffold`, the display type, and a clear primary `PaperButton`. Keep the primary action visually strongest.
- **Menu grids:** Use `MenuCard` with the documented alternating tilt sequence. Do not invent a new card treatment for each tile.
- **Match HUD:** Compose `MatchScoreboard`, `MatchStatusPanel`, and `PowerTimingMeter` from shared tokens and paper components. Show team identity with labels as well as color. Keep the score pair centered, with each player/team name below its score. Show the red human player on the left and the blue opponent (AI in VS AI) on the right.
- **VS AI:** Play controls the red team; the blue bot uses the same formation and kick rules. Disable formation actions and player gestures during bot turns, and label bot aiming explicitly. Sandbox remains manual. Tapping Play vs AI opens a PaperDialog to select Easy, Medium, or Hard using equal-width PaperChips with selection semantics and a short description. Play confirms entry; Cancel dismisses it. Default to Medium and retain the choice when returning home.
- **Local 2 Players:** Home exposes a secondary Play 2 Players button below Play vs AI. Both teams use the existing formation and aiming controls on one device; keep match timers, scoring, pause, and restart behavior consistent. AI difficulty applies only to Play vs AI. The existing tape label explicitly shows the team name plus TURN and DEFEND or KICK, with a polite accessibility live region. Use English for match labels and instructions, including local two-player handoffs. Keep brief move/aim/hold/release instructions inside the existing bottom control height; do not resize the arena for handoff guidance.
- **Match turn indicators:** Tint the countdown team label with that team's color. Keep bottom actions content-sized and distribute them across the bar; use the compact button variant when space is limited.
- **Match team emphasis:** Fade the non-turn team smoothly to 68% during formation. Keep both teams fully visible during aiming, kicker approach, and ball movement. Keep the fade subtle enough to preserve jersey and position readability.
- **Match haptics:** Play one subtle platform haptic when entering a formation or aiming phase, when a player move is accepted, when a kick is released, and when a goal is scored. Do not repeat haptics during timer or animation frames.
- **Match formation and kick states:** During opening kickoff formation, label the defending team and show that movement stays in its own half. Highlight movable players with a pale dashed base marker; selecting one player shows neighboring zones with a reachable, collision-safe drag path in a subtle team-color fill and bright dashed outline, and blocked neighboring zones with a subtle gray fill and outline. Dragging into a highlighted zone may settle on its safe edge when the center is obstructed. Leave non-neighboring zones unmarked and draw no connector lines. During later formation turns, use the same marker and zone treatment. Reserve the yellow tape focus marker for the active kicker.
- **Gameplay shadows:** Keep player cast shadows anchored to the standee's bottom edge so the player reads as touching the field. Place the ball contact shadow immediately below the ball; let it become wider and softer as the ball rises, while the contact mark fades.
- **Match arena sizing:** Keep field width and height independent of phase-specific labels, status panels, and actions. Reserve consistent space for match controls so changing a bottom action never resizes the field or resets its coordinate system.
- **Opening toss and goal celebration:** Use a torn `PaperCard` or `Banner` and the same team colors and type scale as the HUD. Toss motion follows constant-gravity ballistic arcs with energy-losing ground rebounds, then a friction-limited roll. Use the match gravity scale and ground restitution; keep the shared match ball size. Ground travel stays straight, and the ground shadow broadens and fades with height.
- **Arena:** Preserve the classroom desk, paper field, and cardboard materials. Use shared material tokens for newly drawn colors; do not change playable bounds to make a visual treatment fit.
- **Dialogs and feedback:** Use `PaperDialog` for confirmation/results and the shared ink toast treatment for transient feedback.
- **Settings and selection:** Use `PaperToggle` and `CirclePickerItem` when those flows are introduced.
- **Developer labs:** They are reachable through Sandbox today. Keep their controls aligned with this system if they remain accessible from the player-facing app.

A component receives display data, accessibility text, and callbacks from its owner. It must not decide gameplay rules or navigation destinations.

## 6. Accessibility and Layout

- Give every interactive control a minimum 48dp hit area, even when the visible icon is smaller.
- Supply meaningful content descriptions for icon-only actions.
- Expose selected, disabled, and locked states through semantics as well as appearance.
- Do not communicate team or action state by color alone; include text or an accessible label.
- Keep text at the defined sizes where space allows. When a compact HUD needs smaller type, preserve the bundled face and verify legibility at narrow portrait widths.
- Keep Home and Sandbox status-bar/navigation-bar insets at the app root. The full-screen Match route may draw behind transparent bars and must inset interactive content at its route root. Avoid double inset padding in nested screens.
- Decorative grain and ruled lines stay behind content and must not reduce text contrast.

## 7. UI Implementation Workflow

For every task that creates or changes UI:

1. Read this guide before editing UI source.
2. Identify the existing token or component that matches the requested surface or interaction.
3. Reuse it and keep screen code focused on composition, state display, and callbacks.
4. If the design calls for a new reusable rule, update this guide and the shared component/token implementation in the same change.
5. Review affected screens at narrow portrait dimensions and check touch bounds, text clipping, rotation, and contrast.
6. Preserve gameplay geometry, gesture handling, and engine behavior while making visual changes.

## 8. Theme Setup

The product uses a single light visual mode. The desk-and-paper materials define the game's identity, so do not add a dark theme without an explicit product decision.

`FoldballType` loads `Permanent Marker` and `Patrick Hand` from `composeResources/font/`. `FoldballTheme` provides the Material baseline; Foldball-specific controls should use the tokens and components above instead of relying on Material defaults.

### Match sound effects

Use the shared PaperToggle with a minimum 48 dp target for Sound effects in the pause menu. The choice persists while navigating within the app session. Sound follows discrete engine events, never visual animation frames. Pause, mute, and leaving the match stop active sounds. Cardboard impact accompanies players and walls; separate kick, floor bounce, post, and net clips use balanced volumes and short cooldowns. Original reference recordings stay unchanged; runtime assets are trimmed mono PCM WAV.

- For confirmation dialogs with long action labels, use `PaperDialog(stackedActions = true)` to keep both actions fully visible and preserve touch targets. Stack the secondary cancel action above the primary/danger confirmation action; each uses the available content width. Return Home uses this layout.

- AI aiming uses the same aim indicator and PowerTimingMeter as human aiming. Aim moves continuously before charging; the power meter remains visible while the bot charges. Pause cancels bot charging without an automatic release.

- Match scoreboard places the RED bottom-side player on the left and BLUE opponent on the right, in equal-width score columns separated by a colon. VS AI labels the RED human Player and chooses a fictional club opponent identity once per new match and reuses it across scoreboard, jerseys, toss, prompts, and results. Local matches label RED Player 1 and BLUE Player 2.

- VS AI setup separates Difficulty from Gaya bermain. Style uses four shared PaperChips in two equal-width rows (Balanced, Parkir Bus, Kick & Rush, Total Football), with selected-state semantics and a short description. The selector scrolls when needed; retain selection while navigating home.

- Bot body language may briefly aim toward another considered candidate, then correct with slight overshoot. Its power release follows reaction timing on the shared meter. Thinking and sequential defensive pauses stay within the live phase clock; pause/exit cancels execution.

- Match airborne balls draw above their ground position by 25% of simulation height, capped at 72 dp for readability. Ground shadow and field coordinates remain anchored to the planar ball position; visual lift does not alter colliders or arena geometry.

- `GoalGeometry` supplies physical opening height, height projection, and net depth to physics and rendering. Cardboard goals show elevated front posts/crossbar and a receding net; no numeric dimensions are displayed. Goal width remains aligned to existing field collision geometry.

- Focus/formation pads follow facing: FRONT (BLUE) places its pad behind the standee toward screen top; BACK (RED) uses screen bottom. The BLUE focused kicker receives an 8.6% standee-size visual forward offset to match the RED visible contact gap; engine positions and ball colliders remain unchanged. Keep this offset through kick animation. Kicker approach keeps a fixed standoff radius and resolves nearby standee overlap rather than choosing a distant stance.

- Pitch markings omit the small six-yard boxes because they have no Foldball gameplay rule. Keep one large defensive-area box at each end (58% of field width), centered and symmetric, along with outer lines, halfway line, kickoff circle, and corner arcs. Goal opening and collision geometry remain unchanged.

- Sandbox temporary dent prototype uses a shared PaperToggle, labeled as resetting the Sandbox. Dented standees show a persistent compressed/leaning body and paper crease; the focused dented kicker shows effective strength, bounded direction error, and recovery after a goal. Match routes remain normal.

- Dented standees have a clearly buckled upper silhouette, a 27% height compression, 10% widening, and a 10-degree lean anchored at the base. A broad zigzag fold with ink shadow and paper highlight remains visible at gameplay size. This is visual only; collision and condition penalties are unchanged.

- Player Lab has one full-width PaperButton cycling dent preview 0/25/50/75/100% then normal. Its percentage controls the shared standee visual deformation continuously; it does not alter match damage rules or shot penalties.

- Movement Sandbox denting maps accumulated normal-contact speed to a continuous 0–100% visual deformation. Every standee displays its dent percentage below its body while the prototype is enabled; the aiming status shows current dent and effective strength. Condition labels use shared caption typography and do not intercept player gestures.

- FRONT focus pads extend upward from the standee foot anchor (62–85% of its bounding height), overlapping the 79.7% base pivot. Do not mirror the pad around the canvas center: that detaches it near the head. BACK keeps its existing 72–93% pad.

- Player Lab exposes Front/Back as equal-width shared PaperChips with selection semantics. Facing changes preserve current dent percentage and motion controls so both silhouettes and focus anchors can be compared.

- Standee top folds use Paper on Front and a 45% Paper-to-PaperEdge blend on Back for a shaded paper interior. Preserve the ink outline; avoid translucent black fill for Back folds.

- Airborne ball projection remains linear up to crossbar height, then smoothly approaches 72 dp using an exponential curve rather than clipping at a fixed height. Ground shadow stays anchored to the planar position, widens and fades with projected lift. Ball scale grows gently with lift; landing strength drives brief horizontal squash and vertical compression near the ground. Physics, goal clearance, and hitboxes remain unchanged.

- Dented kicker status communicates percentage, strength, faster meter, and recovery after goal. The shared meter cycle is 1.8 s normal to 1.3 s fully dented; no additional hidden directional error is caused by dent.

## UI language and match commentary

All user-facing UI text and accessibility labels must be in English (preserve user-created names). Match commentary reuses the upper field TapeLabel for 1.4 seconds with at least 2.5 seconds between messages. Use discrete verified events: OVER THE BAR!, OFF THE CROSSBAR!, OFF THE POST!, BLOCKED! Do not infer an overbar miss merely from flight height. Goal/results take priority; paused events are consumed without replay. Commentary does not intercept gameplay controls.

- Standee bodies remain fully opaque in inactive, opponent-turn, and charging states. De-emphasize jerseys via PaperEdge color blending (28% default inactive in Player Lab; 38% non-active team during formation), with a 360 ms color-blend transition in Match. Keep paper panels, names, outlines, and colliders solid; focus pads, body compression, and meter communicate active/charging state.

- Standee jerseys retain their original opaque colors in all focus/turn states; remove inactive/opponent color muting. Active kickers use a stronger Tape-yellow pulsing base and a gently bobbing ink-outlined paper arrow above the head. Formation-eligible players keep static dashed pads; inactive players have no focus pad. Arrow/pulse disappear when the kicker begins kicking; overlays do not intercept touches.

- Kicker pointer is a larger yellow bobbing arrow rendered above standee body layers, including Back folds. Movable formation players use smaller static Paper triangles in the same head-adjacent area, plus their dashed base. Inactive and kicking states show no pointer.

- Pointer follows team facing: FRONT uses the top of standee bounds, BACK uses the bottom with an upward-pointing mirrored marker. This keeps kicker and movable-player indicators behind their attack direction, away from the ball.

- Current selection indicators: kicker has a pulsing yellow base only, without a pointer. Formation-eligible movable players have a static Paper triangle above the standee for both Front and Back, plus the dashed base. Inactive/kicking players have no pointer. This replaces earlier facing-dependent/kicker arrow rules.

- During formation, reachable vacant zones for the active defending team receive a light team-colored tint and dashed boundaries before selecting a player. AI formation uses a quieter tint with no interactive overlay. Selecting a player replaces the team overview with that player's legal destinations. The existing legality checks determine all highlights; overlay disappears on leaving formation.

- Dent deformation accumulates an energy-weighted contact direction. Side hits bend left/right, frontal hits compress the center, and oblique hits combine lean and depth. Geometry stays stable between contacts; name/number printing follows the upper-sheet warp. External dent-status text stays upright. Collider geometry is unchanged.

- Dent center creases use a single directional fold with softly graded upper light and lower shadow, plus a fine crease edge. Avoid heavy zigzag outlines or opaque bands that look painted onto the jersey.

- Standee outer edges use a thin ink contour over a PaperEdge rim, with a small lower/right paper-thickness offset and sparse fixed cut-edge marks. All use the same warped silhouette when dented. Avoid uniformly jagged borders and heavy black sticker outlines.

- Home exposes an optional Standee damage PaperToggle, off by default, applying to both VS AI and local matches and retained for the app session. Explain timing difficulty and after-goal recovery before Play. In full matches, only damaged standees show percentage labels; Sandbox may show 0% for debugging.

- Full Match does not display persistent dent-percentage labels under standees. Damage remains visible through deformation; the focused kicker status explains its condition during aiming. Per-player percentage labels remain Sandbox diagnostics only.

- Current match damage policy: Standee damage is enabled by default for VS AI and local two-player matches. Home has no damage checkbox or explanatory toggle copy. Settings UI is deferred; Sandbox retains its independent diagnostic toggle. This supersedes earlier opt-in Home rules.

- Standee damage status is an unlabelled 34×5 dp horizontal bar beneath the body: remaining fill decreases with damage and transitions Grass→Tape→TeamRed. Paper backing and thin Ink border retain contrast on grass. Show for damaged, focused, or selected players; Sandbox may show all bars. No visible label or percentage; provide progress semantics for accessibility.

- Condition bars sit behind the standee relative to team attack direction: BLUE above its body bounds, RED below. Keep a 5 dp gap from the silhouette and preserve the planar position/hitbox.

- With standee damage enabled, show condition bars on every standee throughout the match, including full green bars for undamaged players. Bars remain behind each team's body and have no visible text. This supersedes conditional visibility rules.

- Condition bars anchor to visible paper edges, not the square component bounds: BLUE above the upper fold (11.9% of body height), RED below the foot pivot (79.7%). Use a 2 dp visible gap; include the BLUE kicker visual offset.

- Current damage lifetime: damage and accumulated bend direction persist through possession changes and kickoff after goals until the match ends. Only a new match/rematch or explicit Sandbox reset restores normal standees. Any recovery copy must say match-end/new-match recovery, not after-goal reset. This supersedes previous kickoff reset rules.

- Do not display dent/strength/meter status text over the field beneath commentary. Standee deformation and unlabelled condition bars communicate damage; commentary and power meter keep their existing space.

- Full matches end at three goals or three minutes. Scoreboard shows total countdown separate from phase seconds. Pause and opening toss do not consume match time; a final in-flight shot completes before result. Ties show DRAW without overtime.

- Scoreboard countdown occupies the center separator between equal-width RED and BLUE score/name columns, replacing the colon. Keep the phase timer separate in its existing side card.

- Match result separates team names above large scores in two equal columns; do not concatenate names ending in digits with scores. Result actions stack full-width for long labels. Goal celebration uses a spring entrance, a short decaying paper-banner wobble, an expanding fading ink burst, and team-colored paper confetti; motion resets for each goal and does not alter play.

## Welcome and club identity

First launch opens Welcome within safe drawing insets. Require a trimmed nonempty team name up to 20 characters and a badge from the 16 bundled WebP assets. Badge choices use equal-width PaperCards with minimum 64 dp targets, radio selection semantics, and Tape selected fill; PaperCard accepts a shared-token color override. Team-name entry uses Material OutlinedTextField with Foldball body typography inside PaperCard. Continue saves the profile locally; Home exposes Edit. AI and local opponents draw from badges excluding the human selection; select once per match. Badge appears with name and score in Match. Original PNG references are preserved; runtime badges are 256×256 WebP.

- Scoreboard badge layout is compact: each equal-width team column has a 36 dp badge beside its 30 sp score, with an 11 sp single-line name below. Mirror badge placement at the outer edges of the two score groups; reserve 48 dp for the centered 17 sp countdown. Do not stack badge, score, and name as three separate rows.

- Scoreboard uses RED badge + short name on the left, centered score above total countdown, and BLUE short name + badge on the right. Short names are 1–4 alphanumeric characters and editable on Welcome; blank input auto-generates one. Persist with the profile; legacy profiles derive a fallback. Full names remain in Home/results.

- Scoreboard paper card wraps compact badge/short-name groups and a centered score/time column instead of stretching to full width. Center the complete card on the screen; pause remains at the leading edge with reserved clearance. Avoid weighted team groups that push badges to opposite card edges.

- Team setup uses shared `FoldballSpace` values. Show Loading while the profile is read, disable edits and save while saving, and keep drafts when saving fails. Existing teams expose Save team and Cancel; first launch exposes Continue. Retry reloads only a failed initial read. UI text and accessibility descriptions remain English.

- System Back during a full match opens the existing RETURN HOME? confirmation and pauses gameplay. Leave match returns Home; Keep playing or dismissing the confirmation resumes. Existing dialogs handle Back dismissal before route navigation.

- Home field preview is a decorative 150 dp portrait-pitch strip in a shared Grass PaperCard with a subtle tilt and paper shadow. Blue standees face Front in the upper row; red standees face Back in the lower row. Keep complete bodies and shadows inside the crop, with a centered paper ball, horizontal halfway line, quiet paper bands, and slim Cardboard side rails. Hide decorative preview children from accessibility traversal.

- Welcome is the first-launch onboarding destination while no saved team profile exists. After Continue saves successfully, replace Welcome with Home so Back cannot reopen onboarding. Team is a separate management destination opened from Home: load the saved name, short name, and crest; Save or Cancel returns Home. Both routes use shared paper form controls, preserve drafts on save failure, and receive safe drawing insets from the app root.
