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
| `InkMuted` | `#62656B` | Secondary technical labels |
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
- **App navigation:** Configure destination transitions once on the root `NavHost`. Switch routes without a full-screen animation so the shared wood surface and safe drawing edge remain steady at the iOS status bar. Screen-specific motion, such as the opening toss, remains inside its route.
- **Menu grids:** Use `MenuCard` with the documented alternating tilt sequence. Do not invent a new card treatment for each tile.
- **Match HUD:** Compose `MatchScoreboard`, `MatchStatusPanel`, and the integrated aim/power guide from shared tokens and paper components. Show team identity with labels as well as color. Keep the score pair centered, with each player/team name below its score. Show the red human player on the left and the blue opponent (AI in VS AI) on the right.
- **Full match setup:** Both Play vs AI and Play 2 Players open a PaperDialog with a 3v3/5v5 format choice, using equal-width PaperChips and a short description. Default to 3v3 and retain the choice when returning home. 3v3 uses the two wide players and striker; 5v5 adds a center player and midfielder. The engine roster follows the selected format; Sandbox retains its original four-player formation.
- **Match mode:** Both Play vs AI and Play 2 Players use equal-width PaperChips for Classic and Attack & Defend. Keep the selected mode for the app session and explain it under the chips. Attack & Defend shows the current half in the existing status tape and counts down each 60-second half in the existing scoreboard timer; reuse the established match controls and colors.
- **VS AI:** Play controls the red team; the blue bot uses the same formation and kick rules. Disable formation actions and player gestures during bot turns, and label bot aiming explicitly. Sandbox remains manual. The VS AI setup dialog also lets players select Easy, Medium, or Hard using equal-width PaperChips with selection semantics and a short description. Play confirms entry; Cancel dismisses it. Default to Medium and retain the choice when returning home.
- **Local 2 Players:** Home exposes a secondary Play 2 Players button below Play vs AI. Both teams use the existing formation and aiming controls on one device; keep match timers, scoring, pause, and restart behavior consistent. AI difficulty applies only to Play vs AI. The existing tape label explicitly shows the team name plus TURN and DEFEND or KICK, with a polite accessibility live region. Use English for match labels and instructions, including local two-player handoffs. Keep brief move/aim/hold/release instructions inside the existing bottom control height; do not resize the arena for handoff guidance.
- **Match turn indicators:** Tint the countdown team label with that team's color. Keep bottom actions content-sized and distribute them across the bar; use the compact button variant when space is limited.
- **Match team emphasis:** Fade the non-turn team smoothly to 68% during formation. Keep both teams fully visible during aiming, kicker approach, and ball movement. Keep the fade subtle enough to preserve jersey and position readability.
- **Match haptics:** Play one subtle platform haptic when entering a formation or aiming phase, when a player move is accepted, when a kick is released, and when a goal is scored. Do not repeat haptics during timer or animation frames.
- **Match formation and kick states:** During opening kickoff formation, label the defending team and show that movement stays in its own half. Highlight movable players with a pale dashed base marker; selecting one player shows neighboring zones with a reachable, collision-safe drag path in a subtle team-color fill and bright dashed outline, and blocked neighboring zones with a subtle gray fill and outline. Dragging into a highlighted zone may settle on its safe edge when the center is obstructed. Leave non-neighboring zones unmarked and draw no connector lines. During later formation turns, use the same marker and zone treatment. Reserve the yellow tape focus marker for the active kicker.
- **Full-match defensive move:** In 3v3 and 5v5, the defending team may reposition one standee. Classic uses a default 6-second defensive timer and retains the defensive phase after the move so the human can Undo or confirm with Done; before a move offer Skip. Undo restores the standee and move allowance without resetting the countdown. Done, Skip, or timer expiry starts aiming and closes Undo. The compact bar uses shared compact PaperButtons with 48 dp touch targets and a single-line weighted/ellipsized label to fit narrow screens, without resizing the arena. Bot defense confirms automatically through its existing FinishDefense command. Attack & Defend also offers Undo/Done after its defensive move; Sandbox retains its two-move lab behavior.
- **Attack & Defend positioning:** After defense, start the regular aiming timer immediately. During that same timer, the attacking side may move one teammate other than the kicker, or aim and kick without moving. The compact status bar explains both actions and offers Undo after moving the attacker, restoring its position and movement allowance without resetting the aiming timer. Hide Undo while charging and close it on release or timer expiry; do not add a separate positioning countdown or Skip step. The bot may make the same one move before aiming. Classic keeps its existing aiming behavior.
- **Experimental defensive possession:** `MatchRuleFlags.DEFENSIVE_BLOCK_WINS_POSSESSION` enables or disables the rule for full matches. When enabled, a shot that physically hits a defending standee gives that team the next kick after the ball settles, using its nearest standee. Keep the ball and standee positions; a goal still takes precedence. Sandbox keeps the original nearest-player rule.
- **Aiming:** During aiming, drag the focused standee left or right to aim within the original 1.05-radian arc on either side of its forward direction. The guide follows the engine shot; holding continues the power meter and releasing kicks. Vertical drag does not change aim. Do not move player positions or change arena hitboxes for the gesture.
- **Aim guide:** Use a fixed straight pool-style translucent Paper track with integrated power fill and Soft End: a fading endpoint without a ring, cross, line marker, or arrowhead. Aim length is 22% of arena height capped at 140 dp and follows only the shot direction; crop at drawing edges. The track never changes length, elevation, or curvature with charge. Fill follows the current power directly from the ball toward the endpoint, blending Grass through Tape to TeamRed; unfilled dots preserve direction and fade at the end. Remove the separate vertical Power meter and loft labels in Match/Sandbox. Expose aim/power semantics on the drawing and preserve all charging/release gestures and engine timing. This is a direction and charge indicator, not a trajectory or landing prediction. Reuse `gamekit/presentation/field/` for human and AI turns.
- **Standee loft collision:** Give standees a fixed 52 dp physical height above the field. At the planar contact point, use the ball's interpolated height: low shots still collide, while a ball whose bottom has cleared that height passes over without a block or possession claim. Jersey damage and visual standee compression do not change this physical height.
- **Next-kicker approach:** After a shot settles, finish any necessary ball placement near the goal or wall before moving the next kicker. Keep the ball-placement portion at 0.22 s and the standee approach at 0.36 s; do not animate both together. The physical stopping rule and final ball/standee positions remain unchanged.
- Reset the power-timing session whenever aiming ends, including a timer-triggered automatic kick, so the next turn starts a fresh meter when the kicker is held again.
- **Gameplay shadows:** Keep player cast shadows anchored to the standee's bottom edge so the player reads as touching the field. Place the ball contact shadow immediately below the ball; let it become wider and softer as the ball rises, while the contact mark fades.
- **Arena viewport:** Full matches and Practice simulate at 393×580 units; Movement uses 393×470. Fit the entire stadium uniformly into available space, centered horizontally, without stretching its aspect ratio. The transformed layer maps player movement and aiming input back into simulation coordinates; viewport changes must not recreate the engine. Ball, player, net, and effects share the same transform. HUD stays outside the scaled arena.
- **Match arena sizing:** Keep field width and height independent of phase-specific labels, status panels, and actions. Reserve consistent space for match controls so changing a bottom action never resizes the field or resets its coordinate system.
- **Opening toss and goal celebration:** Use a torn `PaperCard` or `Banner` and the same team colors and type scale as the HUD. Toss motion follows constant-gravity ballistic arcs with energy-losing ground rebounds, then a friction-limited roll. Use the match gravity scale and ground restitution; keep the shared match ball size. Ground travel stays straight, and the ground shadow broadens and fades with height.
- **Arena:** Preserve the classroom desk, paper field, and cardboard materials. Use shared material tokens for newly drawn colors; do not change playable bounds to make a visual treatment fit.
- **Dialogs and feedback:** Use `PaperDialog` for confirmation/results and the shared ink toast treatment for transient feedback.
- **Settings and selection:** Use `PaperToggle` and `CirclePickerItem` when those flows are introduced.
- **Shot practice:** Practice is a separate Sandbox tab with a fixed, unscrollable arena and compact controls outside it. Start with one RED kicker and 0–5 manually placed opponents using shared 48 dp minus/plus PaperChips. Reset shot retains opponents and tuning. Start directly in aiming with no turns, Done/Skip step, move quota, own-half restriction, or countdown. Opponents can be dragged freely within collision-safe field bounds while the ball is idle. Move toggles kicker placement, carrying the ball with it while preserving the ready-to-kick gap; Kick returns to aiming/charging gestures. Undo restores both kicker and ball. Practice uses the match arena geometry and production physics defaults; the same kicker receives every shot. Tuning opens a paper dialog with percentage sliders for wall/player/ground bounce (0–100%) and full-power forward speed (55–100% of original launch speed). Defaults restores 65/50/30/100; Apply resets the drill. Cancel discards edits. Tuning affects Practice only; Movement retains its original roster and timers, and matches retain production defaults. Only the tuning dialog may scroll.
- **Developer labs:** They are reachable through Sandbox today. Keep their controls aligned with this system if they remain accessible from the player-facing app.

A component receives display data, accessibility text, and callbacks from its owner. It must not decide gameplay rules or navigation destinations.

## 6. Accessibility and Layout

### Text contrast (WCAG 2.2 AA baseline)

Use [WCAG 2.2 SC 1.4.3](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum) as the contrast baseline: normal text needs at least 4.5:1; large text needs at least 3:1 (18 pt regular or 14 pt bold equivalent). Use 4.5:1 for Foldball body, caption, and handwritten labels unless their large-text eligibility has been verified.

- Place page headings and descriptions on shared PaperCard surfaces with Ink text. Ink on Paper is 15.03:1. Do not place InkMuted subtitles directly on wood: the previous pairing was only 1.08:1.
- InkMuted is #62656B, intended for secondary text on Paper (5.18:1). It is not a universal color for text on wood, grass, tape, or images.
- Assess foreground against the actual rendered background, including texture, transparency, gradients, overlays, and disabled/selected states. Put text on an opaque shared surface when contrast cannot be ensured.
- Keep error messages readable on Paper and loading text on wood in Paper. Never rely on color alone to identify an error.
- Verify text wrapping and font scaling without clipping or obscuring actions, preserve 48 dp touch targets and semantics, and inspect on a device. Contrast measurements alone do not establish full WCAG compliance.


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

- AI aiming uses the same integrated aim/power guide as human aiming. Aim moves continuously before charging; the guide fill remains visible while the bot charges. Pause cancels bot charging without an automatic release.

- Match scoreboard places the RED bottom-side player on the left and BLUE opponent on the right, in equal-width score columns separated by a colon. VS AI labels the RED human Player and chooses a fictional club opponent identity once per new match and reuses it across scoreboard, jerseys, toss, prompts, and results. Local matches label RED Player 1 and BLUE Player 2.

- VS AI setup separates Difficulty from Gaya bermain. Style uses four shared PaperChips in two equal-width rows (Balanced, Parkir Bus, Kick & Rush, Total Football), with selected-state semantics and a short description. The selector scrolls when needed; retain selection while navigating home.

- Bot body language may briefly aim toward another considered candidate, then correct with slight overshoot. Its power release follows reaction timing on the shared meter. Thinking and sequential defensive pauses stay within the live phase clock; pause/exit cancels execution.

- Match airborne balls draw above their ground position by 25% of simulation height, capped at 72 dp for readability. Ground shadow and field coordinates remain anchored to the planar ball position; visual lift does not alter colliders or arena geometry.

- `GoalGeometry` supplies physical opening height, height projection, and net depth to physics and rendering. Both goals use the original shared cardboard frame and paper net renderer, mirrored in depth direction. Keep posts vertical and anchored at the ground, with 8/5 dp outer/inner post strokes and 7/4 dp crossbar strokes. Net backing uses 70% Paper opacity and the roof 45%, consistently at both ends. Frame lift follows the original ball projection (0.25, smooth 72 dp maximum); do not scale the arena or modify physics to enlarge goals. Render both goals behind match entities. Both visual frames and nets are centered and 12 dp narrower than the original rendering, with each post inset by 6 dp. Physical goal width, playable bounds, scoring, and collisions remain unchanged.

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

First launch opens Welcome within safe drawing insets. Require a trimmed nonempty team name up to 20 characters and a badge from the 25 bundled WebP assets. Badge choices use equal-width PaperCards with minimum 64 dp targets, radio selection semantics, and Tape selected fill; PaperCard accepts a shared-token color override. Team-name entry uses Material OutlinedTextField with Foldball body typography inside PaperCard. Continue saves the profile locally; Home exposes Edit. AI and local opponents draw from badges excluding the human selection; select once per match. Badge appears with name and score in Match. Original PNG references are preserved; runtime badges are 256×256 WebP.

- Scoreboard badge layout is compact: each equal-width team column has a 36 dp badge beside its 30 sp score, with an 11 sp single-line name below. Mirror badge placement at the outer edges of the two score groups; reserve 48 dp for the centered 17 sp countdown. Do not stack badge, score, and name as three separate rows.

- Scoreboard uses RED badge + short name on the left, centered score above total countdown, and BLUE short name + badge on the right. Short names are 1–4 alphanumeric characters and editable on Welcome; blank input auto-generates one. Persist with the profile; legacy profiles derive a fallback. Full names remain in Home/results.

- Scoreboard paper card wraps compact badge/short-name groups and a centered score/time column instead of stretching to full width. Center the complete card on the screen; pause remains at the leading edge with reserved clearance. Avoid weighted team groups that push badges to opposite card edges.

- Team setup uses shared `FoldballSpace` values. Show Loading while the profile is read, disable edits and save while saving, and keep drafts when saving fails. Existing teams expose Save team and Cancel; first launch exposes Continue. Retry reloads only a failed initial read. UI text and accessibility descriptions remain English.

- System Back during a full match opens the existing RETURN HOME? confirmation and pauses gameplay. Leave match returns Home; Keep playing or dismissing the confirmation resumes. Existing dialogs handle Back dismissal before route navigation.

- Home field preview is a decorative 150 dp portrait-pitch strip in a shared Grass PaperCard with a subtle tilt and paper shadow. Blue standees face Front in the upper row; red standees face Back in the lower row. Keep complete bodies and shadows inside the crop, with a centered paper ball, horizontal halfway line, quiet paper bands, and slim Cardboard side rails. Hide decorative preview children from accessibility traversal.

- Welcome is the first-launch onboarding destination while no saved team profile exists. After Continue saves successfully, replace Welcome with Home so Back cannot reopen onboarding. Team is a separate management destination opened from Home: load the saved name, short name, and crest; Save or Cancel returns Home. Both routes use shared paper form controls, preserve drafts on save failure, and receive safe drawing insets from the app root.

- Additional crest references from `referensi_assets/foldball_ball_assets/logo/` are appended as badge IDs 17–25 in numeric source-filename order. Runtime images remain 256×256 transparent WebP; existing badge IDs 1–16 stay stable. Welcome, Team, and opponent selection use the shared badge catalog.

- Welcome keeps Continue outside the scrollable form, pinned at the bottom within the root safe insets and above the keyboard. Use a compact paper heading with a live club preview. Show four badge shortcuts; choosing another badge from See all badges replaces the fourth shortcut with that selection. The full catalog lives in a scrollable PaperDialog, closes on selection, and preserves the draft. Use three dialog columns on narrow widths to retain 48 dp touch targets. The form may scroll on short screens; Continue never requires scrolling.

- Home's Club entry displays the saved crest, club name (up to two lines with ellipsis), and Manage your club subtitle. The entire paper card is a shared PressableSurface with menu tilt, shadow, press feedback, and button semantics; it opens Team management. Do not duplicate it with an Edit action in the header.

- Home uses a compact marker wordmark above a field hero, with a READY FOR KICKOFF TapeLabel. The decorative field reflects the selected ball style and saved crest; the ball rocks gently in place without changing geometry. Place both Play actions directly below the field. Remove inactive placeholder settings actions.
- `DeskMenuCard` is the shared paper equipment-menu component: shared PressableSurface, alternating menu tilts, a 64 dp art slot, two-line reserved title/subtitle areas, and full-card button semantics. Home's Club shows the saved crest/name, Ball the chosen ball, Arena a miniature pitch sketch, and How to play a ruled tactics notebook. Use shared colors and spacings throughout; decorative art is excluded from accessibility.

- Club management lives in `feature/club/` (`ClubScreen`, `ClubRoute`, `ClubViewModel`, `ClubContract`, `ClubModule`) and uses AppRoute.Club. Gameplay TeamProfile/TeamIdentity remain domain concepts. The screen uses a compact high-contrast identity preview, a Club name counter attached to that field, and four crest shortcuts with a full catalog dialog. Shared `ClubBadgeGrid` renders both Welcome and Club selections. Cancel and Save club remain outside the scrollable body and above the keyboard; save errors preserve the draft. Do not show inactive jersey/customization controls before those features exist.

- `PaperBackButton` is a reusable 48×48 dp paper icon action with an arrow, Go back accessibility label, shared press feedback, and an enabled state. Its owner supplies navigation; do not embed route logic in the component. `FoldballTopBar` places this action in its own paper chip beside a centered title on a separate content-sized PaperCard, with symmetric trailing space. Keep it outside scrollable content; safe insets come from the route root. Club uses this top bar separately from its centered crest/name identity card, disables Back while saving, and omits the redundant introductory subtitle.

- Top bars expose the wood background between the Back chip and the separate title paper; do not wrap the entire bar in one paper surface. Initial profile loading shows no loading copy; preserve a visible failure message and Retry action when reading fails.

- Default jerseys derive from the curated `ClubKit` crest palette (25 stable mappings), without runtime image extraction or new persistence fields. Club shows a live paper-standee preview; changing crest changes jersey panels only. RED/BLUE remain internal gameplay sides. Match jersey, scoreboard, turn/formation indicators, toss, goal, and result colors use `MatchTeamConfiguration.kitColor`. The opponent keeps its crest color unless squared RGB distance is below 0.09; in that case select the most distinct curated alternate. This is a visual clash heuristic, not a WCAG or color-vision accessibility guarantee; keep names, crest, and positional indicators. Home's decorative players follow the same palette.

- Jersey patterns support Plain, Stripes (vertical), Hoops (horizontal), and Chevron on both Front and Back panels. Apply patterns inside the existing warped jersey clip; preserve paper folds, print, shadows, collisions, and player geometry. Club offers Match crest plus four pattern chips with selected semantics and a live preview. An explicit pattern is persisted with the profile; absent/unknown stored values use the curated crest default. Selecting another crest retains an explicit override; Match crest restores automatic selection. Home and Match use the saved pattern.

- Club top bar shows the draft club name with a 48 dp Edit club name pencil action. `FoldballTopBar.actions` accepts an optional reusable trailing action slot. Club separates Crest (large crest, shortcuts, catalog action) and Jersey (large centered standee and pattern chips) on opaque paper cards. Name/short name move to an edit dialog: Cancel discards dialog changes, Apply validates and updates the screen draft, and Save club persists everything. `PaperDialog.confirmEnabled` disables Apply for invalid names.

- Current jersey default is Plain for every crest; remove Match crest/automatic pattern controls. Existing explicitly saved patterns remain selected. Club uses a 104 dp standee preview. Two tone splits the jersey vertically into the crest primary and a contrasting curated dark companion (navy or gold), keeping printing readable; apply the same split to Front/Back and preserve existing paper geometry and physics. This supersedes crest-derived pattern defaults; crest still determines primary color.

- Club crest shortcuts sit on the desk outside the selected-crest PaperCard. See all badges uses a centered, content-width compact PaperButton below the shortcuts; avoid nesting its paper/shadow inside another paper surface.

- Club's logo shortcuts now use a horizontal `ClubLogoStrip` with 10 items and a fixed 48 dp grid-icon catalog action on the right (See all badges). Each shortcut is a 64 dp radio target, selected in Tape, with room for paper tilt/shadows. If the selected logo is outside IDs 1–10, replace the tenth shortcut with it and scroll it into view. The catalog dialog remains available; remove the separate See all badges text button from Club. Welcome keeps its existing shortcut layout.

- Horizontal Club logo shortcuts reserve roughly 35% of the next card at the viewport edge to reveal scrollability. Adapt card width to available space while retaining at least 64×64 dp selection targets. The grid action stays fixed outside the scrolling region.

- Club separates Jersey colors (1–4) from Plain/Stripes/Hoops/Chevron. Curated crest palettes provide a primary plus three logo-derived accents or matching neutrals. With two colors, Plain uses split panels and other patterns use the second palette color; third color adds collar/hem trim and fourth adds thin side accents. Multi-color kits print white names/numbers with a fine Ink outline for readability; never use opaque rectangular patches. Preserve warped clips and gameplay geometry. Save color count with the profile; old Two tone selections load as two colors and are presented as Plain plus 2 colors.

- User-facing terminology is Club badge everywhere: section titles, picker dialogs, catalog actions, and accessibility labels. Use Choose your badge, Select a badge for your club, and See all badges. Do not mix logo or crest into UI copy; existing internal identifiers may retain those names.

- `FoldballTopBar.subtitle` optionally shows a smaller centered caption below the title on the same paper. Club supplies the draft short name (including the auto-generated abbreviation when blank); editing updates both immediately after Apply. Omit the subtitle on screens that do not need it.

- Club's top bar title is Your Club. The badge card has no Club badge heading: center the badge, full club name, short name, and Edit club name pencil action together.
- AI identities map to play style: Balanced = Paperford FC (PAPF, badge 20), Park the Bus = Ironwood FC (IRON, 18), Kick & Rush = Falcon United (FALC, 19), Total Football = Aurora Athletic (AURA, 24). Difficulty changes bot skill, not identity. Use a fixed themed alternate badge (9/4/22/8 respectively) only if the human uses the main badge. Resolve the complete match identity once; Play again retains name, badge, kit palette, and pattern. Local Player 2 keeps a local identity rather than an AI club. This supersedes randomized opponent identity rules.

- The status label directly below Match scoreboard uses the acting team jersey color as its background: defending team during formation, possession team during kick/motion, scoring team during Goal. Match complete uses neutral Tape. TapeLabel accepts background/content colors and selects the higher-contrast Ink/Paper foreground by luminance. Keep team names and live-region semantics; colors never replace textual turn information.

- Do not play countdown warnings for total match time approaching zero. Human-turn countdown cues at 3/2/1 seconds use timeout volume 0.25 (previously 0.35), play once per threshold, and consume muted thresholds without replay. The final whistle/result sounds remain tied to match completion.

- Formation/aiming entry cues use original synthesized mono PCM wood knocks: RED/Player 1 has rising two-note knocks, BLUE/Player 2/AI falling knocks. Attack adds a short paper-flick tail. Clips last 250–340 ms at volume 0.3, fire once per phase entry, and replace the phase-entry kickoff whistle. Consume muted/paused transitions without replay; no cues during toss, Sandbox, or other phases. Keep visual actor/action text and team-color status; stop playback on pause/background/exit.

- Multicolor jersey printing uses a thin font-scaled Ink outline underneath the white text. Both layers share the standee deformation and placement. Remove black backing rectangles from names and numbers.

- Match status TapeLabel uses the emphasized 16 sp caption variant with 0.5 sp tracking, 4 dp vertical padding, and a 1 dp Ink border around the acting-team color surface. Keep the same field-size calculation and polite live-region text; ellipsize long single-line messages instead of growing arena layout. Other tape labels retain their existing sizes.

- Match status below the scoreboard identifies clubs by their saved short name (`jerseyLabel`) in every formation, kick, possession, turnover, and goal label, including local handoffs. Full names remain in Club and match results.

- Turn countdown audio uses an original synthesized 110 ms soft wood tap (countdown_tick) at volume 0.25, retaining once-per-threshold 3/2/1-second human-turn reminders only. The lower-left phase timer uses `TurnTimer`: a fixed 52×48 dp paper chip, 24 sp Ink text, and a 1 dp Ink border; fill changes Paper to Tape for the final three seconds. Keep its size stable, expose remaining seconds with the club short name, and preserve field geometry.

- Toss reserves the same upper status-label measurement and 56 dp bottom control slot as full-match phases. Keep the hidden toss status out of accessibility and render a noninteractive bottom spacer. The SpaceBetween arena column then retains the same field position before and after kickoff; do not change field dimensions or physics to compensate.

- Home field standees display the saved club short name and style-mapped AI club short name instead of hardcoded player names. Kit colors, palette, pattern, and color count follow the same identities. Keep printing single-line with ellipsis for long names; Match status still uses short names.

- Opening kickoff toss starts automatically as soon as the 1.2-second paper-crumple animation finishes. Remove the Start button; choose the kickoff team once, launch the existing bounce trajectory, show the settled result briefly, and enter formation. Preserve toss physics, field position, selected ball style, and club identity.

- Android launcher uses a folded Paper ball with InkBlue seams on a subtle desk-wood background, with a small Shadow-token brown (#352213) paper shadow and no text. Adaptive and legacy icons share the same centered geometry. Launch splash uses a separate transparent paper-ball vector centered on a full-screen Wood background, without a launcher tile; Android 12+ uses a solid Wood color while older versions use the desk texture; Android 12+ uses the system splash, older Android uses a centered window drawable. Switch to the app theme on activity creation; do not add artificial splash delays.

- iOS uses the same opaque 1024 px launcher artwork in AppIcon. LaunchScreen.storyboard centers a separate transparent 144 pt paper-ball LaunchIcon on a full-screen subtle desk texture with Auto Layout; omit launcher field tiles, without artificial delays; system-generated launch screen is disabled.

- Front and Back standees share the same body panel (x 122–390 at the feet, x 160–352 at the upper fold), top fold and rear support silhouette. Preserve the 408/512 foot anchor, printing distinctions, collar details and physics hitboxes; facing must not change apparent body size.

- Settings is reachable from the top-right Home header with a shared 48 dp PaperIconButton using icon_settings and Settings accessibility label; keep it outside the scrolling menu and uses the safe-inset root, FoldballTopBar and paper form. Advanced physics has percentage sliders, Use for matches and Restore defaults; Save persists locally. Practice starts with saved values. Match tuning is captured on entry and applies equally to both teams and bot simulation, never during an existing match. Disable editing while loading/saving, retain drafts on save failure, and offer load retry. Defaults restores 65/50/30/100 and disables match overrides.

- Back jersey covers the side support wings out to x 92/420 at y 392, tapering into the unchanged x 122/390 foot edge at y 408. Front retains visible paper supports; Back hides them behind its wider printed jersey. Overall silhouette, foot pivot and gameplay hitboxes stay unchanged.

- How to play opens a six-page paper guide: scoring/toss, defender moves, aim/hold/release, bounce/possession, Attack & Defend, and Practice/Settings. Use current English rules, page count, Next/Previous and Close; keep long text scrollable inside a bounded content area so actions remain visible, with polite page-change semantics and shared touch targets.

- BGM is an original quiet 32-second plucked-note/paper-percussion loop at 12% volume, played independently from SFX. Settings and pause menu expose a Music toggle; pause/background stops music, foreground resumes when enabled. Use native continuous looping rather than scheduling repeated effects; no gameplay physics changes.
