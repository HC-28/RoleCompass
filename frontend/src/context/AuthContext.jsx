import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { clearStoredToken, getStoredToken, setStoredToken } from '../api/client'

const AuthContext = createContext(null)

const EMAIL_KEY = 'rolecompass_email'

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => getStoredToken())
  const [email, setEmail] = useState(() => sessionStorage.getItem(EMAIL_KEY))

  const setAuth = useCallback((response) => {
    setStoredToken(response.token)
    sessionStorage.setItem(EMAIL_KEY, response.email)
    setToken(response.token)
    setEmail(response.email)
  }, [])

  const logout = useCallback(() => {
    clearStoredToken()
    sessionStorage.removeItem(EMAIL_KEY)
    setToken(null)
    setEmail(null)
  }, [])

  const value = useMemo(
    () => ({
      token,
      email,
      isAuthenticated: Boolean(token),
      setAuth,
      logout,
    }),
    [token, email, setAuth, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
