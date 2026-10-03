import {
  Award,
  Brain,
  Calendar,
  CheckCircle,
  ChevronDown,
  ChevronUp,
  Clock,
  Compass,
  FileQuestion,
  ListOrdered,
  Mail,
  Play,
  Sparkles,
  Terminal,
  User as UserIcon,
  XCircle,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProfile } from '../api/session'
import Layout from '../components/Layout'
import { useAuth } from '../context/AuthContext'

function formatPercent(value) {
  if (value == null) return null
  return `${Math.round(value * 100)}%`
}

function getLikertColor(value) {
  switch (value) {
    case 5:
      return 'bg-emerald-500 text-white'
    case 4:
      return 'bg-emerald-400/90 text-white'
    case 3:
      return 'bg-slate-400 text-white'
    case 2:
      return 'bg-amber-500 text-white'
    case 1:
      return 'bg-rose-500 text-white'
    default:
      return 'bg-indigo-500 text-white'
  }
}

export default function ProfilePage() {
  const { email } = useAuth()
  const [profileData, setProfileData] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [expandedSessions, setExpandedSessions] = useState({})

  useEffect(() => {
    let cancelled = false
    const fetchProfile = async () => {
      try {
        const data = await getProfile()
        if (!cancelled) {
          setProfileData(data)
          // Automatically expand the first/latest session if available
          if (data?.sessions && data.sessions.length > 0) {
            setExpandedSessions({ [data.sessions[0].session_id]: true })
          }
        }
      } catch (err) {
        console.error('Failed to load profile data', err)
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    fetchProfile()
    return () => {
      cancelled = true
    }
  }, [])

  const toggleSession = (sessionId) => {
    setExpandedSessions((prev) => ({
      ...prev,
      [sessionId]: !prev[sessionId],
    }))
  }

  const sessions = profileData?.sessions || []

  return (
    <Layout>
      <div className="mx-auto w-full max-w-4xl space-y-6 pb-12">
        {/* Profile Header Card */}
        <div className="surface-card p-6 sm:p-8">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-5">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600/10 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400">
                <UserIcon className="h-7 w-7" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-slate-900 dark:text-white">Student Career Profile</h1>
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1 text-xs text-slate-500 dark:text-slate-400">
                  <span className="flex items-center gap-1.5">
                    <Mail className="h-3.5 w-3.5" />
                    {email}
                  </span>
                  {profileData?.created_at && (
                    <span className="flex items-center gap-1.5">
                      <Calendar className="h-3.5 w-3.5" />
                      Joined {new Date(profileData.created_at).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
            </div>

            <Link
              to="/assessment"
              className="btn-primary gap-2 text-xs self-start sm:self-auto"
            >
              <Play className="h-3.5 w-3.5" />
              Take New Assessment
            </Link>
          </div>
        </div>

        {/* Overview Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="surface-card p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
                <Compass className="h-5 w-5" />
              </div>
              <div>
                <div className="text-2xl font-bold text-slate-900 dark:text-white">
                  {isLoading ? '...' : (profileData?.total_assessments ?? 0)}
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400">Total Evaluations</div>
              </div>
            </div>
          </div>

          <div className="surface-card p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <CheckCircle className="h-5 w-5" />
              </div>
              <div>
                <div className="text-2xl font-bold text-slate-900 dark:text-white">
                  {isLoading ? '...' : (profileData?.completed_assessments ?? 0)}
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400">Completed Predictions</div>
              </div>
            </div>
          </div>

          <div className="surface-card p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-500/10 text-purple-600 dark:text-purple-400">
                <Award className="h-5 w-5" />
              </div>
              <div>
                <div className="text-sm font-bold truncate max-w-[170px] text-slate-900 dark:text-white">
                  {isLoading
                    ? '...'
                    : sessions.find((s) => s.predicted_role)?.predicted_role || 'Not evaluated'}
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400">Latest Top Role Match</div>
              </div>
            </div>
          </div>
        </div>

        {/* Previous Test Results & Questions Section */}
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-base font-semibold text-slate-900 dark:text-white flex items-center gap-2">
              <ListOrdered className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              Previous Assessment Results &amp; Question History
            </h2>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              {sessions.length} {sessions.length === 1 ? 'Record' : 'Records'}
            </span>
          </div>

          {isLoading ? (
            <div className="surface-card p-8 text-center text-xs text-slate-500 dark:text-slate-400">
              Loading test history and questions...
            </div>
          ) : sessions.length === 0 ? (
            <div className="surface-card p-10 text-center">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
                <FileQuestion className="h-6 w-6" />
              </div>
              <h3 className="text-sm font-semibold text-slate-900 dark:text-white">No previous assessment records</h3>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
                You haven&rsquo;t completed any job role evaluations yet. Take an assessment to discover your fit.
              </p>
              <Link to="/assessment" className="btn-primary mt-5 gap-2 text-xs">
                <Play className="h-3.5 w-3.5" />
                Start Assessment
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {sessions.map((session) => {
                const isExpanded = !!expandedSessions[session.session_id]
                const isCompleted = session.status === 'completed'
                const qaList = session.questions_and_answers || []

                return (
                  <div
                    key={session.session_id}
                    className="surface-card overflow-hidden border border-slate-200 dark:border-slate-800 transition duration-200"
                  >
                    {/* Assessment Session Header / Summary */}
                    <div
                      onClick={() => toggleSession(session.session_id)}
                      className="cursor-pointer p-5 sm:p-6 bg-slate-50/50 dark:bg-slate-900/30 hover:bg-slate-100/50 dark:hover:bg-slate-900/60 transition flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 select-none"
                    >
                      <div className="flex items-start sm:items-center gap-3.5">
                        <div
                          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                            isCompleted
                              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                              : 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
                          }`}
                        >
                          {isCompleted ? <Award className="h-5 w-5" /> : <Clock className="h-5 w-5" />}
                        </div>

                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-sm font-bold text-slate-900 dark:text-white">
                              {session.predicted_role || 'In-Progress Evaluation'}
                            </span>
                            {session.confidence != null && (
                              <span className="rounded-md bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300">
                                {formatPercent(session.confidence)} Match
                              </span>
                            )}
                            <span
                              className={`rounded-md px-2 py-0.5 text-[10px] font-medium capitalize ${
                                isCompleted
                                  ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
                                  : 'bg-amber-50 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300'
                              }`}
                            >
                              {session.status.replace('_', ' ')}
                            </span>
                          </div>

                          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1 text-xs text-slate-500 dark:text-slate-400">
                            <span>
                              {new Date(session.created_at).toLocaleDateString(undefined, {
                                month: 'short',
                                day: 'numeric',
                                year: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                            <span>•</span>
                            <span>{qaList.length} Questions Answered</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-end sm:self-center">
                        <span className="text-xs font-medium text-indigo-600 dark:text-indigo-400">
                          {isExpanded ? 'Hide Questions' : 'View Questions & Answers'}
                        </span>
                        {isExpanded ? (
                          <ChevronUp className="h-4 w-4 text-slate-400" />
                        ) : (
                          <ChevronDown className="h-4 w-4 text-slate-400" />
                        )}
                      </div>
                    </div>

                    {/* Expandable Questions & Answers Detail List */}
                    {isExpanded && (
                      <div className="border-t border-slate-200/80 dark:border-slate-800/80 p-5 sm:p-6 space-y-6">
                        {/* Eliminated Roles Section */}
                        {session.eliminated_roles && session.eliminated_roles.length > 0 && (
                          <div className="space-y-3">
                            <div className="flex items-center justify-between">
                              <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                                <XCircle className="h-3.5 w-3.5 text-rose-500" />
                                <span>Adaptive Routing: Disqualified Roles ({session.eliminated_roles.length})</span>
                              </h4>
                              <span className="text-[11px] text-slate-400">Pruned during assessment gates</span>
                            </div>

                            <div className="divide-y divide-slate-100 dark:divide-slate-800/60 rounded-xl border border-slate-200/70 dark:border-slate-800/70 bg-white dark:bg-slate-900/40 overflow-hidden">
                              {session.eliminated_roles.map((elim) => {
                                const isPsych = elim.stage === 'PSYCHOMETRIC'
                                return (
                                  <div key={elim.role} className="p-3.5 space-y-1.5 hover:bg-slate-50/50 dark:hover:bg-slate-800/20 transition">
                                    <div className="flex items-center justify-between gap-2">
                                      <div className="flex items-center gap-2">
                                        <XCircle className="h-3.5 w-3.5 text-rose-500 shrink-0" />
                                        <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                                          {elim.role}
                                        </span>
                                      </div>
                                      <span
                                        className={`inline-flex items-center gap-1 rounded px-2 py-0.5 text-[10px] font-semibold ${
                                          isPsych
                                            ? 'bg-purple-50 text-purple-700 dark:bg-purple-950/60 dark:text-purple-300'
                                            : 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300'
                                        }`}
                                      >
                                        {isPsych ? <Brain className="h-2.5 w-2.5" /> : <Terminal className="h-2.5 w-2.5" />}
                                        <span>{isPsych ? 'Psychometric Gate' : 'Technical Floor Gate'}</span>
                                      </span>
                                    </div>
                                    <p className="text-[11px] text-slate-500 dark:text-slate-400 pl-5 leading-relaxed">
                                      {elim.reason}
                                    </p>
                                  </div>
                                )
                              })}
                            </div>
                          </div>
                        )}

                        <div className="flex items-center justify-between">
                          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                            Questions Asked in this Assessment ({qaList.length})
                          </h4>
                          <span className="text-[11px] text-slate-400">Likert Scale (1..5)</span>
                        </div>

                        {qaList.length === 0 ? (
                          <div className="text-xs text-slate-500 py-3">No recorded responses for this session.</div>
                        ) : (
                          <div className="divide-y divide-slate-100 dark:divide-slate-800/60 rounded-xl border border-slate-200/70 dark:border-slate-800/70 bg-white dark:bg-slate-900/40 overflow-hidden">
                            {qaList.map((qa, qIndex) => (
                              <div
                                key={qa.question_id || qIndex}
                                className="p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 hover:bg-slate-50/50 dark:hover:bg-slate-800/20 transition"
                              >
                                <div className="space-y-1 flex-1 pr-4">
                                  <div className="flex items-center gap-2">
                                    <span className="flex h-5 w-5 items-center justify-center rounded-md bg-slate-100 dark:bg-slate-800 text-[11px] font-semibold text-slate-700 dark:text-slate-300">
                                      {qIndex + 1}
                                    </span>
                                    <span className="text-xs font-medium text-slate-900 dark:text-white">
                                      {qa.question_text}
                                    </span>
                                  </div>
                                </div>

                                <div className="flex items-center gap-3 shrink-0 self-start sm:self-auto">
                                  <div className="flex items-center gap-1.5">
                                    <span
                                      className={`flex h-6 w-6 items-center justify-center rounded-md text-xs font-bold ${getLikertColor(
                                        qa.likert_value,
                                      )}`}
                                    >
                                      {qa.likert_value}
                                    </span>
                                    <span className="text-xs font-medium text-slate-700 dark:text-slate-300">
                                      {qa.likert_label}
                                    </span>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </Layout>
  )
}
