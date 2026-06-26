# frontend-theming Specification

## Purpose
TBD - created by archiving change frontend-facelift. Update Purpose after archive.
## Requirements
### Requirement: Selectable whole-app themes
The frontend SHALL offer five themes — dark, light, soft pink, soft green, and soft brown — chosen
from a switcher in the top navigation. Selecting a theme SHALL recolor the entire app (page
background, surfaces, text, and accent), not just isolated elements.

#### Scenario: User switches theme from the nav
- **WHEN** the user selects a different theme from the nav switcher
- **THEN** the whole app re-renders in that theme — background, surfaces, text, and accent all change

#### Scenario: Five themes are available
- **WHEN** the user opens the theme switcher
- **THEN** dark, light, soft pink, soft green, and soft brown are all selectable

### Requirement: Theme choice persists across sessions
The selected theme SHALL be remembered across reloads and future visits on the same browser.

#### Scenario: Theme survives a reload
- **WHEN** the user selects a theme and then reloads the app
- **THEN** the previously selected theme is still active

### Requirement: Default theme on first visit
On the first visit (no stored preference) the app SHALL default to the soft brown theme.

#### Scenario: First visit with no stored preference
- **WHEN** a user with no stored theme preference opens the app
- **THEN** the app starts in the soft brown theme

### Requirement: Theme applies without a flash on load
The active theme SHALL be applied before the first paint so the page never flashes a different theme
on load.

#### Scenario: Stored non-default theme on reload
- **WHEN** the app loads with a stored theme that differs from the default
- **THEN** the page renders in the stored theme from the first paint, with no flash of another theme

