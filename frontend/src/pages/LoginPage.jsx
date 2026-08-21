import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import AuthCard from '../components/AuthCard'
import Layout from '../components/Layout'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage, validateEmail, validatePassword } from '../utils/validation'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth } = useAuth()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [apiError, setApiError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setApiError(null)

    const emailError = validateEmail(email)
    const passwordError = validatePassword(password)
    const errors = { email: emailError ?? undefined, password: passwordError ?? undefined }

    setFieldErrors(errors)
    if (emailError || passwordError) {
      return
    }

    setIsSubmitting(true)
    try {
      const response = await login({ email: email.trim(), password })
      setAuth(response)
      navigate('/assessment')
    } catch (error) {
      setApiError(extractErrorMessage(error, 'Unable to sign in. Check your credentials.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Layout>
      <AuthCard
        title="Welcome back"
        subtitle="Sign in to continue your career assessment."
        footerText="Don't have an account?"
        footerLinkText="Create one"
        footerLinkTo="/register"
      >
        <form onSubmit={handleSubmit} className="space-y-5" noValidate>
          {apiError && (
            <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              {apiError}
            </div>
          )}

          <div>
            <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-slate-300">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="w-full rounded-xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white placeholder:text-slate-500 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-400/30"
              placeholder="you@example.com"
            />
            {fieldErrors.email && (
              <p className="mt-1.5 text-sm text-rose-400">{fieldErrors.email}</p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-slate-300">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white placeholder:text-slate-500 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-400/30"
              placeholder="At least 8 characters"
            />
            {fieldErrors.password && (
              <p className="mt-1.5 text-sm text-rose-400">{fieldErrors.password}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-xl bg-indigo-500 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-slate-500">
          By continuing, you agree to complete the RoleCompass assessment honestly.
        </p>
      </AuthCard>

      <p className="mt-6 text-center text-sm text-slate-500">
        <Link to="/register" className="text-indigo-400 hover:text-indigo-300">
          New here? Start with registration
        </Link>
      </p>
    </Layout>
  )
}
