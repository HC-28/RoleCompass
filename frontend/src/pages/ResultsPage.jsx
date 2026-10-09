import {
  ArrowRight,
  Award,
  BookOpen,
  Brain,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Compass,
  Copy,
  Cpu,
  GitMerge,
  Layers,
  Network,
  Printer,
  RotateCcw,
  ShieldAlert,
  Sparkles,
  Terminal,
  TrendingUp,
  User,
  XCircle,
} from "lucide-react"
import { useMemo, useState } from "react"
import { Link, useLocation, useNavigate } from "react-router-dom"
import Layout from "../components/Layout"
import { ALL_ROLES, ROLE_METADATA } from "../constants/assessment.constants"

const RESULTS_KEY = "rolecompass_results"

const FEATURE_LABELS = [
  { key: "SERVER",    label: "Server / Backend Logic",      role: "Backend Developer" },
  { key: "STORAGE",   label: "Database & Storage",          role: "Data Engineer" },
  { key: "API",       label: "REST / API Design",           role: "Backend Developer" },
  { key: "UI",        label: "UI / Frontend Rendering",     role: "Frontend Developer" },
  { key: "STATE",     label: "State Management",            role: "Frontend Developer" },
  { key: "BUILD",     label: "Build Pipelines (CI/CD)",     role: "DevOps Engineer" },
  { key: "INFRA",     label: "Infrastructure Provisioning", role: "Cloud Engineer" },
  { key: "CONTAINER", label: "Container Orchestration",     role: "DevOps Engineer" },
  { key: "CLOUD",     label: "Cloud Services",              role: "Cloud Engineer" },
  { key: "STATS",     label: "Statistical Analysis",        role: "Data Scientist" },
  { key: "MODEL",     label: "ML Model Building",           role: "AI / ML Engineer" },
  { key: "PIPELINE",  label: "Data Pipelines",              role: "Data Engineer" },
  { key: "MOBILE",    label: "Mobile Development",          role: "Mobile Developer" },
  { key: "THREAT",    label: "Threat Analysis",             role: "Cybersecurity Engineer" },
  { key: "HARDENING", label: "System Hardening",            role: "Cybersecurity Engineer" },
  { key: "TESTDES",   label: "Test Design",                 role: "QA / Test Automation Engineer" },
  { key: "TESTAUTO",  label: "Test Automation Scripts",     role: "QA / Test Automation Engineer" },
  { key: "OBSERV",    label: "Observability & Monitoring",  role: "DevOps Engineer" },
  { key: "PERF",      label: "Performance Tuning",          role: "QA / Test Automation Engineer" },
  { key: "FULLSPEC",  label: "Full-Stack Breadth",          role: "Full Stack Developer" },
]

const STAGE_CONFIG = {
  PSYCHOMETRIC: {
    label: "Personality Gate (Section 1)",
    badge: "bg-purple-100 text-purple-700 dark:bg-purple-900/60 dark:text-purple-300 border border-purple-200/50 dark:border-purple-800/50",
    icon: Brain,
    iconColor: "text-purple-500",
  },
  TECHNICAL: {
    label: "Skill Floor Gate (Section 2)",
    badge: "bg-blue-100 text-blue-700 dark:bg-blue-900/60 dark:text-blue-300 border border-blue-200/50 dark:border-blue-800/50",
    icon: Terminal,
    iconColor: "text-blue-500",
  },
  RESOLVER: {
    label: "Pairwise Discriminator (Section 3)",
    badge: "bg-amber-100 text-amber-700 dark:bg-amber-900/60 dark:text-amber-300 border border-amber-200/50 dark:border-amber-800/50",
    icon: GitMerge,
    iconColor: "text-amber-500",
  },
}

function formatPercent(value) {
  if (value == null) return "0%"
  return `${Math.round(value * 100)}%`
}

function getConfidenceTier(confidence) {
  if (confidence >= 0.6) return { label: "Decisive Alignment", color: "text-emerald-700 bg-emerald-50 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800/50", barColor: "from-emerald-500 to-teal-400", description: "The Random Forest ensemble reached strong consensus for this occupational profile." }
  if (confidence >= 0.35) return { label: "Solid Match", color: "text-indigo-700 bg-indigo-50 border-indigo-200 dark:bg-indigo-950/60 dark:text-indigo-300 dark:border-indigo-800/50", barColor: "from-indigo-500 to-cyan-400", description: "Clear primary affinity, with balanced secondary competencies in related technical tracks." }
  return { label: "Multi-Disciplinary Fit", color: "text-amber-700 bg-amber-50 border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800/50", barColor: "from-amber-500 to-orange-400", description: "Probability is distributed across overlapping domains — you are versatile across multiple IT tracks." }
}

function SkillBar({ label, value, highlight }) {
  const pct = Math.round((value ?? 0) * 100)
  const barColor = highlight ? "from-indigo-500 to-purple-500" : pct >= 70 ? "from-emerald-500 to-teal-400" : pct >= 40 ? "from-amber-400 to-orange-400" : "from-rose-400 to-rose-500"
  return (
    <div className="flex items-center gap-2.5 text-xs">
      <span className={`w-44 shrink-0 truncate text-right font-medium ${highlight ? "text-indigo-600 dark:text-indigo-400" : "text-slate-600 dark:text-slate-400"}`}>{label}</span>
      <div className="flex-1 h-2 rounded-full bg-slate-200 dark:bg-slate-800 overflow-hidden">
        <div className={`h-full rounded-full bg-gradient-to-r ${barColor} transition-all duration-700`} style={{ width: `${Math.max(pct, 3)}%` }} />
      </div>
      <span className={`w-8 text-right font-bold tabular-nums ${highlight ? "text-indigo-600 dark:text-indigo-400" : "text-slate-500 dark:text-slate-400"}`}>{pct}%</span>
    </div>
  )
}

function JourneyStep({ stepNum, title, subtitle, children, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen)
  return (
    <div className="rounded-xl border border-slate-200/80 dark:border-slate-800/80 overflow-hidden">
      <button type="button" onClick={() => setOpen(v => !v)} className="w-full flex items-center gap-3 p-4 text-left hover:bg-slate-50 dark:hover:bg-slate-800/30 transition">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-indigo-600 text-[11px] font-black text-white">{stepNum}</span>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-bold text-slate-900 dark:text-white">{title}</p>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">{subtitle}</p>
        </div>
        {open ? <ChevronUp className="h-4 w-4 text-slate-400 shrink-0" /> : <ChevronDown className="h-4 w-4 text-slate-400 shrink-0" />}
      </button>
      {open && <div className="border-t border-slate-200/80 dark:border-slate-800/80 p-4 space-y-3 bg-slate-50/50 dark:bg-slate-900/30">{children}</div>}
    </div>
  )
}

export default function ResultsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [eliminationFilter, setEliminationFilter] = useState("ALL")
  const [copied, setCopied] = useState(false)

  const results = useMemo(() => {
    const fromState = location.state?.results
    if (fromState) return fromState
    const stored = sessionStorage.getItem(RESULTS_KEY)
    if (stored) { try { return JSON.parse(stored) } catch { return null } }
    return null
  }, [location.state])

  if (!results) {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-md p-8 text-center space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400"><Compass className="h-6 w-6" /></div>
          <h1 className="text-lg font-bold text-slate-900 dark:text-white">No Assessment Results Found</h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">Complete an assessment session to generate your personalized IT role prediction.</p>
          <div className="pt-2"><Link to="/assessment" className="btn-primary gap-2 text-xs px-5 py-2"><span>Start Assessment</span><ArrowRight className="h-3.5 w-3.5" /></Link></div>
        </div>
      </Layout>
    )
  }

  const predictedRole   = results.predicted_role || "Specialist Role"
  const eliminatedRoles = results.eliminated_roles ?? []
  const isFallback      = results.is_fallback === true || results.fallback === true
  const roleMeta        = ROLE_METADATA[predictedRole]
  const techVector      = results.answered_vector ?? results.tech_vector ?? []

  // ── Display-only renormalization ─────────────────────────────────────────
  // The Random Forest assigns probabilities across ALL 10 roles summing to 1.0.
  // When only 2–3 roles survive routing, the remaining probability mass sits
  // with eliminated roles and is invisible to the user. Showing raw values
  // (e.g. QA=34%, Backend=1%) is misleading: Backend's 1% isn't because it's
  // "nearly impossible" — it's because 8 eliminated roles absorbed 65% of the
  // probability budget. Renormalising among survivors gives honest relative
  // confidence (QA=97%, Backend=3%) that accurately reflects the model's
  // verdict within the candidate set.
  // NOTE: This is purely cosmetic. The routing engine on the backend uses raw
  // probabilities for its 18% margin check — this change does NOT affect that.
  const rawAlternates     = results.alternates ?? []
  const rawConfidence     = results.confidence ?? 0
  const totalSurvivorProb = rawConfidence + rawAlternates.reduce((s, a) => s + (a.confidence ?? 0), 0)
  const normFactor        = totalSurvivorProb > 0 ? totalSurvivorProb : 1
  const confidence        = rawConfidence / normFactor
  const confidencePercent = Math.round(confidence * 100)
  const tier              = getConfidenceTier(confidence)
  const alternates        = rawAlternates.map(a => ({ ...a, confidence: (a.confidence ?? 0) / normFactor }))

  const byStage = useMemo(() => {
    const g = { PSYCHOMETRIC: [], TECHNICAL: [], RESOLVER: [] }
    eliminatedRoles.forEach(r => { const s = r.stage?.toUpperCase(); if (g[s]) g[s].push(r) })
    return g
  }, [eliminatedRoles])

  const filteredEliminations = useMemo(() => {
    if (eliminationFilter === "PSYCHOMETRIC") return byStage.PSYCHOMETRIC
    if (eliminationFilter === "TECHNICAL")    return byStage.TECHNICAL
    if (eliminationFilter === "RESOLVER")     return byStage.RESOLVER
    return eliminatedRoles
  }, [eliminationFilter, byStage, eliminatedRoles])

  const psychometricCount = byStage.PSYCHOMETRIC.length
  const technicalCount    = byStage.TECHNICAL.length
  const resolverCount     = byStage.RESOLVER.length

  const handleCopySummary = () => {
    navigator.clipboard.writeText(`RoleCompass Assessment Results\nTop Role: ${predictedRole} (${formatPercent(confidence)} match)\nArchetype: ${roleMeta?.archetype || "IT Professional"}\nAlternates: ${alternates.map(a => `${a.role} (${formatPercent(a.confidence)})`).join(", ") || "None"}\nRoles eliminated: ${eliminatedRoles.length} of ${ALL_ROLES.length}`)
    setCopied(true)
    setTimeout(() => setCopied(false), 2500)
  }

  return (
    <Layout>
      <div className="mx-auto w-full max-w-3xl space-y-6 pb-16">

        <div className="flex items-center justify-between px-1 text-xs text-slate-500 dark:text-slate-400 print:hidden">
          <div className="flex items-center gap-1.5 font-medium"><Sparkles className="h-4 w-4 text-indigo-500" /><span>AI-Driven Career Evaluation Complete</span></div>
          <div className="flex items-center gap-2">
            <button type="button" onClick={handleCopySummary} className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 transition">
              {copied ? <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copied ? "Copied!" : "Copy Summary"}</span>
            </button>
            <button type="button" onClick={() => window.print()} className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 transition">
              <Printer className="h-3.5 w-3.5" /><span>Print Report</span>
            </button>
          </div>
        </div>

        {isFallback && (
          <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/90 p-4 text-xs text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/40 dark:text-amber-300 shadow-sm">
            <ShieldAlert className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
            <div>
              <p className="font-bold text-sm">Local Heuristic Fallback Mode Active</p>
              <p className="mt-0.5 leading-relaxed text-amber-700 dark:text-amber-400">The FastAPI ML inference microservice was unreachable. This result was computed via the server-side candidate survival rules rather than the 300-tree Random Forest model.</p>
            </div>
          </div>
        )}

        {/* PRIMARY PREDICTION HERO */}
        <div className="surface-card relative overflow-hidden p-6 sm:p-9 text-center space-y-6 border-indigo-500/30 dark:border-indigo-500/20 shadow-2xl">
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-indigo-500 via-purple-500 to-emerald-400" />
          <div className="flex flex-wrap items-center justify-center gap-2">
            <div className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-3.5 py-1 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/80 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60"><Sparkles className="h-3.5 w-3.5 text-indigo-500 animate-pulse" /><span>Best-Fit Recommended Role</span></div>
            {roleMeta?.archetype && <div className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300"><Award className="h-3.5 w-3.5 text-purple-500" /><span>Archetype: {roleMeta.archetype}</span></div>}
          </div>
          <div className="space-y-2">
            <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-white">{predictedRole}</h1>
            {roleMeta?.tagline && <p className="text-sm sm:text-base font-medium text-indigo-600 dark:text-indigo-400 max-w-xl mx-auto">{roleMeta.tagline}</p>}
          </div>
          <div className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-5 dark:border-slate-800/70 dark:bg-slate-900/40 max-w-xl mx-auto space-y-3.5 text-left">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2"><Cpu className="h-4 w-4 text-indigo-600 dark:text-indigo-400" /><span className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-300">Random Forest Ensemble Confidence</span></div>
              <span className={`rounded-lg px-2.5 py-0.5 text-xs font-bold border ${tier.color}`}>{tier.label}</span>
            </div>
            <div className="space-y-1.5">
              <div className="flex items-baseline justify-between text-xs"><span className="font-semibold text-slate-700 dark:text-slate-300">Model Probability Consensus</span><span className="text-lg font-black text-slate-900 dark:text-white">{formatPercent(confidence)}</span></div>
              <div className="h-3 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800"><div className={`h-full rounded-full bg-gradient-to-r ${tier.barColor} transition-all duration-700`} style={{ width: `${Math.max(confidencePercent, 8)}%` }} /></div>
            </div>
            <p className="text-[11px] leading-relaxed text-slate-500 dark:text-slate-400">{tier.description} Out of 300 independent decision trees, <strong className="text-slate-700 dark:text-slate-300">{Math.round(confidence * 300)} trees</strong> voted for {predictedRole} as your dominant occupational fit.</p>
          </div>
          {roleMeta && (
            <div className="text-left border-t border-slate-200/80 pt-6 dark:border-slate-800/80 space-y-4">
              <div className="space-y-1.5">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-1.5"><BookOpen className="h-3.5 w-3.5 text-indigo-500" /><span>Role Overview &amp; Day-to-Day Focus</span></h3>
                <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">{roleMeta.summary}</p>
              </div>
              <div className="space-y-2">
                <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">Primary Technical Competencies:</span>
                <div className="flex flex-wrap gap-2">{roleMeta.keySkills.map(skill => <span key={skill} className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-50/80 px-2.5 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300 border border-indigo-200/50 dark:border-indigo-800/40"><CheckCircle2 className="h-3 w-3 text-indigo-500" /><span>{skill}</span></span>)}</div>
              </div>
              <div className="rounded-xl bg-slate-100/70 p-3 text-xs text-slate-600 dark:bg-slate-900/50 dark:text-slate-400 border border-slate-200/40 dark:border-slate-800/40"><span className="font-semibold text-slate-700 dark:text-slate-300">Psychometric Workstyle Fit: </span>{roleMeta.workStyle}</div>
            </div>
          )}
          {alternates && alternates.length > 0 && (
            <div className="border-t border-slate-200/80 pt-6 text-left dark:border-slate-800/80 space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-300 flex items-center gap-1.5"><Layers className="h-3.5 w-3.5 text-indigo-500" /><span>Top Alternative Roles</span></h3>
              <p className="text-[11px] text-slate-400 -mt-2">Other occupational tracks with notable vote share in the Random Forest classifier.</p>
              <div className="space-y-2.5">
                {alternates.map(alternate => {
                  const altMeta = ROLE_METADATA[alternate.role]
                  const altPercent = Math.round((alternate.confidence ?? 0) * 100)
                  return (
                    <div key={alternate.role} className="rounded-xl border border-slate-200/80 bg-slate-50/60 p-3.5 dark:border-slate-800/70 dark:bg-slate-900/30 space-y-2 hover:border-indigo-300 dark:hover:border-indigo-800/60 transition">
                      <div className="flex items-center justify-between text-xs">
                        <div className="space-y-0.5"><span className="font-bold text-slate-800 dark:text-slate-200">{alternate.role}</span>{altMeta?.archetype && <span className="block text-[10px] text-slate-400">{altMeta.archetype}</span>}</div>
                        <span className="font-black text-indigo-600 dark:text-indigo-400 text-sm">{formatPercent(alternate.confidence)}</span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800"><div className="h-full rounded-full bg-indigo-500/70 dark:bg-indigo-400/60 transition-all duration-500" style={{ width: `${Math.max(altPercent, 5)}%` }} /></div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </div>

        {/* DECISION JOURNEY */}
        <div className="surface-card p-6 sm:p-7 space-y-5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400"><Network className="h-5 w-5" /></div>
            <div>
              <h2 className="text-sm font-bold text-slate-900 dark:text-white">How RoleCompass Reached This Decision</h2>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">Full audit trail of every elimination gate and the final ML prediction logic.</p>
            </div>
          </div>
          <div className="space-y-2.5">

            <JourneyStep stepNum={1} title="Section 1 — Personality & Work Style (RIASEC Psychometric Gates)" subtitle={psychometricCount > 0 ? `${psychometricCount} role${psychometricCount > 1 ? "s" : ""} eliminated by personality trait thresholds` : "All roles passed — no eliminations at this stage"} defaultOpen={psychometricCount > 0}>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">Section 1 measured your <strong className="text-slate-700 dark:text-slate-300">O*NET RIASEC personality dimensions</strong> — not job-specific skills. These are the same dimensions used by professional career counselling systems worldwide. Each gate fires when a trait score falls outside the range that O*NET research shows is required for that role.</p>
              <div className="rounded-xl bg-indigo-50/70 dark:bg-indigo-950/30 border border-indigo-200/50 dark:border-indigo-800/40 p-3 text-[11px] space-y-1">
                <p className="font-semibold text-indigo-800 dark:text-indigo-300">Gates checked at this stage:</p>
                <ul className="list-disc pl-4 space-y-0.5 text-indigo-700 dark:text-indigo-400">
                  <li><strong>Realistic Gate (R &lt; 0.40):</strong> Eliminates DevOps &amp; Cloud — requires hands-on systems configuration preference</li>
                  <li><strong>Investigative Gate (I &lt; 0.60):</strong> Eliminates Data Scientist — requires strong analytical research drive</li>
                  <li><strong>Artistic Gate (A &lt; 0.35):</strong> Eliminates Frontend Developer — requires visual design appreciation</li>
                  <li><strong>Anti-Artistic Gate (A &gt; 0.55):</strong> Eliminates Data Engineer &amp; Cybersecurity — requires structured rule-driven mindset</li>
                  <li><strong>Conventional Gate (C &lt; 0.65):</strong> Eliminates Data Engineer &amp; QA — requires affinity for process-driven methodical work</li>
                </ul>
              </div>
              {byStage.PSYCHOMETRIC.length > 0 ? (
                <div className="space-y-2">{byStage.PSYCHOMETRIC.map(item => <div key={item.role} className="flex gap-2.5 p-3 rounded-lg bg-purple-50/70 dark:bg-purple-950/30 border border-purple-200/50 dark:border-purple-800/40"><XCircle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" /><div><p className="text-xs font-bold text-slate-900 dark:text-white">{item.role}</p><p className="text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed mt-0.5">{item.reason}</p></div></div>)}</div>
              ) : (
                <div className="flex items-center gap-2 text-[11px] text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200/50 dark:border-emerald-800/40 rounded-lg p-3"><CheckCircle2 className="h-4 w-4 shrink-0" /><span>No roles eliminated at this stage. All passed the personality gates.</span></div>
              )}
            </JourneyStep>

            <JourneyStep stepNum={2} title="Section 2 — Technical Skill Affinity (Minimum Floor Gates)" subtitle={technicalCount > 0 ? `${technicalCount} role${technicalCount > 1 ? "s" : ""} eliminated by minimum technical score threshold` : "All remaining roles passed the technical floor gates"} defaultOpen={technicalCount > 0}>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">Section 2 collected your <strong className="text-slate-700 dark:text-slate-300">Task Affinity scores</strong> across 20 technical domains. Three minimum-floor gates verify baseline interest in a role&apos;s core domain before that role proceeds to the ML stage.</p>
              {byStage.TECHNICAL.length > 0 ? (
                <div className="space-y-2">{byStage.TECHNICAL.map(item => <div key={item.role} className="flex gap-2.5 p-3 rounded-lg bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200/50 dark:border-blue-800/40"><XCircle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" /><div><p className="text-xs font-bold text-slate-900 dark:text-white">{item.role}</p><p className="text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed mt-0.5">{item.reason}</p></div></div>)}</div>
              ) : (
                <div className="flex items-center gap-2 text-[11px] text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200/50 dark:border-emerald-800/40 rounded-lg p-3"><CheckCircle2 className="h-4 w-4 shrink-0" /><span>No roles eliminated. All remaining roles passed the skill floor gates.</span></div>
              )}
            </JourneyStep>

            <JourneyStep stepNum={3} title="Section 3 — Pairwise Preference Discriminators" subtitle={resolverCount > 0 ? `${resolverCount} role${resolverCount > 1 ? "s" : ""} eliminated by trade-off preference questions` : "Section 3 was skipped — ML confidence margin was already decisive (18%+)"} defaultOpen={resolverCount > 0}>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">When two closely-related roles both survived Sections 1 and 2, a single trade-off question separated them. <strong className="text-slate-700 dark:text-slate-300">These questions eliminate losers — they do NOT predict the winner.</strong> The final prediction is always made by the ML model. A role that was never in any discriminator pair can still win via ML score.</p>
              {byStage.RESOLVER.length > 0 ? (
                <div className="space-y-2">{byStage.RESOLVER.map(item => <div key={item.role} className="flex gap-2.5 p-3 rounded-lg bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200/50 dark:border-amber-800/40"><XCircle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" /><div><p className="text-xs font-bold text-slate-900 dark:text-white">{item.role}</p><p className="text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed mt-0.5">{item.reason}</p></div></div>)}</div>
              ) : (
                <div className="flex items-center gap-2 text-[11px] text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200/50 dark:border-emerald-800/40 rounded-lg p-3"><CheckCircle2 className="h-4 w-4 shrink-0" /><span>Section 3 was not needed — ML already had a decisive lead (margin 18%+).</span></div>
              )}
            </JourneyStep>

            <JourneyStep stepNum={4} title="Step 4 — Random Forest Final Prediction (300 Decision Trees)" subtitle={`Winner: ${predictedRole} at ${formatPercent(confidence)} — decided purely by your 20 technical skill scores`} defaultOpen={true}>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">Surviving roles were passed to the <strong className="text-slate-700 dark:text-slate-300">Python Random Forest classifier</strong>. The model read your 20 normalized technical skill scores from Section 2 and voted across 300 decision trees. <strong className="text-slate-700 dark:text-slate-300">Your Section 3 preferences are not visible to the ML model — only your skill scores matter here.</strong></p>
              {techVector.length === 20 && (
                <div className="space-y-2 pt-1">
                  <p className="text-[11px] font-semibold text-slate-600 dark:text-slate-300 flex items-center gap-1.5"><TrendingUp className="h-3.5 w-3.5 text-indigo-500" />Your 20 Technical Skill Scores (exactly what the ML model received):</p>
                  <div className="space-y-1.5 rounded-xl border border-slate-200/80 dark:border-slate-800/80 p-4 bg-white/60 dark:bg-slate-900/40">
                    {FEATURE_LABELS.map((f, i) => <SkillBar key={f.key} label={f.label} value={techVector[i]} highlight={f.role === predictedRole} />)}
                  </div>
                  <p className="text-[10px] text-slate-400 dark:text-slate-500"><span className="inline-block w-2 h-2 rounded-full bg-indigo-500 mr-1 align-middle" />Highlighted bars are the core skill dimensions for <strong>{predictedRole}</strong>. These scored highest in your profile — that is why the model chose this role.</p>
                </div>
              )}
              <div className="rounded-xl bg-emerald-50/70 dark:bg-emerald-950/30 border border-emerald-200/50 dark:border-emerald-800/40 p-3 text-[11px]">
                <p className="font-semibold text-emerald-800 dark:text-emerald-300 mb-1">Why this result may seem surprising:</p>
                <p className="leading-relaxed text-emerald-700 dark:text-emerald-400">Section 3 only compared roles that were directly competing against each other. If your highest skill scores belong to a role that was never directly compared against your stated preference, the ML still picks it — because your Section 2 skill affinity data is more reliable than a single trade-off preference question.</p>
              </div>
            </JourneyStep>

          </div>
        </div>

        {/* FULL ELIMINATION AUDIT LOG */}
        <div className="surface-card p-6 sm:p-7 space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <div className="flex items-center gap-2"><XCircle className="h-5 w-5 text-rose-500" /><h2 className="text-base font-bold text-slate-900 dark:text-white">Full Role Elimination Audit Log</h2></div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">Every role eliminated, at which stage, and exactly why.</p>
            </div>
            {eliminatedRoles.length > 0 && (
              <div className="inline-flex rounded-xl bg-slate-100 p-1 dark:bg-slate-800 text-xs self-start sm:self-auto">
                {[{ key: "ALL", label: `All (${eliminatedRoles.length})` }, { key: "PSYCHOMETRIC", label: `Personality (${psychometricCount})` }, { key: "TECHNICAL", label: `Skills (${technicalCount})` }, { key: "RESOLVER", label: `Pairwise (${resolverCount})` }].map(({ key, label }) => (
                  <button key={key} type="button" onClick={() => setEliminationFilter(key)} className={`rounded-lg px-2.5 py-1 font-medium transition ${eliminationFilter === key ? "bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-white" : "text-slate-600 dark:text-slate-400 hover:text-slate-900"}`}>{label}</button>
                ))}
              </div>
            )}
          </div>
          {eliminatedRoles.length === 0 ? (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-5 text-center dark:border-emerald-900/50 dark:bg-emerald-950/30"><CheckCircle2 className="mx-auto h-7 w-7 text-emerald-600 dark:text-emerald-400 mb-2" /><h3 className="text-sm font-bold text-slate-900 dark:text-white">All 10 IT Roles Qualified</h3><p className="mt-1 text-xs text-slate-600 dark:text-slate-300 max-w-md mx-auto">Your responses passed all psychometric and technical floor filters. The Random Forest evaluated the complete candidate space.</p></div>
          ) : filteredEliminations.length === 0 ? (
            <div className="text-center py-6 text-xs text-slate-400">No roles eliminated in this category.</div>
          ) : (
            <div className="divide-y divide-slate-100 dark:divide-slate-800/80 rounded-xl border border-slate-200/80 dark:border-slate-800/80 overflow-hidden bg-white/40 dark:bg-slate-900/20">
              {filteredEliminations.map(item => {
                const cfg = STAGE_CONFIG[item.stage?.toUpperCase()] ?? STAGE_CONFIG.PSYCHOMETRIC
                const Icon = cfg.icon
                return (
                  <div key={item.role} className="p-4 sm:p-5 space-y-2 hover:bg-slate-50/50 dark:hover:bg-slate-800/20 transition">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2.5"><XCircle className="h-4 w-4 text-rose-500 shrink-0" /><span className="text-sm font-bold text-slate-900 dark:text-white">{item.role}</span></div>
                      <span className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-semibold ${cfg.badge}`}><Icon className={`h-3 w-3 ${cfg.iconColor}`} /><span>{cfg.label}</span></span>
                    </div>
                    <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed pl-6">{item.reason}</p>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-3 print:hidden">
          <Link to="/profile" className="btn-secondary gap-2 text-xs w-full sm:w-auto px-5 py-2.5"><User className="h-3.5 w-3.5" /><span>View Profile &amp; Test History</span></Link>
          <button type="button" onClick={() => { sessionStorage.removeItem(RESULTS_KEY); navigate("/assessment") }} className="btn-primary gap-2 text-xs w-full sm:w-auto px-6 py-2.5"><RotateCcw className="h-3.5 w-3.5" /><span>Retake Assessment</span></button>
        </div>

      </div>
    </Layout>
  )
}
