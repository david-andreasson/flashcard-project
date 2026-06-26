## Why

The frontend is effectively unstyled. The only stylesheet is leftover Vite-scaffold CSS that no
component uses, while every screen renders with ad-hoc inline styles and hardcoded colors that
ignore the design tokens and clash in dark mode; native form controls are unstyled. The app works
but looks rough and inconsistent — poor for a tool meant for long study sessions. A consistent
design system plus user-selectable, calming themes make it pleasant to use for hours.

## What Changes

- Adopt Tailwind CSS v4 (via the Vite plugin) and define a semantic, token-based design system
  (background / surface / text / muted / border / accent, plus typography, spacing, radius).
- Add a theme system with five calm, soft themes — **dark, light, soft pink, soft green, soft
  brown** — each recoloring the whole page (background + surfaces + a coordinated accent), chosen
  from a switcher in the top nav, persisted across sessions, defaulting to the OS color scheme on
  first visit, and applied before first paint (no flash).
- Build a small set of reusable UI primitives (button, input, select, textarea, card, page header,
  alert/notice) and restyle every screen and the app shell/nav to use them.
- Remove the dead scaffold CSS; replace the stale HomePage stub with a real landing/dashboard.
- Visual change plus one small data addition: the course list shows per-course deck and card
  counts (for the redesigned Courses screen). No other behavior, route, or data-flow changes.
- Update `docs/ROADMAP.md` to reflect reality (10 = inline-deck-course-creation done,
  11 = frontend-facelift, 12 = aws-deployment deferred).

## Capabilities

### New Capabilities

- `frontend-theming`: user-selectable, persisted, whole-app themes (five) with an OS-based default
  and flash-free application on load.
- `frontend-design-system`: a shared, token-driven Tailwind design system and reusable UI primitives
  applied consistently across all screens, replacing ad-hoc inline styles, with no behavior change.

### Modified Capabilities

(none — existing screen behaviors, routes, and APIs are unchanged; this adds a visual/theming layer.)

## Impact

- Frontend dependencies: add `tailwindcss` and `@tailwindcss/vite` (v4).
- One small backend addition: per-course deck/card counts on the course list response
  (`CourseResponse` + grouped count queries in `DeckRepository` / `CardRepository`). No database or
  migration changes.
- Touches `vite.config.ts`, `src/style.css` (replaced by the token/theme + Tailwind layer),
  `index.html` (pre-paint theme script), `RootLayout` (nav + theme switcher), and every screen
  component (restyle). Adds `src/components/ui/` primitives and a small theme module/hook.
- Removes the dead Vite-scaffold CSS; rewrites the HomePage stub into a real landing.
- Docs: `docs/ROADMAP.md` restructured (adds rows 10/11, marks AWS deferred as 12).
- No automated frontend tests (the project has no frontend test runner); verified via the `tsc`
  build and a manual visual pass across every screen in each theme.
