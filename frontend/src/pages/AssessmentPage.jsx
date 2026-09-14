import { ArrowLeft, ArrowRight, CheckCircle2, Loader2, Play, Sparkles } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { predict, startSession, submitAnswers } from '../api/session'
import Layout from '../components/Layout'
import LikertScale from '../components/assessment/LikertScale'
import SectionBanner from '../components/assessment/SectionBanner'
import { ASSESSMENT_BASELINE_TOTAL } from '../constants/assessment.constants'
import { extractErrorMessage } from '../utils/validation'

const RESULTS_KEY = 'rolecompass_results'

/**
 * AssessmentPage
 *
 * Orchestrates the full 4-section assessment flow:
 *   - Section 1 (4 questions per batch)
 *   - Section 2 (1 question per API call — adaptive skip fires between each question)
 *   - Section 3 (batch delivery, conditional)
 *   - Section 4 (batch delivery, conditional)
 *
 * Key invariant: `totalAnswered` always reflects cumulative questions answered
 * across all submitted batches. `currentIndex` is the 0-based position within
 * the current in-memory batch. The displayed question number is always:
 *
 *   totalAnswered + currentIndex + 1
 *
 * This is correct even when Section 2 delivers 1 question at a time (batchSize=1,
 * so currentIndex is always 0 during Section 2).
 */
export default function AssessmentPage() {
  const navigate = useNavigate()

  // 'idle' | 'loading' | 'active' | 'error'
  const [phase, setPhase] = useState('idle')
  const [sessionId, setSessionId] = useState(null)
  const [questions, setQuestions] = useState([])       // current in-memory batch
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})           // answers for current batch only
  const [totalAnswered, setTotalAnswered] = useState(0)// cumulative answered
  const [fsmState, setFsmState] = useState('SECTION_1_RIASEC')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  // ─── Derived state ─────────────────────────────────────────────────────────

  const currentQuestion = questions[currentIndex]
  const selectedValue   = currentQuestion ? (answers[currentQuestion.id] ?? null) : null
  const isLastInBatch   = currentIndex === questions.length - 1

  // Cumulative question number shown to the user (never resets)
  const questionNumber = totalAnswered + currentIndex + 1
  const progress = Math.min((questionNumber / ASSESSMENT_BASELINE_TOTAL) * 100, 100)

  const canProceed = useMemo(
    () => selectedValue !== null && !isSubmitting,
    [selectedValue, isSubmitting],
  )

  // Subsection label comes from the current question (Section 2 only)
  const subsectionLabel = currentQuestion?.subsection_label ?? null

  // ─── Session Start ──────────────────────────────────────────────────────────

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

  // ─── Within-batch navigation ───────────────────────────────────────────────

  const handleSelect = useCallback(
    (value) => {
      if (!currentQuestion) return
      setAnswers((prev) => ({ ...prev, [currentQuestion.id]: value }))
    },
    [currentQuestion],
  )

  const handleNext = () => {
    if (!canProceed || isLastInBatch) return
    setCurrentIndex((prev) => prev + 1)
  }

  // ─── Batch submission & next delivery ─────────────────────────────────────

  /**
   * Submits all answered questions in the current batch and loads the next one.
   * For Section 2 the "batch" is a single question, so this fires after every answer.
   * For Sections 1, 3, 4 it fires after the last question in the 4-question batch.
   */
  const handleSubmit = async () => {
    if (!canProceed || !sessionId || !isLastInBatch) return

    setIsSubmitting(true)
    setError(null)

    try {
      const payload = {
        answers: questions.map((question) => ({
          question_id: question.id,
          likert_value: answers[question.id],
        })),
      }

      const response = await submitAnswers(sessionId, payload)

      // Update FSM state from response so SectionBanner reflects the transition
      if (response.fsm_state) {
        setFsmState(response.fsm_state)
      }

      if (response.status === 'ready_to_predict') {
        const results = await predict(sessionId)
        sessionStorage.setItem(RESULTS_KEY, JSON.stringify(results))
        navigate('/results', { state: { results } })
      } else if (response.questions && response.questions.length > 0) {
        // Advance cumulative counter BEFORE resetting currentIndex
        setTotalAnswered((prev) => prev + questions.length)
        setQuestions(response.questions)
        setAnswers({})
        setCurrentIndex(0)
      }
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to submit answers. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // ─── Phase: Idle ────────────────────────────────────────────────────────────

  if (phase === 'idle') {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-xl p-10 text-center space-y-5">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600/10 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400">
            <Sparkles className="h-7 w-7" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-900 dark:text-white">
              Career Role Assessment
            </h2>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
              Answer a series of questions about your interests and thinking style. The engine
              adapts in real time, skipping questions where your signal is already clear.
            </p>
          </div>
          <div className="grid grid-cols-2 gap-3 text-xs text-slate-600 dark:text-slate-400 max-w-xs mx-auto">
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-slate-900">
              <div className="font-bold text-slate-800 dark:text-white">4 Sections</div>
              <div>Personality → Technical → Role Fit → Specialist</div>
            </div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-slate-900">
              <div className="font-bold text-slate-800 dark:text-white">Adaptive</div>
              <div>44–84 questions based on your answers</div>
            </div>
          </div>
          <button
            type="button"
            onClick={handleBegin}
            className="btn-primary gap-2 text-sm mx-auto"
          >
            <Play className="h-4 w-4" />
            Begin Assessment
          </button>
        </div>
      </Layout>
    )
  }

  // ─── Phase: Loading ──────────────────────────────────────────────────────────

  if (phase === 'loading') {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-xl p-12 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-600/10 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
          <h2 className="text-base font-semibold text-slate-900 dark:text-white">
            Preparing your career assessment
          </h2>
          <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
            Loading tailored evaluation questions…
          </p>
        </div>
      </Layout>
    )
  }

  // ─── Phase: Error ────────────────────────────────────────────────────────────

  if (phase === 'error') {
    return (
      <Layout>
        <div className="surface-card mx-auto max-w-xl border-rose-200 p-8 text-center dark:border-rose-900/50 space-y-3">
          <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-rose-100 text-rose-600 dark:bg-rose-950/60 dark:text-rose-400 font-bold text-lg">
            !
          </div>
          <h2 className="text-sm font-semibold text-slate-900 dark:text-white">Assessment Error</h2>
          <p className="text-xs text-rose-600 dark:text-rose-400">{error}</p>
          <button
            type="button"
            onClick={() => { setPhase('idle'); setError(null) }}
            className="btn-primary mt-2 px-5 py-2 text-xs"
          >
            Try Again
          </button>
        </div>
      </Layout>
    )
  }

  // ─── Phase: Active ───────────────────────────────────────────────────────────

  return (
    <Layout>
      <div className="mx-auto w-full max-w-3xl">

        {/* Section Banner */}
        <SectionBanner fsmState={fsmState} subsectionLabel={subsectionLabel} />

        {/* Progress bar and cumulative counter */}
        <div className="mb-6">
          <div className="mb-2 flex items-center justify-between text-xs font-medium text-slate-500 dark:text-slate-400">
            <span className="flex items-center gap-1.5">
              <span className="flex h-2 w-2 rounded-full bg-indigo-600 dark:bg-indigo-400" />
              Question {questionNumber} of ~{ASSESSMENT_BASELINE_TOTAL}
            </span>
            <span>{Math.round(progress)}% Completed</span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-200/80 dark:bg-slate-800">
            <div
              className="h-full rounded-full bg-indigo-600 transition-all duration-300 ease-out dark:bg-indigo-500"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        {/* Assessment Card */}
        <div className="surface-card overflow-hidden">
          {/* Card Header */}
          <div className="border-b border-slate-200/80 px-6 py-6 sm:px-8 sm:py-7 dark:border-slate-800/80">
            <div className="inline-flex items-center gap-1.5 rounded-md bg-indigo-50 px-2.5 py-1 text-[11px] font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
              <Sparkles className="h-3 w-3" />
              Question {questionNumber}
            </div>
            <h1 className="mt-3 text-lg font-semibold leading-snug text-slate-900 sm:text-xl dark:text-white">
              {currentQuestion?.text}
            </h1>
          </div>

          {/* Card Body */}
          <div className="px-6 py-6 sm:px-8 sm:py-8">
            <p className="mb-4 text-xs font-medium text-slate-500 dark:text-slate-400">
              Rate how strongly you identify with the statement:
            </p>

            {currentQuestion && (
              <LikertScale
                options={currentQuestion.options}
                selectedValue={selectedValue}
                onSelect={handleSelect}
                disabled={isSubmitting}
              />
            )}

            {error && (
              <div className="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-300">
                {error}
              </div>
            )}

            {/* Navigation Footer */}
            <div className="mt-8 flex items-center justify-between border-t border-slate-200/60 pt-6 dark:border-slate-800/60">
              <button
                type="button"
                disabled={currentIndex === 0 || isSubmitting}
                onClick={() => setCurrentIndex((prev) => Math.max(prev - 1, 0))}
                className="btn-secondary gap-1.5 text-xs"
              >
                <ArrowLeft className="h-3.5 w-3.5" />
                Previous
              </button>

              <div>
                {!isLastInBatch ? (
                  <button
                    type="button"
                    disabled={!canProceed}
                    onClick={handleNext}
                    className="btn-primary gap-1.5 text-xs"
                  >
                    Next Question
                    <ArrowRight className="h-3.5 w-3.5" />
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={!canProceed}
                    onClick={handleSubmit}
                    className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-emerald-600 px-5 py-2.5 text-xs font-medium text-white shadow-sm transition hover:bg-emerald-500 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50 dark:bg-emerald-600 dark:hover:bg-emerald-500"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        Processing…
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Submit &amp; Continue
                      </>
                    )}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}
