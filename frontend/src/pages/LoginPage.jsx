import { Loader2 } from 'lucide-react'
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
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          {apiError && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-300">
              {apiError}
            </div>
          )}

          <div>
            <label htmlFor="email" className="mb-1.5 block text-xs font-medium text-slate-700 dark:text-slate-300">
              Email address
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="input-field"
              placeholder="you@example.com"
            />
            {fieldErrors.email && (
              <p className="mt-1.5 text-xs text-rose-500 dark:text-rose-400">{fieldErrors.email}</p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="mb-1.5 block text-xs font-medium text-slate-700 dark:text-slate-300">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="input-field"
              placeholder="Enter your password"
            />
            {fieldErrors.password && (
              <p className="mt-1.5 text-xs text-rose-500 dark:text-rose-400">{fieldErrors.password}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="btn-primary w-full py-3"
          >
            {isSubmitting ? (
              <span className="flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin" />
                Signing in…
              </span>
            ) : (
              'Sign in'
            )}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-slate-400 dark:text-slate-500">
          Protected by end-to-end secure session authentication.
        </p>
      </AuthCard>
    </Layout>
  )
}
