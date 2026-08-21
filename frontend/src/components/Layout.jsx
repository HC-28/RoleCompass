import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Layout({ children }) {
  const { isAuthenticated, email, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950">
      <header className="border-b border-white/10 bg-slate-950/40 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500 text-sm font-bold text-white shadow-lg shadow-indigo-500/30">
              RC
            </span>
            <span className="text-lg font-semibold tracking-tight text-white">
              RoleCompass
            </span>
          </Link>

          {isAuthenticated && (
            <div className="flex items-center gap-4">
              <span className="hidden text-sm text-slate-400 sm:inline">{email}</span>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-lg border border-white/10 px-3 py-1.5 text-sm text-slate-300 transition hover:border-white/20 hover:bg-white/5 hover:text-white"
              >
                Sign out
              </button>
            </div>
          )}
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 sm:py-12">{children}</main>
    </div>
  )
}
