import { Compass, LogOut, Menu, Moon, Sparkles, Sun, User, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

export default function Header() {
  const { isAuthenticated, email, logout } = useAuth()
  const { isDark, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    setMenuOpen(false)
    logout()
    navigate('/login')
  }

  const isRouteActive = (path) => location.pathname === path

  return (
    <header className="fixed inset-x-0 top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur-md dark:border-slate-800 dark:bg-slate-950/90">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        {/* Brand */}
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-white">
              <Compass className="h-5 w-5" />
            </div>
            <span>RoleCompass</span>
          </Link>

          {/* Desktop Nav */}
          {isAuthenticated && (
            <nav className="hidden md:flex items-center gap-1">
              <Link
                to="/assessment"
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                  isRouteActive('/assessment')
                    ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300'
                    : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
                }`}
              >
                <Sparkles className="h-3.5 w-3.5" />
                Assessment
              </Link>
              <Link
                to="/profile"
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                  isRouteActive('/profile')
                    ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300'
                    : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
                }`}
              >
                <User className="h-3.5 w-3.5" />
                Profile
              </Link>
            </nav>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2">
          {isAuthenticated && (
            <div className="hidden sm:flex items-center gap-2">
              <span className="max-w-[160px] truncate text-xs text-slate-600 dark:text-slate-400">
                {email}
              </span>
              <button
                type="button"
                onClick={handleLogout}
                className="btn-secondary h-8 px-2.5 text-xs"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span>Sign out</span>
              </button>
            </div>
          )}

          {/* Theme Toggle */}
          <button
            type="button"
            onClick={toggleTheme}
            aria-label="Toggle theme"
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 dark:border-slate-800 dark:text-slate-300 dark:hover:bg-slate-900"
          >
            {isDark ? <Sun className="h-4 w-4 text-amber-400" /> : <Moon className="h-4 w-4" />}
          </button>

          {/* Mobile Menu Toggle */}
          {isAuthenticated && (
            <button
              type="button"
              onClick={() => setMenuOpen(!menuOpen)}
              className="flex md:hidden h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 dark:border-slate-800 dark:text-slate-300"
            >
              {menuOpen ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
            </button>
          )}
        </div>
      </div>

      {/* Mobile Nav */}
      {isAuthenticated && menuOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white px-4 py-3 dark:border-slate-800 dark:bg-slate-950 space-y-1">
          <Link
            to="/assessment"
            onClick={() => setMenuOpen(false)}
            className="block rounded-lg px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900"
          >
            Assessment
          </Link>
          <Link
            to="/profile"
            onClick={() => setMenuOpen(false)}
            className="block rounded-lg px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900"
          >
            Profile
          </Link>
          <button
            type="button"
            onClick={handleLogout}
            className="w-full text-left rounded-lg px-3 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950/30"
          >
            Sign Out
          </button>
        </div>
      )}
    </header>
  )
}
