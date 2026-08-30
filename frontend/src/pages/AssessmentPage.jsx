import { ArrowLeft, ArrowRight, CheckCircle2, Loader2, Play, Sparkles } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { predict, startSession, submitAnswers } from '../api/session'
import Layout from '../components/Layout'
import LikertScale from '../components/LikertScale'
import { extractErrorMessage } from '../utils/validation'

const RESULTS_KEY = 'rolecompass_results'

export default function AssessmentPage() {
  const navigate = useNavigate()

  // 'idle' | 'loading' | 'active' | 'error'
  // A session is only created when the user explicitly clicks "Begin Assessment".
  // This prevents ghost empty sessions from appearing in history every time the
  // user navigates to this page (e.g. via the "Take New Assessment" link).
  const [phase, setPhase] = useState('idle')
  const [sessionId, setSessionId] = useState(null)
  const [questions, setQuestions] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const handleBegin = async () => {
    setPhase('loading')
    setError(null)
    try {
      const response = await startSession()
      setSessionId(response.session_id)
      setQuestions(response.questions || [])
      setPhase('active')
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to start assessment. Please try again.'))
      setPhase('error')
    }
  }

  const currentQuestion = questions[currentIndex]
  const selectedValue = currentQuestion ? (answers[currentQuestion.id] ?? null) : null
  const isLastQuestion = currentIndex === questions.length - 1
  const progress = questions.length > 0 ? ((currentIndex + 1) / questions.length) * 100 : 0

  const canProceed = useMemo(
    () => selectedValue !== null && !isSubmitting,
    [selectedValue, isSubmitting],
  )

  const handleSelect = useCallback(
    (value) => {
      if (!currentQuestion) {
        return
      }
      setAnswers((prev) => ({ ...prev, [currentQuestion.id]: value }))
    },
    [currentQuestion],
  )

  const handleNext = () => {
    if (!canProceed || isLastQuestion) {
      return
    }
    setCurrentIndex((prev) => prev + 1)
  }

  const handleSubmit = async () => {
    if (!canProceed || !sessionId || !isLastQuestion) {
      return
    }

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

      if (response.status === 'ready_to_predict') {
        const results = await predict(sessionId)
        sessionStorage.setItem(RESULTS_KEY, JSON.stringify(results))
        navigate('/results', { state: { results } })
      } else if (response.questions && response.questions.length > 0) {
        setQuestions(response.questions)
        setCurrentIndex(0)
      }
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to submit assessment. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // ── Phase: Idle — landing / confirmation card ──────────────────────────────
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
              Answer a series of questions about your interests and skills. The system will
              adaptively route you to the most relevant questions and predict your best-fit IT
              role.
            </p>
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

  // ── Phase: Loading ─────────────────────────────────────────────────────────
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

  // ── Phase: Error ───────────────────────────────────────────────────────────
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

  // ── Phase: Active — the question flow ─────────────────────────────────────
  return (
    <Layout>
      <div className="mx-auto w-full max-w-3xl">
        {/* Progress bar and counter */}
        <div className="mb-6">
          <div className="mb-2 flex items-center justify-between text-xs font-medium text-slate-500 dark:text-slate-400">
            <span className="flex items-center gap-1.5">
              <span className="flex h-2 w-2 rounded-full bg-indigo-600 dark:bg-indigo-400" />
              Question {currentIndex + 1} of {questions.length}
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
              Evaluation Question {currentIndex + 1}
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
                {!isLastQuestion ? (
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
                        Processing Answers…
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
