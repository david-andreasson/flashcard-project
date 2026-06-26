## Context

The frontend is React 18 + TypeScript + Vite 6. The single `src/style.css` is ~90% dead
Vite-scaffold CSS (`.hero`, `#app`, `#next-steps`, `.vite`, …) that no component uses; only its
`:root` tokens, a `prefers-color-scheme` dark block, and base `h1/h2/p/code` rules are live.
Every screen uses ad-hoc inline styles with hardcoded colors (`#ccc`, `crimson`, `#888`) that
never reference those tokens, so they clash in dark mode; native form controls are unstyled. There
are ~12 screens plus the `RootLayout` nav, and no frontend test runner. The styling approach was
chosen during explore: Tailwind CSS v4.

## Goals / Non-Goals

**Goals:**
- A consistent, token-driven design system (Tailwind v4) across every screen and the app shell.
- Five calm, whole-app themes, user-selectable from the nav, persisted, OS-defaulted, flash-free.
- Reusable UI primitives so screens are clean and visually consistent.
- Remove the dead scaffold CSS; turn the HomePage stub into a real landing.

**Non-Goals:**
- No behavior, route, or data-flow changes beyond one small additive enhancement — per-course
  deck/card counts on the course list, added for the redesigned Courses screen. No database or
  migration changes.
- No new features or screens; the theme switcher is the only new control.
- No automated frontend test harness (no runner is added).
- No i18n; responsive/mobile polish is best-effort, not a full overhaul.

## Decisions

### Decision 1: Tailwind CSS v4 via the Vite plugin
Use `@tailwindcss/vite` plus `@import "tailwindcss"` in the stylesheet — minimal config (no PostCSS
or `tailwind.config.js` needed in v4), first-class Vite integration, and utility-first styling that
fits an incremental migration away from inline styles. Chosen in explore over a component library /
shadcn / hand-written CSS for its balance of speed, control, and transferable skill.

### Decision 2: Theme system via semantic CSS variables swapped by `data-theme`
Define semantic tokens as CSS custom properties (`--bg`, `--surface`, `--text`, `--muted`,
`--border`, `--accent`, `--accent-contrast`). Each theme is a block overriding those vars under a
selector (`:root[data-theme="dark"]`, `…="pink"`, …). Tailwind v4 `@theme inline` maps utilities
(`bg-surface`, `text-muted`, `border-default`, …) to the runtime vars, so utilities resolve to the
active theme and every screen follows automatically. Switching = set
`document.documentElement.dataset.theme`. Adding a theme later = one more var block.
- Alternative: Tailwind's binary `dark:` variant only. Rejected — we need five named themes.

### Decision 3: Five calm themes, each recoloring the whole page, with a coordinated accent
`dark`, `light`, `soft pink`, `soft green`, `soft brown`. Each sets the page background, surfaces,
text, and a harmonizing accent (purple for dark/light; dusty rose / sage / caramel for the tinted
themes). Palettes are muted/low-saturation for long-session comfort, and surfaces are a soft tint of
the same hue (not pure white) so the whole screen carries the theme. Hex values live in the
stylesheet and are tunable; the five theme identities are fixed.

### Decision 4: Persistence, default theme, and flash-free application
Persist the chosen theme in `localStorage`. On first visit (no stored value) default to the soft
brown theme. A tiny inline script in `index.html` reads localStorage and sets `data-theme` on
`<html>` before React mounts, avoiding a flash. A small React hook/context exposes the current theme
and a setter that writes through to both `localStorage` and the `<html>` attribute.

### Decision 5: Theme switcher in the nav
Five swatch "dots" in the top nav, each filled with its theme's representative color; the active one
is ringed. Accessible: real `<button>` elements with `aria-label`, keyboard focus, and `aria-pressed`
on the active theme.

### Decision 6: A small `components/ui/` primitive set, then restyle screen-by-screen
Build `Button`, `Input`, `Select`, `Textarea`, `Card`, `PageHeader`, and `Alert` as thin React
components wrapping native elements with token-bound Tailwind classes. Restyle each screen to use the
primitives and token utilities instead of inline styles.
- Alternative: raw utilities everywhere with no primitives. Rejected — repetition and visual drift
  across ~12 screens.

### Decision 7: Visual-only migration
No screen's logic, routing, state, or API calls change — only markup and classes. The dead scaffold
CSS is removed, and the HomePage stub becomes a real landing (presentational + existing links, no
new data).

## Risks / Trade-offs

- **A large diff touching every screen** → Order the work foundation → theme engine → primitives →
  shell → screen-by-screen; each screen is an isolated, verifiable step. One change, reviewable in
  groups.
- **Theme flash on load** → Pre-paint inline script sets `data-theme` before React mounts
  (Decision 4).
- **Utilities not following the active theme** → Use `@theme inline` so utilities reference runtime
  CSS vars, not baked values; verify a non-default theme recolors all surfaces.
- **Accidental behavior regression while restyling** → Visual-only rule; keep handlers/props intact;
  verify each screen's actions after restyle (`tsc` build + manual pass).
- **Hardcoded colors lingering somewhere** → After migration, grep for hex / `crimson` / `#` in
  components; every theme-related color must go through tokens.
- **No frontend test runner** → Rely on the strict `tsc` build and a manual visual pass across
  screens × themes; accepted (adding a runner is out of scope).

## Open Questions

- Exact palette hex values may be tuned against a real screen in daylight during implementation; the
  five theme identities themselves are fixed.
