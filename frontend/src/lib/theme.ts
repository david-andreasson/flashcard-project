import { useCallback, useState } from 'react'

export const THEMES = ['light', 'dark', 'pink', 'green', 'brown'] as const
export type Theme = (typeof THEMES)[number]

export const THEME_LABELS: Record<Theme, string> = {
  light: 'Light',
  dark: 'Dark',
  pink: 'Soft pink',
  green: 'Soft green',
  brown: 'Soft brown',
}

// Representative swatch colour for each theme's dot in the switcher.
export const THEME_SWATCH: Record<Theme, string> = {
  light: '#ffffff',
  dark: '#2b2d36',
  pink: '#ecc4cf',
  green: '#cfe0c6',
  brown: '#e4d4b8',
}

const STORAGE_KEY = 'theme'

function isTheme(value: string | null): value is Theme {
  return value !== null && (THEMES as readonly string[]).includes(value)
}

/** The theme currently on <html>, set by the pre-paint script in index.html. */
export function currentTheme(): Theme {
  const attr = document.documentElement.getAttribute('data-theme')
  return isTheme(attr) ? attr : 'brown'
}

export function applyTheme(theme: Theme): void {
  document.documentElement.setAttribute('data-theme', theme)
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // ignore unavailable storage
  }
}

export function useTheme(): [Theme, (theme: Theme) => void] {
  const [theme, setThemeState] = useState<Theme>(() => currentTheme())
  const setTheme = useCallback((next: Theme) => {
    applyTheme(next)
    setThemeState(next)
  }, [])
  return [theme, setTheme]
}
