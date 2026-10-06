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
