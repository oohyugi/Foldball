# Repository Guidance

## UI and visual implementation

Before proposing, editing, or implementing any user interface, read [`docs/Foldball Design System.md`](docs/Foldball%20Design%20System.md). Treat that document as the source of truth for Foldball colors, typography, spacing, paper shapes, shadows, interaction states, and shared UI components.

- Check the existing components and tokens in `shared/src/commonMain/kotlin/com/yogi/foldball/core/ui/theme/` before creating a new surface or control.
- Reuse the design system and keep UI code in the existing `:shared` Gradle module. Do not assume a separate `core:designsystem` module exists.
- Keep screen-specific UI under `feature/<name>/presentation/`; keep reusable ball, field, and player rendering under `gamekit/presentation/`.
- Avoid screen-local color, typography, paper-shape, shadow, and press-state recipes when a shared token or component applies.
- Preserve arena geometry, gameplay hitboxes, and physics while changing visual presentation.
- Keep Home and Sandbox within safe drawing insets at the app root. The full-screen Match route may draw behind transparent system bars, but must inset interactive content at its route root.
- If a UI change needs a new visual rule or reusable component, update `docs/Foldball Design System.md` in the same change.
- Read the system's accessibility and layout guidance before changing interactive controls; keep their touch targets and semantics intact.
