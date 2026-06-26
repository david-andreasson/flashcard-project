## 1. Tailwind v4 setup

- [x] 1.1 Add `tailwindcss` + `@tailwindcss/vite` and register the plugin in `vite.config.ts`.
- [x] 1.2 Replace `src/style.css` with `@import "tailwindcss";` plus the token/theme layer; delete the dead Vite-scaffold CSS.

## 2. Theme tokens & palettes

- [x] 2.1 Define semantic CSS variables (`--color-page`, `--color-surface`, `--color-ink`, `--color-muted`, `--color-line`, `--color-accent`, `--color-accent-fg`) registered with `@theme` so Tailwind emits matching utilities.
- [x] 2.2 Define the five themes as `:root[data-theme="…"]` blocks — light, dark, pink, green, brown — with calm, coordinated palettes; surfaces a soft tint of the page hue.
- [x] 2.3 Base element styles bound to tokens (body background/text, headings, links, color-scheme per theme).

## 3. Theme engine (default, persist, switch)

- [x] 3.1 Pre-paint inline script in `index.html`: set `<html data-theme>` from `localStorage`, else the default brown theme, before React mounts (no flash).
- [x] 3.2 Theme module/hook (`lib/theme.ts`): expose the current theme + a setter that writes `localStorage` and updates `<html data-theme>`.
- [x] 3.3 Theme switcher in the nav: five accessible swatch buttons (`aria-label`, `aria-pressed`, keyboard focus); the active theme is ringed.

## 4. UI primitives (`components/ui.tsx`)

- [x] 4.1 `Button` (primary/secondary/ghost), `Input`, `Select`, `Textarea`.
- [x] 4.2 `Card`, `PageHeader`, `Alert`/notice.

## 5. App shell & screens (restyle, visual only)

- [x] 5.1 `RootLayout`: real app header (logo, nav links with active state, user/logout, theme switcher).
- [x] 5.2 Auth screens: `LoginPage`, `RegisterPage`.
- [x] 5.3 `HomePage`: replace the stub with a real landing/dashboard (existing links only, no new data).
- [x] 5.4 Courses: `CoursesPage`, `CourseDetailPage`, `DeckCardsPage`.
- [x] 5.5 Study: `StudyPage`, `ReviewPage`, `ProgressPage`, `StudyHistoryPage`.
- [x] 5.6 `AiGeneratePage` (including the inline create-course/deck save row).
- [x] 5.7 `NotFoundPage` (and the `ProtectedRoute` loading state).

## 6. Cleanup & docs

- [x] 6.1 Remove remaining inline styles / hardcoded theme colors from components (only the theme switcher's intentional swatch colors remain).
- [x] 6.2 Update `docs/ROADMAP.md`: 10 = inline-deck-course-creation (Done), 11 = frontend-facelift (this change), 12 = aws-deployment (deferred — no budget); update the dependency tree.

## 7. Verification

- [x] 7.1 `npm run build` (strict `tsc`) is clean.
- [x] 7.2 Manual: every screen is restyled and visually consistent; no off-theme hardcoded colors remain.
- [x] 7.3 Switching themes recolors the whole app; the choice persists across a reload; first visit defaults to brown; no flash on load. (Verified in-browser: themes recolor body + surfaces; localStorage persists across reload; pre-paint sets `data-theme` before mount.)
- [x] 7.4 Manual: existing actions still work on restyled screens (create a course, generate + save AI cards incl. a new inline deck, study and grade a card).

## 8. Polish pass (review feedback)

- [x] 8.1 Switch the app font to Inter and add soft per-theme elevation (`--shadow`) on surfaces (cards, nav).
- [x] 8.2 Sticky app header with elevation; rename the "AI Cards" nav item to "Generate cards".
- [x] 8.3 Silence the React Router v7 `startTransition` warning via `RouterProvider future`.
- [x] 8.4 Backend: per-course deck/card counts on the course list (`CourseResponse` + grouped queries); shown on the redesigned Courses card grid.
- [x] 8.5 Restyle Courses as a card grid; wrap Login/Register in cards.
- [x] 8.6 Re-verify: `npm run build` and `./mvnw test` (63 tests) both green.
- [x] 8.7 Default the app to the soft brown theme; deck click goes straight to Study (with an Edit/Cards link to the card management); fix the Home tile label "AI Cards" → "Generate cards".
- [x] 8.8 Add a "Back to course" link in the active Study and Review views so a mis-picked deck is easy to leave.
