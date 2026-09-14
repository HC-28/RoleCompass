import { ArrowRight, Award, Compass, RotateCcw, ShieldOff, XCircle } from 'lucide-react'
import { useMemo } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'

const RESULTS_KEY = 'rolecompass_results'

function formatPercent(value) {
  return `${Math.round(value * 100)}%`
}

/**
 * Maps the backend stage key to a user-facing label and badge style.
 * PSYCHOMETRIC = interest / personality pass (Section 1 gate)
 * TECHNICAL    = skills interest pass (Section 3 gate)
 */
const STAGE_META = {
  PSYCHOMETRIC: {
    label: 'Personality Match',
    badgeClass:
      'bg-violet-50 text-violet-700 border-violet-200 ' +
      'dark:bg-violet-950/60 dark:text-violet-300 dark:border-violet-900/60',
  },
  TECHNICAL: {
    label: 'Skills Check',
    badgeClass:
      'bg-amber-50 text-amber-700 border-amber-200 ' +
      'dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-900/60',
  },
}

export default function ResultsPage() {
  const location = useLocation()
  const navigate = useNavigate()

  const results = useMemo(() => {
    const fromState = location.state?.results
    if (fromState) return fromState

    const stored = sessionStorage.getItem(RESULTS_KEY)
    if (stored) {
      try { return JSON.parse(stored) } catch { return null }
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
          <h1 className="text-lg font-semibold text-slate-900 dark:text-white">
            No evaluation results yet
          </h1>
          <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
            Complete the interactive assessment to generate your personalized job role analysis.
          </p>
          <Link to="/assessment" className="btn-primary mt-6 gap-1.5 text-xs">
            Start Assessment
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </Layout>
    )
  }

  const eliminatedRoles = results.eliminated_roles ?? []
  const isFallback = results.is_fallback === true

  return (
    <Layout>
      <div className="mx-auto w-full max-w-3xl space-y-6">

        {/* ── Fallback Warning Banner ─────────────────────────────────────── */}
        {isFallback && (
          <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 dark:border-amber-900/60 dark:bg-amber-950/40">
            <span className="mt-0.5 text-lg">⚠️</span>
            <div>
              <p className="text-xs font-semibold text-amber-800 dark:text-amber-300">
                Prediction service was temporarily unavailable
              </p>
              <p className="mt-0.5 text-xs text-amber-700 dark:text-amber-400">
                The AI model could not be reached. This result is a best-guess estimate based
                on your surviving role candidates — not a full trained-model prediction.
                Retake the assessment when the service is back online for an accurate result.
              </p>
            </div>
          </div>
        )}

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

          {/* Alternate Roles */}
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

        {/* ── How We Narrowed It Down ─────────────────────────────────────── */}
        {eliminatedRoles.length > 0 && (
          <div className="surface-card overflow-hidden">

            {/* Section header */}
            <div className="border-b border-slate-200/80 px-6 py-5 sm:px-8 dark:border-slate-800/80">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-rose-50 text-rose-500 dark:bg-rose-950/60 dark:text-rose-400">
                  <ShieldOff className="h-4 w-4" />
                </div>
                <div>
                  <h2 className="text-sm font-semibold text-slate-900 dark:text-white">
                    How we narrowed it down
                  </h2>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">
                    Roles ruled out by the adaptive routing system before the AI made its final prediction
                  </p>
                </div>
              </div>
            </div>

            {/* One card per eliminated role */}
            <div className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {eliminatedRoles.map((item) => {
                const meta = STAGE_META[item.stage] ?? STAGE_META.PSYCHOMETRIC
                return (
                  <div key={item.role} className="px-6 py-5 sm:px-8">
                    {/* Role name + stage badge row */}
                    <div className="flex flex-wrap items-center gap-2.5">
                      <XCircle className="h-4 w-4 shrink-0 text-rose-500 dark:text-rose-400" />
                      <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                        {item.role}
                      </span>
                      <span
                        className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold ${meta.badgeClass}`}
                      >
                        {meta.label}
                      </span>
                    </div>

                    {/* Plain-English reason — no jargon */}
                    <p className="mt-2.5 pl-6 text-xs leading-relaxed text-slate-600 dark:text-slate-400">
                      {item.reason}
                    </p>
                  </div>
                )
              })}
            </div>

            {/* Legend footer */}
            <div className="border-t border-slate-100 bg-slate-50/60 px-6 py-3 dark:border-slate-800/60 dark:bg-slate-900/30">
              <p className="text-[11px] text-slate-400 dark:text-slate-500">
                <span className="font-semibold text-violet-600 dark:text-violet-400">
                  Personality Match
                </span>{' '}
                — based on your work-style and interest preferences.{' '}
                <span className="font-semibold text-amber-600 dark:text-amber-400">
                  Skills Check
                </span>{' '}
                — based on your answers to technical topic questions.
              </p>
            </div>
          </div>
        )}

        {/* ── Actions ────────────────────────────────────────────────────────── */}
        <div className="flex flex-col items-center justify-center gap-3 sm:flex-row">
          <button
            type="button"
            onClick={() => navigate('/assessment')}
            className="btn-primary w-full gap-1.5 text-xs sm:w-auto"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            Retake Assessment
          </button>
          <Link to="/login" className="btn-secondary w-full text-xs sm:w-auto">
            Back to Sign In
          </Link>
        </div>

      </div>
    </Layout>
  )
}
