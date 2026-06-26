## ADDED Requirements

### Requirement: Token-driven design system across all screens
The frontend SHALL style every screen and the app shell through a shared, token-driven design system
(Tailwind v4 utilities bound to semantic theme tokens), rather than ad-hoc per-component hardcoded
colors. Theme-dependent colors SHALL come from the tokens so a theme change recolors all screens
consistently.

#### Scenario: A theme change recolors every screen
- **WHEN** the active theme changes
- **THEN** every screen and the nav reflect the new theme consistently, with no screen keeping a
  hardcoded off-theme color

#### Scenario: Reusable primitives back the common controls
- **WHEN** a screen renders a button, input, select, textarea, card, page header, or alert
- **THEN** it uses the shared UI primitive so the styling is consistent across screens

### Requirement: Restyle preserves existing behavior
The facelift SHALL be visual only: existing routes, actions, state, and API calls SHALL keep working
exactly as before.

#### Scenario: Existing actions still work after restyle
- **WHEN** the user performs an existing action on a restyled screen (for example creating a course,
  generating AI cards, or grading a card)
- **THEN** it behaves exactly as it did before the facelift
