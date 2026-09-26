import { createContext, useContext, useState, ReactNode } from 'react'
import { api } from '../api/client'
import { User } from '../types'

interface AuthContextType {
  user: User | null
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  function persist(token: string, u: User) {
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(u))
    setUser(u)
  }

  async function login(email: string, password: string) {
    const { data } = await api.post('/auth/login', { email, password })
    persist(data.token, { userId: data.userId, fullName: data.fullName, email: data.email })
  }

  async function register(fullName: string, email: string, password: string) {
    const { data } = await api.post('/auth/register', { fullName, email, password })
    persist(data.token, { userId: data.userId, fullName: data.fullName, email: data.email })
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
