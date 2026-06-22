const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

/**
 * Fetch wrapper for the backend API.
 * - Prefixes the backend context path (`/api`) and any configured base URL.
 * - Always sends cookies (`credentials: 'include'`) so the auth cookies travel.
 * - On state-changing requests, echoes the `XSRF-TOKEN` cookie as the `X-XSRF-TOKEN`
 *   header (double-submit CSRF pattern).
 *
 * @param path API path beginning with `/`, e.g. `/auth/login`
 */
export async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (!SAFE_METHODS.includes(method)) {
    const csrf = readCookie('XSRF-TOKEN')
    if (csrf) {
      headers.set('X-XSRF-TOKEN', csrf)
    }
    // Don't force JSON for FormData uploads — the browser must set the multipart boundary itself.
    if (options.body !== undefined && !headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json')
    }
  }

  return fetch(`${BASE_URL}/api${path}`, {
    ...options,
    headers,
    credentials: 'include',
  })
}

/**
 * Extracts a human-readable error message from a failed API response.
 * Falls back to a generic message if the body is not a problem-detail JSON.
 */
export async function errorMessage(response: Response): Promise<string> {
  try {
    const data = await response.json()
    if (data && typeof data.detail === 'string') {
      return data.detail
    }
  } catch {
    // not JSON — fall through
  }
  return `Request failed (${response.status})`
}
