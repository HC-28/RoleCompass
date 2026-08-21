import { useMemo } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'

const RESULTS_KEY = 'rolecompass_results'

function formatPercent(value) {
  return `${Math.round(value * 100)}%`
}

export default function ResultsPage() {
  const location = useLocation()
  const navigate = useNavigate()

  const results = useMemo(() => {
    const fromState = location.state?.results
    if (fromState) {
      return fromState
    }

    const stored = sessionStorage.getItem(RESULTS_KEY)
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        return null
      }
    }

    return null
  }, [location.state])

  if (!results) {
    return (
      <Layout>
        <div className="mx-auto max-w-2xl rounded-3xl border border-white/10 bg-slate-900/70 p-10 text-center backdrop-blur-xl">
          <h1 className="text-xl font-semibold text-white">No results yet</h1>
          <p className="mt-3 text-slate-400">
            Complete the assessment to see your predicted job role.
          </p>
          <Link
            to="/assessment"
            className="mt-6 inline-flex rounded-xl bg-indigo-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-400"
          >
            Start assessment
          </Link>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="mx-auto max-w-3xl space-y-8">
        <div className="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/70 shadow-2xl shadow-black/30 backdrop-blur-xl">
          <div className="border-b border-white/10 bg-gradient-to-r from-indigo-500/15 via-violet-500/10 to-emerald-500/10 px-8 py-10 text-center">
            <p className="text-xs font-semibold uppercase tracking-widest text-indigo-300">
              Your predicted role
            </p>
            <h1 className="mt-4 text-3xl font-bold tracking-tight text-white sm:text-4xl">
              {results.predicted_role}
            </h1>
            <div className="mt-6 inline-flex items-center gap-2 rounded-full border border-emerald-400/30 bg-emerald-500/15 px-5 py-2">
              <span className="text-sm text-emerald-200">Match confidence</span>
              <span className="rounded-full bg-emerald-500 px-3 py-0.5 text-sm font-bold text-white">
                {formatPercent(results.confidence)}
              </span>
            </div>
          </div>

          {results.alternates.length > 0 && (
            <div className="px-8 py-8">
              <h2 className="mb-6 text-sm font-semibold uppercase tracking-widest text-slate-400">
                Alternate roles
              </h2>
              <div className="space-y-5">
                {results.alternates.map((alternate) => (
                  <div key={alternate.role}>
                    <div className="mb-2 flex items-center justify-between gap-4">
                      <span className="text-sm font-medium text-slate-200">{alternate.role}</span>
                      <span className="text-sm text-slate-400">
                        {formatPercent(alternate.confidence)}
                      </span>
                    </div>
                    <div className="h-3 overflow-hidden rounded-full bg-slate-800">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-violet-500 transition-all duration-500"
                        style={{ width: `${alternate.confidence * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
          <button
            type="button"
            onClick={() => navigate('/assessment')}
            className="rounded-xl bg-indigo-500 px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:bg-indigo-400"
          >
            Retake assessment
          </button>
          <Link
            to="/login"
            className="rounded-xl border border-white/10 px-6 py-3 text-center text-sm font-medium text-slate-300 transition hover:bg-white/5"
          >
            Back to home
          </Link>
        </div>
      </div>
    </Layout>
  )
}
