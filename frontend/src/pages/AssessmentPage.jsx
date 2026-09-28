import { ArrowLeft, ArrowRight, CheckCircle2, Loader2, Sparkles } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { predict, startSession, submitAnswers } from '../api/session'
import Layout from '../components/Layout'
import LikertScale from '../components/assessment/LikertScale'
import SectionBanner from '../components/assessment/SectionBanner'
import { ASSESSMENT_BASELINE_TOTAL } from '../constants/assessment.constants'
import { extractErrorMessage } from '../utils/validation'

const RESULTS_KEY = 'rolecompass_results'

export default function AssessmentPage() {
  const navigate = useNavigate()

  const [phase, setPhase] = useState('idle') // 'idle' | 'loading' | 'active' | 'error'
  const [sessionId, setSessionId] = useState(null)
  const [questions, setQuestions] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [totalAnswered, setTotalAnswered] = useState(0)
  const [fsmState, setFsmState] = useState('SECTION_1_RIASEC')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [showUnansweredWarning, setShowUnansweredWarning] = useState(false)

  const currentQuestion = questions[currentIndex]
  const selectedValue = currentQuestion ? (answers[currentQuestion.id] ?? null) : null
  const isLastInBatch = currentIndex === questions.length - 1

  // ── Batch completion checks ────────────────────────────────────────────────
  /** True only when every question in the current batch has been answered. */
  const allBatchAnswered = useMemo(
    () => questions.length > 0 && questions.every((q) => answers[q.id] != null),
    [questions, answers],
  )

  /**
   * Can navigate to the next question in the batch:
   * - Current question must be answered.
   * - Not already on the last question.
   * - Not submitting.
   */
  const canGoNext = useMemo(
    () => selectedValue !== null && !isLastInBatch && !isSubmitting,
    [selectedValue, isLastInBatch, isSubmitting],
  )

  /**
   * Can submit the batch:
   * - ALL batch questions must be answered.
   * - Must be on the last question (otherwise use Next).
   * - Not already submitting.
   */
  const canSubmit = useMemo(
    () => allBatchAnswered && isLastInBatch && !isSubmitting,
    [allBatchAnswered, isLastInBatch, isSubmitting],
  )

  // Progress bar — based on total answered + current position within batch
  const questionNumber = totalAnswered + currentIndex + 1
  const progress = Math.min((questionNumber / ASSESSMENT_BASELINE_TOTAL) * 100, 100)

  // How many batch questions still need an answer (for warning display)
  const unansweredInBatch = useMemo(
    () => questions.filter((q) => answers[q.id] == null).length,
    [questions, answers],
  )

  // ── Session Start ──────────────────────────────────────────────────────────
  const handleBegin = async () => {
    setPhase('loading')
    setError(null)
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

  // ── Answer Selection ───────────────────────────────────────────────────────
  const handleSelect = useCallback(
    (value) => {
      if (!currentQuestion) return
      setShowUnansweredWarning(false)
      setAnswers((prev) => ({ ...prev, [currentQuestion.id]: value }))
    },
    [currentQuestion],
  )

  // ── Navigation ─────────────────────────────────────────────────────────────
  const handleNext = useCallback(() => {
    if (!canGoNext) return
    setCurrentIndex((prev) => prev + 1)
  }, [canGoNext])

  const handlePrev = useCallback(() => {
    if (currentIndex === 0 || isSubmitting) return
    setShowUnansweredWarning(false)
    setCurrentIndex((prev) => Math.max(prev - 1, 0))
  }, [currentIndex, isSubmitting])

  // ── Batch Submit ───────────────────────────────────────────────────────────
  const handleSubmit = useCallback(async () => {
    if (isSubmitting || !sessionId || !isLastInBatch) return

    // Guard: all batch questions must be answered before submitting
    if (!allBatchAnswered) {
      setShowUnansweredWarning(true)
      // Navigate to the first unanswered question in the batch so the user can see it
      const firstUnansweredIdx = questions.findIndex((q) => answers[q.id] == null)
      if (firstUnansweredIdx >= 0) {
        setCurrentIndex(firstUnansweredIdx)
      }
      return
    }

    setShowUnansweredWarning(false)
    setIsSubmitting(true)
    setError(null)

    try {
      // Build payload: only include questions that have been answered (safety net)
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
  }, [isSubmitting, sessionId, isLastInBatch, allBatchAnswered, questions, answers, navigate])

  // ── Idle Screen ────────────────────────────────────────────────────────────
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
            onClick={() => {
              setPhase('idle')
              setError(null)
            }}
            className="btn-primary px-4 py-2 text-xs"
          >
            Try Again
          </button>
        </div>
      </Layout>
    )
  }

  // ── Active Assessment ──────────────────────────────────────────────────────
  const isSection2 = fsmState === 'SECTION_2_TECH_CORE'

  // For single-question batches (Section 2), the "submit" action advances to the next
  // question rather than the final submission — label it "Continue" to avoid confusion.
  const isSingleQuestionBatch = questions.length === 1
  const submitLabel = isSingleQuestionBatch ? 'Continue' : 'Submit Answers'

  return (
    <Layout>
      <div className="mx-auto w-full max-w-2xl space-y-5">
        {/* Section Banner */}
        <SectionBanner fsmState={fsmState} subsectionLabel={currentQuestion?.subsection_label} />

        {/* Progress & Question Header */}
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

          {/* Adaptive Skip Explanatory Banner in Section 2 */}
          {isSection2 && (
            <div className="flex items-center justify-between rounded-lg border border-indigo-100 bg-indigo-50/70 px-3 py-1.5 text-[11px] text-indigo-800 dark:border-indigo-900/50 dark:bg-indigo-950/30 dark:text-indigo-300">
              <span className="flex items-center gap-1.5">
                <span className="text-amber-500 font-bold">⚡</span>
                <span>Adaptive Skip Active: Extreme responses (1, 2, 4, 5) resolve the domain instantly &amp; skip follow-up questions.</span>
              </span>
            </div>
          )}

          {/* Batch progress indicator — only shown for multi-question batches */}
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

        {/* Question Card */}
        <div className="surface-card p-6 sm:p-7 space-y-6">
          <div className="space-y-2">
            {currentQuestion?.section_label && (
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                <span>{currentQuestion.section_label}</span>
              </div>
            )}
            <h2 className="text-lg sm:text-xl font-semibold leading-relaxed text-slate-900 dark:text-white">
              {currentQuestion?.text}
            </h2>
          </div>

          {currentQuestion && (
            <LikertScale
              options={currentQuestion.options}
              selectedValue={selectedValue}
              onSelect={handleSelect}
              disabled={isSubmitting}
            />
          )}

          {/* Unanswered questions warning */}
          {showUnansweredWarning && unansweredInBatch > 0 && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-700 dark:border-amber-800/50 dark:bg-amber-950/30 dark:text-amber-300">
              ⚠️ Please answer {unansweredInBatch === 1 ? 'the remaining question' : `all ${unansweredInBatch} remaining questions`} in this batch before continuing.
            </div>
          )}

          {error && (
            <div className="rounded-lg bg-rose-50 p-3 text-xs text-rose-600 dark:bg-rose-950/40 dark:text-rose-400">
              {error}
            </div>
          )}

          {/* Navigation Controls */}
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
      </div>
    </Layout>
  )
}
