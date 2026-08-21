import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import AuthCard from '../components/AuthCard'
import Layout from '../components/Layout'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage, validateEmail, validatePassword } from '../utils/validation'

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
        subtitle="Register to discover your best-fit job role."
        footerText="Already have an account?"
        footerLinkText="Sign in"
        footerLinkTo="/login"
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
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white placeholder:text-slate-500 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-400/30"
              placeholder="At least 8 characters"
            />
            {fieldErrors.password && (
              <p className="mt-1.5 text-sm text-rose-400">{fieldErrors.password}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="confirmPassword"
              className="mb-1.5 block text-sm font-medium text-slate-300"
            >
              Confirm password
            </label>
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              className="w-full rounded-xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white placeholder:text-slate-500 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-400/30"
              placeholder="Repeat your password"
            />
            {fieldErrors.confirmPassword && (
              <p className="mt-1.5 text-sm text-rose-400">{fieldErrors.confirmPassword}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-xl bg-indigo-500 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
      </AuthCard>
    </Layout>
  )
}
