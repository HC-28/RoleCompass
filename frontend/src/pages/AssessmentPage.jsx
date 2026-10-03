import { ArrowLeft, ArrowRight, CheckCircle2, ChevronDown, ChevronUp, Loader2, Sparkles } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { predict, startSession, submitAnswers } from '../api/session'
import Layout from '../components/Layout'
import InterestScale from '../components/assessment/InterestScale'
import LikertScale from '../components/assessment/LikertScale'
import PreferenceScale from '../components/assessment/PreferenceScale'
import SectionBanner from '../components/assessment/SectionBanner'
import { ASSESSMENT_BASELINE_TOTAL, RESPONSE_TYPES } from '../constants/assessment.constants'
import { extractErrorMessage } from '../utils/validation'

const RESULTS_KEY = 'rolecompass_results'

// All 10 roles in canonical order — shown as the "initial" pool
const ALL_ROLES = [
  'Backend Developer',
  'Frontend Developer',
  'Full Stack Developer',
  'Data Scientist',
  'Data Engineer',
  'Cybersecurity Engineer',
  'DevOps Engineer',
  'Cloud Engineer',
  'Android Developer',
  'QA / Test Automation Engineer',
]

// Section label map for the progress panel
const SECTION_LABELS = {
  SECTION_1_RIASEC:     'Section 1 — Psychometric (RIASEC)',
  SECTION_2_TECH_CORE:  'Section 2 — Technical Core',
  SECTION_3_RESOLVER:   'Section 3 — Resolver',
  SECTION_4_SPECIALIST: 'Section 4 — Specialist',
  TERMINAL_SCORING:     'Section 4 — Specialist',
  COMPLETED:            'Completed',
}

export default function AssessmentPage() {
  const navigate = useNavigate()

  const [phase, setPhase] = useState('idle')
  const [sessionId, setSessionId] = useState(null)
  const [questions, setQuestions] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [totalAnswered, setTotalAnswered] = useState(0)
  const [fsmState, setFsmState] = useState('SECTION_1_RIASEC')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [showUnansweredWarning, setShowUnansweredWarning] = useState(false)

  // Progress panel state
  const [progressSnapshots, setProgressSnapshots] = useState([
    { sectionLabel: 'Initial Pool', roles: ALL_ROLES },
  ])
  const [showProgress, setShowProgress] = useState(false)

  const currentQuestion = questions[currentIndex]
  const selectedValue = currentQuestion ? (answers[currentQuestion.id] ?? null) : null
  const isLastInBatch = currentIndex === questions.length - 1

  const allBatchAnswered = useMemo(
    () => questions.length > 0 && questions.every((q) => answers[q.id] != null),
    [questions, answers],
  )

  const canGoNext = useMemo(
    () => selectedValue !== null && !isLastInBatch && !isSubmitting,
    [selectedValue, isLastInBatch, isSubmitting],
  )

  const canSubmit = useMemo(
    () => allBatchAnswered && isLastInBatch && !isSubmitting,
    [allBatchAnswered, isLastInBatch, isSubmitting],
  )

  const questionNumber = totalAnswered + currentIndex + 1
  const progress = Math.min((questionNumber / ASSESSMENT_BASELINE_TOTAL) * 100, 100)

  const unansweredInBatch = useMemo(
    () => questions.filter((q) => answers[q.id] == null).length,
    [questions, answers],
  )

  const handleBegin = async () => {
    setPhase('loading')
    setError(null)
    setProgressSnapshots([{ sectionLabel: 'Initial Pool', roles: ALL_ROLES }])
    try {
      const response = await startSession()
      setSessionId(response.session_id)
      setQuestions(response.questions || [])
      setTotalAnswered(0)
      setCurrentIndex(0)
      setAnswers({})
      setFsmState('SECTION_1_RIASEC')
      setPhase('active')
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to start assessment. Please try again.'))
      setPhase('error')
    }
  }

  // Called by Layout -> Header when user confirms exit
  const handleExitConfirm = () => {
    setPhase('idle')
    setSessionId(null)
    setQuestions([])
    setAnswers({})
    setTotalAnswered(0)
    setCurrentIndex(0)
    setFsmState('SECTION_1_RIASEC')
    setProgressSnapshots([{ sectionLabel: 'Initial Pool', roles: ALL_ROLES }])
    setError(null)
  }

  const handleSelect = useCallback(
    (value) => {
      if (!currentQuestion) return
      setShowUnansweredWarning(false)
      setAnswers((prev) => ({ ...prev, [currentQuestion.id]: value }))
    },
    [currentQuestion],
  )

  const handleNext = useCallback(() => {
    if (!canGoNext) return
    setCurrentIndex((prev) => prev + 1)
  }, [canGoNext])

  const handlePrev = useCallback(() => {
    if (currentIndex === 0 || isSubmitting) return
    setShowUnansweredWarning(false)
    setCurrentIndex((prev) => Math.max(prev - 1, 0))
  }, [currentIndex, isSubmitting])

  const handleSubmit = useCallback(async () => {
    if (isSubmitting || !sessionId || !isLastInBatch) return

    if (!allBatchAnswered) {
      setShowUnansweredWarning(true)
      const firstUnansweredIdx = questions.findIndex((q) => answers[q.id] == null)
      if (firstUnansweredIdx >= 0) setCurrentIndex(firstUnansweredIdx)
      return
    }

    setShowUnansweredWarning(false)
    setIsSubmitting(true)
    setError(null)

    try {
      const payload = {
        answers: questions
          .filter((q) => answers[q.id] != null)
          .map((q) => ({
            question_id: q.id,
            likert_value: answers[q.id],
          })),
      }

      const response = await submitAnswers(sessionId, payload)

      if (response.fsm_state) {
        setFsmState(response.fsm_state)
      }

      // Capture progress snapshot from candidate_roles sent by backend
      if (Array.isArray(response.candidate_roles) && response.candidate_roles.length > 0) {
        const sectionLabel = SECTION_LABELS[fsmState] ?? `Section (${fsmState})`
        setProgressSnapshots((prev) => {
          const already = prev.some((s) => s.sectionLabel === sectionLabel)
          if (already) {
            return prev.map((s) =>
              s.sectionLabel === sectionLabel
                ? { ...s, roles: response.candidate_roles }
                : s,
            )
          }
          return [...prev, { sectionLabel, roles: response.candidate_roles }]
        })
      }

      if (response.status === 'ready_to_predict') {
        const results = await predict(sessionId)
        sessionStorage.setItem(RESULTS_KEY, JSON.stringify(results))
        navigate('/results', { state: { results } })
      } else if (response.questions && response.questions.length > 0) {
        if (response.answers_count != null) {
          setTotalAnswered(response.answers_count)
        } else {
          setTotalAnswered((prev) => prev + questions.length)
        }
        setQuestions(response.questions)
        setAnswers({})
        setCurrentIndex(0)
      }
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to submit answers. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }, [isSubmitting, sessionId, isLastInBatch, allBatchAnswered, questions, answers, navigate, fsmState])

  if (phase === 'idle') {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-lg p-8 text-center space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400">
            <Sparkles className="h-6 w-6" />
          </div>
          <h1 className="text-xl font-bold text-slate-900 dark:text-white">
            RoleCompass Assessment
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Answer the following questions to discover your best-fit IT job role.
          </p>
          <div className="pt-2">
            <button type="button" onClick={handleBegin} className="btn-primary px-6 py-2.5">
              Start Assessment
            </button>
          </div>
        </div>
      </Layout>
    )
  }

  if (phase === 'loading') {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-sm p-8 text-center space-y-3">
          <Loader2 className="mx-auto h-8 w-8 animate-spin text-indigo-600 dark:text-indigo-400" />
          <p className="text-sm text-slate-600 dark:text-slate-400">Loading questions...</p>
        </div>
      </Layout>
    )
  }

  if (phase === 'error') {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-md border-rose-200 p-6 text-center dark:border-rose-900/50 space-y-3">
          <p className="text-sm font-semibold text-rose-600 dark:text-rose-400">{error}</p>
          <button
            type="button"
            onClick={() => { setPhase('idle'); setError(null) }}
            className="btn-primary px-4 py-2 text-xs"
          >
            Try Again
          </button>
        </div>
      </Layout>
    )
  }

  const isSection2 = fsmState === 'SECTION_2_TECH_CORE'
  const isSingleQuestionBatch = questions.length === 1
  const submitLabel = isSingleQuestionBatch ? 'Continue' : 'Submit Answers'

  return (
    <Layout
      isAssessmentActive={phase === 'active'}
      onExitConfirm={handleExitConfirm}
    >
      <div className="mx-auto w-full max-w-2xl space-y-5">
        <SectionBanner fsmState={fsmState} subsectionLabel={currentQuestion?.subsection_label} />

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-3 py-1 text-xs font-bold text-white shadow-sm">
                Question {questionNumber}
              </span>
              {currentQuestion?.subsection_label && (
                <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                  {currentQuestion.subsection_label}
                </span>
              )}
            </div>
            <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
              {Math.round(progress)}% Completed
            </span>
          </div>

          <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
            <div
              className="h-full bg-gradient-to-r from-indigo-500 to-cyan-400 transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>

          {isSection2 && (
            <div className="flex items-center justify-between rounded-lg border border-indigo-100 bg-indigo-50/70 px-3 py-1.5 text-[11px] text-indigo-800 dark:border-indigo-900/50 dark:bg-indigo-950/30 dark:text-indigo-300">
              <span className="flex items-center gap-1.5">
                <span className="text-amber-500 font-bold">&#x26A1;</span>
                <span>Adaptive Skip Active: Extreme responses (1, 2, 4, 5) resolve the domain instantly &amp; skip follow-up questions.</span>
              </span>
            </div>
          )}

          {questions.length > 1 && (
            <div className="flex items-center gap-1.5">
              {questions.map((q, idx) => {
                const isAnswered = answers[q.id] != null
                const isCurrent = idx === currentIndex
                return (
                  <button
                    key={q.id}
                    type="button"
                    onClick={() => setCurrentIndex(idx)}
                    disabled={isSubmitting}
                    aria-label={`Go to question ${idx + 1}`}
                    className={`h-2 flex-1 rounded-full transition-all duration-200 ${
                      isCurrent
                        ? 'bg-indigo-600 scale-y-125'
                        : isAnswered
                          ? 'bg-emerald-500'
                          : 'bg-slate-300 dark:bg-slate-700'
                    }`}
                  />
                )
              })}
              <span className="ml-2 text-[10px] font-medium text-slate-500 dark:text-slate-400 whitespace-nowrap">
                {questions.filter((q) => answers[q.id] != null).length}/{questions.length} answered
              </span>
            </div>
          )}
        </div>

        <div className="surface-card p-6 sm:p-7 space-y-6">
          <div className="space-y-2">
            {currentQuestion?.section_label && (
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                <span>{currentQuestion.section_label}</span>
              </div>
            )}
            <h2 className="text-lg sm:text-xl font-semibold leading-relaxed text-slate-900 dark:text-white">
              {currentQuestion?.text?.includes('| Option A:')
                ? currentQuestion.text.split('|')[0].trim()
                : currentQuestion?.text}
            </h2>
            <p className="text-xs text-slate-400 dark:text-slate-500">
              {(() => {
                const responseType = currentQuestion?.response_type || RESPONSE_TYPES.LIKERT_5
                if (responseType === RESPONSE_TYPES.INTEREST_4) {
                  return 'Select how interested you would be in doing this task:'
                }
                if (responseType === RESPONSE_TYPES.PREFERENCE_4) {
                  return 'Compare the two options below and pick the direction that resonates more:'
                }
                return 'Select how accurately this statement describes you:'
              })()}
            </p>
          </div>

          {currentQuestion && (() => {
            const responseType = currentQuestion.response_type || RESPONSE_TYPES.LIKERT_5
            if (responseType === RESPONSE_TYPES.INTEREST_4) {
              return (
                <InterestScale
                  selectedValue={selectedValue}
                  onSelect={handleSelect}
                  disabled={isSubmitting}
                />
              )
            }
            if (responseType === RESPONSE_TYPES.PREFERENCE_4) {
              return (
                <PreferenceScale
                  questionText={currentQuestion.text}
                  selectedValue={selectedValue}
                  onSelect={handleSelect}
                  disabled={isSubmitting}
                />
              )
            }
            // Default: Section 1 descriptive fit Likert
            return (
              <LikertScale
                options={currentQuestion.options}
                selectedValue={selectedValue}
                onSelect={handleSelect}
                disabled={isSubmitting}
              />
            )
          })()}

          {showUnansweredWarning && unansweredInBatch > 0 && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-700 dark:border-amber-800/50 dark:bg-amber-950/30 dark:text-amber-300">
              Warning: Please answer {unansweredInBatch === 1 ? 'the remaining question' : `all ${unansweredInBatch} remaining questions`} in this batch before continuing.
            </div>
          )}

          {error && (
            <div className="rounded-lg bg-rose-50 p-3 text-xs text-rose-600 dark:bg-rose-950/40 dark:text-rose-400">
              {error}
            </div>
          )}

          <div className="flex items-center justify-between border-t border-slate-200 pt-4 dark:border-slate-800">
            <button
              type="button"
              disabled={currentIndex === 0 || isSubmitting}
              onClick={handlePrev}
              className="btn-secondary gap-1.5 text-xs"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Previous</span>
            </button>

            {!isLastInBatch ? (
              <button
                type="button"
                disabled={!canGoNext}
                onClick={handleNext}
                className="btn-primary gap-1.5 text-xs"
              >
                <span>Next</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            ) : (
              <button
                type="button"
                disabled={isSubmitting || (!allBatchAnswered && !isSubmitting)}
                onClick={handleSubmit}
                className={`btn-primary gap-1.5 text-xs ${
                  isSingleQuestionBatch ? '' : 'bg-emerald-600 hover:bg-emerald-500'
                } ${!allBatchAnswered && !isSubmitting ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Submitting...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="h-4 w-4" />
                    <span>{submitLabel}</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>

        {/* Show Progress Panel */}
        {progressSnapshots.length > 0 && (
          <div className="rounded-xl border border-slate-200 bg-white/60 backdrop-blur-sm dark:border-slate-700/50 dark:bg-slate-900/50">
            <button
              id="toggle-progress-panel"
              type="button"
              onClick={() => setShowProgress((prev) => !prev)}
              className="flex w-full items-center justify-between px-5 py-3.5 text-left"
              aria-expanded={showProgress}
              aria-controls="progress-panel-content"
            >
              <span className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-200">
                <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-indigo-100 text-[10px] font-bold text-indigo-700 dark:bg-indigo-900/60 dark:text-indigo-300">
                  {progressSnapshots.length}
                </span>
                Show Progress
              </span>
              {showProgress ? (
                <ChevronUp className="h-4 w-4 text-slate-500" />
              ) : (
                <ChevronDown className="h-4 w-4 text-slate-500" />
              )}
            </button>

            {showProgress && (
              <div
                id="progress-panel-content"
                className="border-t border-slate-200 dark:border-slate-700/50 px-5 py-4 space-y-5"
              >
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  The <span className="font-semibold text-indigo-600 dark:text-indigo-400">Adaptive Routing Engine</span> (server-side) eliminates roles that do not match your psychometric and technical profile. The list below shows which roles survived after each section.
                </p>

                <div className="space-y-4">
                  {progressSnapshots.map((snapshot, snapshotIdx) => {
                    const isInitial = snapshotIdx === 0
                    const prevRoles = snapshotIdx > 0 ? progressSnapshots[snapshotIdx - 1].roles : ALL_ROLES
                    const eliminated = prevRoles.filter((r) => !snapshot.roles.includes(r))

                    return (
                      <div key={snapshot.sectionLabel} className="space-y-2">
                        <div className="flex flex-wrap items-center gap-2">
                          <div
                            className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                              isInitial
                                ? 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                                : 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/60 dark:text-indigo-300'
                            }`}
                          >
                            {isInitial ? '*' : snapshotIdx}
                          </div>
                          <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                            {snapshot.sectionLabel}
                          </span>
                          <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300">
                            {snapshot.roles.length} remaining
                          </span>
                          {!isInitial && eliminated.length > 0 && (
                            <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-semibold text-rose-700 dark:bg-rose-900/40 dark:text-rose-300">
                              -{eliminated.length} eliminated
                            </span>
                          )}
                        </div>

                        <div className="flex flex-wrap gap-1.5 pl-8">
                          {ALL_ROLES.map((role) => {
                            const isRemaining = snapshot.roles.includes(role)
                            const wasJustEliminated = !isInitial && eliminated.includes(role)
                            if (!isInitial && !isRemaining && !wasJustEliminated) return null
                            return (
                              <span
                                key={role}
                                className={`rounded-lg px-2.5 py-1 text-[11px] font-medium ${
                                  wasJustEliminated
                                    ? 'bg-rose-100 text-rose-600 line-through dark:bg-rose-900/30 dark:text-rose-400'
                                    : isRemaining
                                      ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                                      : 'bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
                                }`}
                              >
                                {role}
                              </span>
                            )
                          })}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Layout>
  )
}
