import { AlertTriangle, Compass, LogOut, Menu, Moon, Sparkles, Sun, User, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

/**
 * Header — top navigation bar.
 *
 * Props:
 *   isAssessmentActive {boolean} — when true, clicking the brand logo shows
 *                                  an exit-confirmation modal instead of navigating.
 *   onExitConfirm      {function} — called when the user confirms they want to exit.
 *                                   Should clean up any in-progress assessment state.
 */
export default function Header({ isAssessmentActive = false, onExitConfirm }) {
  const { isAuthenticated, email, logout } = useAuth()
  const { isDark, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const [showExitModal, setShowExitModal] = useState(false)

  const handleLogout = () => {
    setMenuOpen(false)
    logout()
    navigate('/login')
  }

  const isRouteActive = (path) => location.pathname === path

  // Called when user clicks the brand logo or the "RoleCompass" text
  const handleBrandClick = (e) => {
    if (isAssessmentActive) {
      e.preventDefault()
      setShowExitModal(true)
    }
    // else: default Link behaviour navigates to /
  }

  const handleExitConfirm = () => {
    setShowExitModal(false)
    if (onExitConfirm) onExitConfirm()
    navigate('/')
  }

  const handleExitCancel = () => {
    setShowExitModal(false)
  }

  return (
    <>
      <header className="fixed inset-x-0 top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur-md dark:border-slate-800 dark:bg-slate-950/90">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          {/* Brand */}
          <div className="flex items-center gap-6">
            <Link
              to="/"
              onClick={handleBrandClick}
              className="flex items-center gap-2 font-bold text-slate-900 dark:text-white"
            >
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

      {/* Exit Assessment Confirmation Modal */}
      {showExitModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          role="dialog"
          aria-modal="true"
          aria-labelledby="exit-modal-title"
        >
          <div className="mx-4 w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-slate-700 dark:bg-slate-900">
            {/* Icon */}
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 dark:bg-amber-950/50">
              <AlertTriangle className="h-6 w-6 text-amber-600 dark:text-amber-400" />
            </div>

            <h2 id="exit-modal-title" className="mb-2 text-base font-bold text-slate-900 dark:text-white">
              Exit Assessment?
            </h2>
            <p className="mb-6 text-sm text-slate-600 dark:text-slate-400">
              If you leave now, your current progress <span className="font-semibold text-slate-800 dark:text-slate-200">will not be submitted</span> and your answers won&apos;t be saved. You can restart the assessment anytime.
            </p>

            <div className="flex gap-3">
              <button
                id="exit-modal-cancel"
                type="button"
                onClick={handleExitCancel}
                className="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                Keep Going
              </button>
              <button
                id="exit-modal-confirm"
                type="button"
                onClick={handleExitConfirm}
                className="flex-1 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-rose-500"
              >
                Yes, Exit
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
