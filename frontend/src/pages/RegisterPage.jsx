import { Loader2 } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import AuthCard from '../components/auth/AuthCard'
import Layout from '../components/Layout'
import { useAuth } from '../context/AuthContext'
import {
  extractErrorMessage,
  validateEmail,
  validatePassword,
} from '../utils/validation'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { setAuth } = useAuth()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [fieldErrors, setFieldErrors] = useState({})
  const [apiError, setApiError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setApiError(null)

    const emailError = validateEmail(email)
    const passwordError = validatePassword(password)
    const confirmError =
      password !== confirmPassword ? 'Passwords do not match' : null

    const errors = {
      email: emailError ?? undefined,
      password: passwordError ?? undefined,
      confirmPassword: confirmError ?? undefined,
    }

    setFieldErrors(errors)
    if (emailError || passwordError || confirmError) {
      return
    }

    setIsSubmitting(true)
    try {
      const response = await register({ email: email.trim(), password })
      setAuth(response)
      navigate('/assessment')
    } catch (error) {
      setApiError(extractErrorMessage(error, 'Unable to create account. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Layout>
      <AuthCard
        title="Create your account"
        subtitle="Get started with your personalized career role assessment."
        footerText="Already have an account?"
        footerLinkText="Sign in"
        footerLinkTo="/login"
        maxWidth="max-w-md"
      >
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          {apiError && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-300">
              {apiError}
            </div>
          )}

          <div>
            <label
              htmlFor="email"
              className="mb-1.5 block text-xs font-medium text-slate-700 dark:text-slate-300"
            >
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
              <p className="mt-1 text-xs text-rose-500 dark:text-rose-400">{fieldErrors.email}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="password"
              className="mb-1.5 block text-xs font-medium text-slate-700 dark:text-slate-300"
            >
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="input-field"
              placeholder="At least 8 characters"
            />
            {fieldErrors.password && (
              <p className="mt-1 text-xs text-rose-500 dark:text-rose-400">{fieldErrors.password}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="confirmPassword"
              className="mb-1.5 block text-xs font-medium text-slate-700 dark:text-slate-300"
            >
              Confirm password
            </label>
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              className="input-field"
              placeholder="Repeat your password"
            />
            {fieldErrors.confirmPassword && (
              <p className="mt-1 text-xs text-rose-500 dark:text-rose-400">
                {fieldErrors.confirmPassword}
              </p>
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
                Creating account…
              </span>
            ) : (
              'Create account'
            )}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-slate-500 dark:text-slate-400">
          By signing up, you agree to complete the assessment honestly.
        </p>
      </AuthCard>
    </Layout>
  )
}
