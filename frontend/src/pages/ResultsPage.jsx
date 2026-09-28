import { ArrowRight, Compass, RotateCcw, ShieldAlert, Sparkles, User, XCircle } from 'lucide-react'
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
    if (fromState) return fromState

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
        <div className="surface-card mx-auto max-w-md p-8 text-center space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400">
            <Compass className="h-6 w-6" />
          </div>
          <h1 className="text-lg font-bold text-slate-900 dark:text-white">
            No Results Found
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Please complete an assessment session to view results.
          </p>
          <div className="pt-2">
            <Link to="/assessment" className="btn-primary gap-2 text-xs px-5 py-2">
              <span>Start Assessment</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        </div>
      </Layout>
    )
  }

  const eliminatedRoles = results.eliminated_roles ?? []
  const isFallback = results.is_fallback === true || results.fallback === true

  // Check if any alternate has higher raw confidence than the recommended role
  const hasHigherAlternate = results.alternates?.some((alt) => alt.confidence > (results.confidence ?? 0))

  return (
    <Layout>
      <div className="mx-auto w-full max-w-2xl space-y-5">
        {/* Fallback Notice */}
        {isFallback && (
          <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/40 dark:text-amber-300">
            <ShieldAlert className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
            <div>
              <p className="font-semibold">Local Fallback Prediction Mode</p>
              <p>The ML FastAPI service was offline during inference. Result calculated using candidate heuristics.</p>
            </div>
          </div>
        )}

        {/* Prediction Card */}
        <div className="surface-card relative overflow-hidden p-7 text-center space-y-5 border-indigo-500/30 dark:border-indigo-500/20 shadow-xl">
          {/* Subtle top glow bar */}
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-cyan-400" />

          <div className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-3.5 py-1 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 border border-indigo-200/50 dark:border-indigo-800/50">
            <Sparkles className="h-3.5 w-3.5 text-indigo-500 animate-pulse" />
            <span>Top Recommended Fit</span>
          </div>

          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-slate-900 dark:text-white">
            {results.predicted_role}
          </h1>

          <div className="flex flex-col items-center justify-center gap-2">
            <div className="inline-flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-2 text-sm text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300 border border-emerald-200/60 dark:border-emerald-800/40 shadow-sm">
              <span className="text-xs uppercase tracking-wider font-semibold text-emerald-600 dark:text-emerald-400">ML Confidence:</span>
              <span className="text-base font-black">{formatPercent(results.confidence)}</span>
            </div>

            {results.confidence < 0.35 && (
              <p className="text-[11px] text-slate-400 dark:text-slate-500 max-w-md">
                Distributed probability: Responses showed neutral or multi-disciplinary preferences across several roles.
              </p>
            )}
          </div>

          {/* Alternate Roles */}
          {results.alternates && results.alternates.length > 0 && (
            <div className="border-t border-slate-200/80 pt-5 text-left dark:border-slate-800/80 space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Alternative Role Probabilities
                </h2>
                <span className="text-[11px] text-slate-400">Global Classifier Distribution</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {results.alternates.map((alternate) => (
                  <div
                    key={alternate.role}
                    className="flex items-center justify-between rounded-xl border border-slate-200/70 bg-slate-50/50 p-3.5 text-xs dark:border-slate-800/60 dark:bg-slate-900/30 hover:border-indigo-300 dark:hover:border-indigo-800 transition"
                  >
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {alternate.role}
                    </span>
                    <span className="font-bold text-indigo-600 dark:text-indigo-400">
                      {formatPercent(alternate.confidence)}
                    </span>
                  </div>
                ))}
              </div>

              {/* Clarification when an alternate is higher than main prediction */}
              {hasHigherAlternate && (
                <div className="rounded-lg bg-slate-100/70 p-3 text-[11px] text-slate-600 dark:bg-slate-900/60 dark:text-slate-400 border border-slate-200/50 dark:border-slate-800/50 leading-relaxed">
                  <span className="font-semibold text-slate-700 dark:text-slate-300">Why does an alternative show higher raw probability?</span><br />
                  Alternative roles reflect raw unconstrained ML probabilities across all 10 IT occupations. Your recommended role ({results.predicted_role}) was chosen because incompatible roles were eliminated by your adaptive psychometric &amp; technical gate answers.
                </div>
              )}
            </div>
          )}
        </div>

        {/* Eliminated Roles */}
        {eliminatedRoles.length > 0 && (
          <div className="surface-card p-5 space-y-3">
            <h2 className="text-sm font-semibold text-slate-900 dark:text-white">
              Eliminated Roles
            </h2>
            <div className="divide-y divide-slate-100 dark:divide-slate-800 text-xs">
              {eliminatedRoles.map((item) => (
                <div key={item.role} className="py-2.5 space-y-1">
                  <div className="flex items-center gap-2">
                    <XCircle className="h-4 w-4 text-rose-500 shrink-0" />
                    <span className="font-semibold text-slate-800 dark:text-slate-200">
                      {item.role}
                    </span>
                    <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                      {item.stage === 'TECHNICAL' ? 'Skills' : 'Preferences'}
                    </span>
                  </div>
                  <p className="text-slate-500 dark:text-slate-400 pl-6">
                    {item.reason}
                  </p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex items-center justify-center gap-3 pt-2">
          <Link to="/profile" className="btn-secondary gap-1.5 text-xs">
            <User className="h-3.5 w-3.5" />
            <span>View Profile</span>
          </Link>

          <button
            type="button"
            onClick={() => {
              sessionStorage.removeItem(RESULTS_KEY)
              navigate('/assessment')
            }}
            className="btn-primary gap-1.5 text-xs"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Retake Assessment</span>
          </button>
        </div>
      </div>
    </Layout>
  )
}
