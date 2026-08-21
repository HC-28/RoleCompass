import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { predict, startSession, submitAnswers } from '../api/session'
import Layout from '../components/Layout'
import LikertScale from '../components/LikertScale'
import { extractErrorMessage } from '../utils/validation'

const RESULTS_KEY = 'rolecompass_results'

export default function AssessmentPage() {
  const navigate = useNavigate()

  const [sessionId, setSessionId] = useState(null)
  const [questions, setQuestions] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false

    const loadSession = async () => {
      setIsLoading(true)
      setError(null)
      try {
        const response = await startSession()
        if (!cancelled) {
          setSessionId(response.session_id)
          setQuestions(response.questions)
        }
      } catch (err) {
        if (!cancelled) {
          setError(extractErrorMessage(err, 'Unable to start assessment. Please try again.'))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadSession()
    return () => {
      cancelled = true
    }
  }, [])

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

      await submitAnswers(sessionId, payload)
      const results = await predict(sessionId)

      sessionStorage.setItem(RESULTS_KEY, JSON.stringify(results))
      navigate('/results', { state: { results } })
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to submit assessment. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return (
      <Layout>
        <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-slate-900/60 p-12 text-center backdrop-blur-xl">
          <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-2 border-indigo-400 border-t-transparent" />
          <p className="text-slate-300">Loading your assessment…</p>
        </div>
      </Layout>
    )
  }

  if (error && questions.length === 0) {
    return (
      <Layout>
        <div className="mx-auto max-w-3xl rounded-3xl border border-rose-500/30 bg-rose-500/10 p-8 text-center">
          <p className="text-rose-200">{error}</p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-6 rounded-xl bg-indigo-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-400"
          >
            Retry
          </button>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="mx-auto max-w-4xl">
        <div className="mb-8">
          <div className="mb-3 flex items-center justify-between text-sm text-slate-400">
            <span>
              Question {currentIndex + 1} of {questions.length}
            </span>
            <span>{Math.round(progress)}% complete</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-800">
            <div
              className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-violet-500 transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        <div className="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/70 shadow-2xl shadow-black/30 backdrop-blur-xl">
          <div className="border-b border-white/10 px-6 py-6 sm:px-10 sm:py-8">
            <p className="text-xs font-semibold uppercase tracking-widest text-indigo-400">
              Career assessment
            </p>
            <h1 className="mt-3 text-xl font-medium leading-relaxed text-white sm:text-2xl">
              {currentQuestion?.text}
            </h1>
          </div>

          <div className="px-6 py-8 sm:px-10 sm:py-10">
            {currentQuestion && (
              <LikertScale
                options={currentQuestion.options}
                selectedValue={selectedValue}
                onSelect={handleSelect}
                disabled={isSubmitting}
              />
            )}

            {error && (
              <div className="mt-6 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                {error}
              </div>
            )}

            <div className="mt-10 flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
              <button
                type="button"
                disabled={currentIndex === 0 || isSubmitting}
                onClick={() => setCurrentIndex((prev) => Math.max(prev - 1, 0))}
                className="rounded-xl border border-white/10 px-5 py-3 text-sm font-medium text-slate-300 transition hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Previous
              </button>

              <div className="flex gap-3 sm:justify-end">
                {!isLastQuestion ? (
                  <button
                    type="button"
                    disabled={!canProceed}
                    onClick={handleNext}
                    className="rounded-xl bg-indigo-500 px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Next
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={!canProceed}
                    onClick={handleSubmit}
                    className="rounded-xl bg-emerald-500 px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-emerald-500/30 transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {isSubmitting ? 'Submitting…' : 'Submit'}
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
