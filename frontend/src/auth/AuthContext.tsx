import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { apiFetch, errorMessage } from '../lib/api'

export type Role = 'USER' | 'ADMIN'
export type Plan = 'FREE' | 'PREMIUM'

export interface User {
  id: number
  email: string
  role: Role
  plan: Plan
}

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  // On mount, check for an existing session. This GET also bootstraps the
  // XSRF-TOKEN cookie that later state-changing requests need.
  useEffect(() => {
    let active = true
    apiFetch('/auth/me')
      .then(async (res) => {
        if (active) {
          setUser(res.ok ? ((await res.json()) as User) : null)
        }
      })
      .catch(() => {
        if (active) setUser(null)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  async function submitCredentials(path: string, email: string, password: string) {
    const res = await apiFetch(path, {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) {
      throw new Error(await errorMessage(res))
    }
    setUser((await res.json()) as User)
  }

  const login = (email: string, password: string) =>
    submitCredentials('/auth/login', email, password)

  const register = (email: string, password: string) =>
    submitCredentials('/auth/register', email, password)

  async function logout() {
    await apiFetch('/auth/logout', { method: 'POST' })
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
