import { ArrowRight, Award, Compass, RotateCcw } from 'lucide-react'
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
        <div className="surface-card mx-auto max-w-lg p-10 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-600/10 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400">
            <Compass className="h-6 w-6" strokeWidth={2.2} />
          </div>
          <h1 className="text-lg font-semibold text-slate-900 dark:text-white">No evaluation results yet</h1>
          <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
            Complete the interactive assessment to generate your personalized job role analysis.
          </p>
          <Link
            to="/assessment"
            className="btn-primary mt-6 gap-1.5 text-xs"
          >
            Start Assessment
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="mx-auto w-full max-w-3xl space-y-6">
        {/* Main Predicted Role Hero Card */}
        <div className="surface-card overflow-hidden">
          <div className="border-b border-slate-200/80 bg-slate-50/80 px-6 py-10 text-center dark:border-slate-800/80 dark:bg-slate-900/50 sm:px-10 sm:py-12">
            <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200/80 bg-indigo-50 px-3 py-1 text-[11px] font-semibold text-indigo-700 dark:border-indigo-900/60 dark:bg-indigo-950/60 dark:text-indigo-300">
              <Award className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
              Primary AI Prediction
            </div>

            <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl lg:text-4xl dark:text-white">
              {results.predicted_role}
            </h1>

            <div className="mt-5 inline-flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50/80 px-4 py-1.5 dark:border-emerald-900/60 dark:bg-emerald-950/40">
              <span className="text-xs font-medium text-emerald-800 dark:text-emerald-300">
                Match Confidence
              </span>
              <span className="rounded-lg bg-emerald-600 px-2 py-0.5 text-xs font-bold text-white dark:bg-emerald-500">
                {formatPercent(results.confidence)}
              </span>
            </div>
          </div>

          {/* Alternate Roles List */}
          {results.alternates && results.alternates.length > 0 && (
            <div className="px-6 py-6 sm:px-10 sm:py-8">
              <h2 className="mb-4 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Alternative Career Matches
              </h2>
              <div className="space-y-4">
                {results.alternates.map((alternate) => (
                  <div
                    key={alternate.role}
                    className="rounded-xl border border-slate-200/70 bg-slate-50/60 p-4 transition hover:border-slate-300 dark:border-slate-800/80 dark:bg-slate-950/30 dark:hover:border-slate-700"
                  >
                    <div className="mb-2 flex items-center justify-between gap-4">
                      <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                        {alternate.role}
                      </span>
                      <span className="text-xs font-medium text-slate-600 dark:text-slate-400">
                        {formatPercent(alternate.confidence)}
                      </span>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                      <div
                        className="h-full rounded-full bg-indigo-600 transition-all duration-500 ease-out dark:bg-indigo-500"
                        style={{ width: `${alternate.confidence * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col items-center justify-center gap-3 sm:flex-row">
          <button
            type="button"
            onClick={() => navigate('/assessment')}
            className="btn-primary w-full gap-1.5 text-xs sm:w-auto"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            Retake Assessment
          </button>
          <Link
            to="/login"
            className="btn-secondary w-full text-xs sm:w-auto"
          >
            Back to Sign In
          </Link>
        </div>
      </div>
    </Layout>
  )
}
