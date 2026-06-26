import { THEMES, THEME_LABELS, THEME_SWATCH, useTheme } from '../lib/theme'

export function ThemeSwitcher() {
  const [theme, setTheme] = useTheme()
  return (
    <div role="group" aria-label="Theme" className="flex items-center gap-2">
      {THEMES.map((t) => {
        const active = t === theme
        return (
          <button
            key={t}
            type="button"
            onClick={() => setTheme(t)}
            title={THEME_LABELS[t]}
            aria-label={THEME_LABELS[t]}
            aria-pressed={active}
            className={`h-4 w-4 rounded-full border border-line transition hover:scale-110 ${
              active ? 'ring-2 ring-accent ring-offset-2 ring-offset-page' : ''
            }`}
            style={{ backgroundColor: THEME_SWATCH[t] }}
          />
        )
      })}
    </div>
  )
}
