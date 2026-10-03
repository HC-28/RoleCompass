import {
  ArrowRight,
  Award,
  BookOpen,
  Brain,
  CheckCircle2,
  Compass,
  Copy,
  Cpu,
  Layers,
  Printer,
  RotateCcw,
  ShieldAlert,
  Sparkles,
  Terminal,
  User,
  XCircle,
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import { ALL_ROLES, ROLE_METADATA } from '../constants/assessment.constants'

const RESULTS_KEY = 'rolecompass_results'

function formatPercent(value) {
  if (value == null) return '0%'
  return `${Math.round(value * 100)}%`
}

function getConfidenceTier(confidence) {
  if (confidence >= 0.6) {
    return {
      label: 'Decisive Alignment',
      color: 'text-emerald-700 bg-emerald-50 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800/50',
      barColor: 'from-emerald-500 to-teal-400',
      description: 'The Random Forest ensemble reached strong consensus for this occupational profile.',
    }
  }
  if (confidence >= 0.35) {
    return {
      label: 'Solid Match',
      color: 'text-indigo-700 bg-indigo-50 border-indigo-200 dark:bg-indigo-950/60 dark:text-indigo-300 dark:border-indigo-800/50',
      barColor: 'from-indigo-500 to-cyan-400',
      description: 'Clear primary affinity, with balanced secondary competencies in related technical tracks.',
    }
  }
  return {
    label: 'Multi-Disciplinary Fit',
    color: 'text-amber-700 bg-amber-50 border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800/50',
    barColor: 'from-amber-500 to-orange-400',
    description: 'Ensemble probability is distributed across overlapping domains, demonstrating versatile interdisciplinary aptitude.',
  }
}

export default function ResultsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [eliminationFilter, setEliminationFilter] = useState('ALL')
  const [copied, setCopied] = useState(false)

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
            No Assessment Results Found
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Please complete an assessment session to generate your personalized IT role prediction.
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

  const predictedRole = results.predicted_role || 'Specialist Role'
  const confidence = results.confidence ?? 0
  const confidencePercent = Math.round(confidence * 100)
  const tier = getConfidenceTier(confidence)
  const eliminatedRoles = results.eliminated_roles ?? []
  const alternates = results.alternates ?? []
  const isFallback = results.is_fallback === true || results.fallback === true
  const roleMeta = ROLE_METADATA[predictedRole]

  // Filter eliminated roles based on user-selected tab
  const filteredEliminations = useMemo(() => {
    if (eliminationFilter === 'PSYCHOMETRIC') {
      return eliminatedRoles.filter((item) => item.stage === 'PSYCHOMETRIC')
    }
    if (eliminationFilter === 'TECHNICAL') {
      return eliminatedRoles.filter((item) => item.stage === 'TECHNICAL')
    }
    return eliminatedRoles
  }, [eliminatedRoles, eliminationFilter])

  // Count by gate
  const psychometricCount = eliminatedRoles.filter((r) => r.stage === 'PSYCHOMETRIC').length
  const technicalCount = eliminatedRoles.filter((r) => r.stage === 'TECHNICAL').length

  const handleCopySummary = () => {
    const summaryText = `RoleCompass Assessment Results:
Top Recommended Role: ${predictedRole} (${formatPercent(confidence)} match)
Archetype: ${roleMeta?.archetype || 'IT Professional'}
Alternates: ${alternates.map((a) => `${a.role} (${formatPercent(a.confidence)})`).join(', ') || 'None'}
Disqualified Roles: ${eliminatedRoles.length} of ${ALL_ROLES.length}`

    navigator.clipboard.writeText(summaryText)
    setCopied(true)
    setTimeout(() => setCopied(false), 2500)
  }

  const handlePrint = () => {
    window.print()
  }

  return (
    <Layout>
      <div className="mx-auto w-full max-w-3xl space-y-6 pb-16">
        {/* Top Action Bar (Print & Share) */}
        <div className="flex items-center justify-between px-1 text-xs text-slate-500 dark:text-slate-400 print:hidden">
          <div className="flex items-center gap-1.5 font-medium">
            <Sparkles className="h-4 w-4 text-indigo-500" />
            <span>AI-Driven Career Evaluation Complete</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleCopySummary}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 transition"
              title="Copy Summary to Clipboard"
            >
              {copied ? <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copied ? 'Copied!' : 'Copy Summary'}</span>
            </button>

            <button
              type="button"
              onClick={handlePrint}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 transition"
              title="Print or Save as PDF"
            >
              <Printer className="h-3.5 w-3.5" />
              <span>Print Report</span>
            </button>
          </div>
        </div>

        {/* Fallback Notice */}
        {isFallback && (
          <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/90 p-4 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/40 dark:text-amber-300 shadow-sm">
            <ShieldAlert className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
            <div>
              <p className="font-bold text-sm">Local Heuristic Fallback Mode Active</p>
              <p className="mt-0.5 leading-relaxed text-amber-700 dark:text-amber-400">
                The FastAPI ML inference microservice was unreachable during terminal prediction. This result was computed via the server-side candidate survival rules rather than the 300-tree Random Forest model.
              </p>
            </div>
          </div>
        )}

        {/* Primary Prediction Hero Card */}
        <div className="surface-card relative overflow-hidden p-6 sm:p-9 text-center space-y-6 border-indigo-500/30 dark:border-indigo-500/20 shadow-2xl">
          {/* Subtle top glow bar */}
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-indigo-500 via-purple-500 to-emerald-400" />

          {/* Archetype & Recommendation Badge */}
          <div className="flex flex-wrap items-center justify-center gap-2">
            <div className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-3.5 py-1 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/80 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60">
              <Sparkles className="h-3.5 w-3.5 text-indigo-500 animate-pulse" />
              <span>Best-Fit Recommended Role</span>
            </div>

            {roleMeta?.archetype && (
              <div className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                <Award className="h-3.5 w-3.5 text-purple-500" />
                <span>Archetype: {roleMeta.archetype}</span>
              </div>
            )}
          </div>

          {/* Role Title & Tagline */}
          <div className="space-y-2">
            <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-white">
              {predictedRole}
            </h1>
            {roleMeta?.tagline && (
              <p className="text-sm sm:text-base font-medium text-indigo-600 dark:text-indigo-400 max-w-xl mx-auto">
                {roleMeta.tagline}
              </p>
            )}
          </div>

          {/* Confidence Meter Section */}
          <div className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-5 dark:border-slate-800/70 dark:bg-slate-900/40 max-w-xl mx-auto space-y-3.5 text-left">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Cpu className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
                <span className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-300">
                  Random Forest Ensemble Confidence
                </span>
              </div>
              <span className={`rounded-lg px-2.5 py-0.5 text-xs font-bold border ${tier.color}`}>
                {tier.label}
              </span>
            </div>

            {/* Visual Animated Gauge Bar */}
            <div className="space-y-1.5">
              <div className="flex items-baseline justify-between text-xs">
                <span className="font-semibold text-slate-700 dark:text-slate-300">
                  Model Probability Consensus
                </span>
                <span className="text-lg font-black text-slate-900 dark:text-white">
                  {formatPercent(confidence)}
                </span>
              </div>

              <div className="h-3 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                <div
                  className={`h-full rounded-full bg-gradient-to-r ${tier.barColor} transition-all duration-700`}
                  style={{ width: `${Math.max(confidencePercent, 8)}%` }}
                />
              </div>
            </div>

            <p className="text-[11px] leading-relaxed text-slate-500 dark:text-slate-400">
              {tier.description} Out of 300 independent decision trees in the trained model,{' '}
              <strong className="text-slate-700 dark:text-slate-300">{Math.round(confidence * 300)} trees</strong>{' '}
              voted for {predictedRole} as your dominant occupational fit.
            </p>
          </div>

          {/* Role Summary & Core Competencies */}
          {roleMeta && (
            <div className="text-left border-t border-slate-200/80 pt-6 dark:border-slate-800/80 space-y-4">
              <div className="space-y-1.5">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                  <BookOpen className="h-3.5 w-3.5 text-indigo-500" />
                  <span>Role Overview &amp; Day-to-Day Focus</span>
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
                  {roleMeta.summary}
                </p>
              </div>

              {/* Key Competencies Badges */}
              <div className="space-y-2">
                <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
                  Primary Technical Competencies:
                </span>
                <div className="flex flex-wrap gap-2">
                  {roleMeta.keySkills.map((skill) => (
                    <span
                      key={skill}
                      className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-50/80 px-2.5 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300 border border-indigo-200/50 dark:border-indigo-800/40"
                    >
                      <CheckCircle2 className="h-3 w-3 text-indigo-500" />
                      <span>{skill}</span>
                    </span>
                  ))}
                </div>
              </div>

              {/* Workstyle Fit */}
              <div className="rounded-xl bg-slate-100/70 p-3 text-xs text-slate-600 dark:bg-slate-900/50 dark:text-slate-400 border border-slate-200/40 dark:border-slate-800/40">
                <span className="font-semibold text-slate-700 dark:text-slate-300">Psychometric Workstyle Fit: </span>
                {roleMeta.workStyle}
              </div>
            </div>
          )}

          {/* Alternates & Classifier Distribution */}
          {alternates && alternates.length > 0 && (
            <div className="border-t border-slate-200/80 pt-6 text-left dark:border-slate-800/80 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                    <Layers className="h-3.5 w-3.5 text-indigo-500" />
                    <span>Top Alternative Roles</span>
                  </h3>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Other occupational tracks with notable vote share in the Random Forest classifier.
                  </p>
                </div>
              </div>

              <div className="space-y-2.5">
                {alternates.map((alternate) => {
                  const altMeta = ROLE_METADATA[alternate.role]
                  const altPercent = Math.round((alternate.confidence ?? 0) * 100)

                  return (
                    <div
                      key={alternate.role}
                      className="rounded-xl border border-slate-200/80 bg-slate-50/60 p-3.5 dark:border-slate-800/70 dark:bg-slate-900/30 space-y-2 hover:border-indigo-300 dark:hover:border-indigo-800/60 transition"
                    >
                      <div className="flex items-center justify-between text-xs">
                        <div className="space-y-0.5">
                          <span className="font-bold text-slate-800 dark:text-slate-200">
                            {alternate.role}
                          </span>
                          {altMeta?.archetype && (
                            <span className="block text-[10px] text-slate-400">
                              {altMeta.archetype}
                            </span>
                          )}
                        </div>
                        <span className="font-black text-indigo-600 dark:text-indigo-400 text-sm">
                          {formatPercent(alternate.confidence)}
                        </span>
                      </div>

                      {/* Alternate Progress Bar */}
                      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                        <div
                          className="h-full rounded-full bg-indigo-500/70 dark:bg-indigo-400/60 transition-all duration-500"
                          style={{ width: `${Math.max(altPercent, 5)}%` }}
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </div>

        {/* Dual-Layer Architecture Explainer Card (For Academic / Professor Presentation) */}
        <div className="surface-card p-6 sm:p-7 space-y-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400">
              <Compass className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-900 dark:text-white">
                How RoleCompass Reached This Decision
              </h2>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Separation of concerns between Adaptive Routing and Machine Learning layers.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 text-xs">
            <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4 dark:border-slate-800/60 dark:bg-slate-900/30 space-y-2">
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-md bg-indigo-600 text-[10px] font-bold text-white">
                  1
                </span>
                <span className="font-bold text-slate-800 dark:text-slate-200">
                  Adaptive Routing Engine (Server)
                </span>
              </div>
              <p className="text-slate-600 dark:text-slate-400 leading-relaxed text-[11px]">
                Rule-based gate passes evaluated your RIASEC personality profile (Section 1) and core technical thresholds (Section 2) to eliminate roles incompatible with your expressed interests.
              </p>
            </div>

            <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4 dark:border-slate-800/60 dark:bg-slate-900/30 space-y-2">
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-md bg-purple-600 text-[10px] font-bold text-white">
                  2
                </span>
                <span className="font-bold text-slate-800 dark:text-slate-200">
                  Random Forest Classifier (Python ML)
                </span>
              </div>
              <p className="text-slate-600 dark:text-slate-400 leading-relaxed text-[11px]">
                Passed your normalized 20-dimensional technical skill vector through 300 decision trees to pick the winning match exclusively from the candidate roles that survived the routing gates.
              </p>
            </div>
          </div>
        </div>

        {/* Explainability Engine: Elimination Audit Log (Task #3) */}
        <div className="surface-card p-6 sm:p-7 space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <XCircle className="h-5 w-5 text-rose-500" />
                <h2 className="text-base font-bold text-slate-900 dark:text-white">
                  Decision Engine: Role Elimination Audit Log
                </h2>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Detailed plain-English reasoning showing why other IT roles were disqualified during your assessment.
              </p>
            </div>

            {/* Filter Tabs */}
            {eliminatedRoles.length > 0 && (
              <div className="inline-flex rounded-xl bg-slate-100 p-1 dark:bg-slate-800 text-xs self-start sm:self-auto">
                <button
                  type="button"
                  onClick={() => setEliminationFilter('ALL')}
                  className={`rounded-lg px-2.5 py-1 font-medium transition ${
                    eliminationFilter === 'ALL'
                      ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-white'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                  }`}
                >
                  All ({eliminatedRoles.length})
                </button>
                <button
                  type="button"
                  onClick={() => setEliminationFilter('PSYCHOMETRIC')}
                  className={`rounded-lg px-2.5 py-1 font-medium transition ${
                    eliminationFilter === 'PSYCHOMETRIC'
                      ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-white'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                  }`}
                >
                  Preferences ({psychometricCount})
                </button>
                <button
                  type="button"
                  onClick={() => setEliminationFilter('TECHNICAL')}
                  className={`rounded-lg px-2.5 py-1 font-medium transition ${
                    eliminationFilter === 'TECHNICAL'
                      ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-white'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                  }`}
                >
                  Skills ({technicalCount})
                </button>
              </div>
            )}
          </div>

          {/* Disqualification Cards */}
          {eliminatedRoles.length === 0 ? (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-5 text-center dark:border-emerald-900/50 dark:bg-emerald-950/30">
              <CheckCircle2 className="mx-auto h-7 w-7 text-emerald-600 dark:text-emerald-400 mb-2" />
              <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                All 10 IT Occupational Roles Qualified
              </h3>
              <p className="mt-1 text-xs text-slate-600 dark:text-slate-300 max-w-md mx-auto">
                Your responses passed all initial psychometric and technical floor filters without disqualification. The Random Forest evaluated the complete candidate space to select your optimal match.
              </p>
            </div>
          ) : filteredEliminations.length === 0 ? (
            <div className="text-center py-6 text-xs text-slate-400">
              No roles were eliminated in this specific category.
            </div>
          ) : (
            <div className="divide-y divide-slate-100 dark:divide-slate-800/80 rounded-xl border border-slate-200/80 dark:border-slate-800/80 overflow-hidden bg-white/40 dark:bg-slate-900/20">
              {filteredEliminations.map((item) => {
                const isPsych = item.stage === 'PSYCHOMETRIC'
                return (
                  <div key={item.role} className="p-4 sm:p-5 space-y-2 hover:bg-slate-50/50 dark:hover:bg-slate-800/20 transition">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <XCircle className="h-4 w-4 text-rose-500 shrink-0" />
                        <span className="text-sm font-bold text-slate-900 dark:text-white">
                          {item.role}
                        </span>
                      </div>

                      <span
                        className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-semibold ${
                          isPsych
                            ? 'bg-purple-50 text-purple-700 dark:bg-purple-950/60 dark:text-purple-300 border border-purple-200/50 dark:border-purple-800/50'
                            : 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300 border border-blue-200/50 dark:border-blue-800/50'
                        }`}
                      >
                        {isPsych ? <Brain className="h-3 w-3" /> : <Terminal className="h-3 w-3" />}
                        <span>{isPsych ? 'Psychometric Gate (Preferences)' : 'Technical Floor Gate (Skills)'}</span>
                      </span>
                    </div>

                    <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed pl-6">
                      {item.reason}
                    </p>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* Action Controls & Navigation */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-3 print:hidden">
          <Link to="/profile" className="btn-secondary gap-2 text-xs w-full sm:w-auto px-5 py-2.5">
            <User className="h-3.5 w-3.5" />
            <span>View Profile &amp; Test History</span>
          </Link>

          <button
            type="button"
            onClick={() => {
              sessionStorage.removeItem(RESULTS_KEY)
              navigate('/assessment')
            }}
            className="btn-primary gap-2 text-xs w-full sm:w-auto px-6 py-2.5"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Retake Assessment</span>
          </button>
        </div>
      </div>
    </Layout>
  )
}
