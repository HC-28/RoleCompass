import { PREFERENCE_OPTIONS } from '../../constants/assessment.constants'

/**
 * PreferenceScale — 4-option bipolar forced-choice scale for Section 3.
 *
 * Psychology rationale: Section 3 questions are comparative by nature —
 * "I prefer X over Y". Asking "Strongly Agree / Neutral / Strongly Disagree"
 * on a comparison statement breaks semantic coherence: a "Neutral" response
 * means nothing for a resolver whose entire job is to break ties between
 * two matched roles. This 4-option bipolar scale eliminates the neutral
 * option entirely and makes the directionality explicit and visible.
 *
 * The question statement always describes Path A (the first option).
 * Path B is the implicit alternative described in the same sentence.
 *
 * Option → Likert value mapping (preserves backend aggregation math):
 *   Strongly this path      → 5  (strong agreement with statement)
 *   Slightly this path      → 4  (mild agreement)
 *   Slightly the alternative → 2  (mild disagreement)
 *   Strongly the alternative → 1  (strong disagreement)
 */
export default function PreferenceScale({ questionText, selectedValue, onSelect, disabled = false }) {
  // Extract Option A and Option B if encoded in questionText
  let optionA = null
  let optionB = null
  if (questionText && questionText.includes('| Option A:') && questionText.includes('| Option B:')) {
    const parts = questionText.split('|')
    for (const part of parts) {
      const trimmed = part.trim()
      if (trimmed.startsWith('Option A:')) {
        optionA = trimmed.replace('Option A:', '').trim()
      } else if (trimmed.startsWith('Option B:')) {
        optionB = trimmed.replace('Option B:', '').trim()
      }
    }
  }

  return (
    <div className="flex flex-col gap-4" role="radiogroup" aria-label="Career path preference">
      {/* Option A vs Option B Cards */}
      {optionA && optionB && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="p-3.5 rounded-xl border border-indigo-200 bg-indigo-50/60 dark:border-indigo-800/60 dark:bg-indigo-950/30">
            <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700 dark:text-indigo-300 block mb-1">
              Option A
            </span>
            <p className="text-sm font-medium text-slate-800 dark:text-slate-200">
              {optionA}
            </p>
          </div>
          <div className="p-3.5 rounded-xl border border-amber-200 bg-amber-50/60 dark:border-amber-800/60 dark:bg-amber-950/30">
            <span className="text-[10px] font-bold uppercase tracking-wider text-amber-700 dark:text-amber-300 block mb-1">
              Option B
            </span>
            <p className="text-sm font-medium text-slate-800 dark:text-slate-200">
              {optionB}
            </p>
          </div>
        </div>
      )}

      {/* 3 Crisp Path Choice Buttons */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {PREFERENCE_OPTIONS.map(({ value, label, shortLabel }) => {
          const isSelected = selectedValue === value
          const isOptionA = value === 5
          const isNeutral = value === 3
          const isOptionB = value === 1

          return (
            <button
              key={value}
              type="button"
              role="radio"
              disabled={disabled}
              onClick={() => onSelect(value)}
              aria-checked={isSelected}
              aria-label={label}
              className={`flex flex-col items-center justify-center gap-2 rounded-xl border py-4 px-3 text-center transition-all duration-150
                ${isSelected
                  ? isOptionA
                    ? 'border-indigo-600 bg-indigo-50/95 text-indigo-950 ring-2 ring-indigo-400/40 dark:border-indigo-500 dark:bg-indigo-950/60 dark:text-white shadow-md'
                    : isNeutral
                    ? 'border-emerald-600 bg-emerald-50/95 text-emerald-950 ring-2 ring-emerald-400/40 dark:border-emerald-500 dark:bg-emerald-950/60 dark:text-white shadow-md'
                    : 'border-amber-600 bg-amber-50/95 text-amber-950 ring-2 ring-amber-400/40 dark:border-amber-500 dark:bg-amber-950/60 dark:text-white shadow-md'
                  : 'border-slate-200 bg-white/70 text-slate-700 hover:border-slate-300 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900/50 dark:text-slate-200 dark:hover:bg-slate-800/80'
                } ${disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer active:scale-[0.98]'}`}
            >
              {/* Visual indicator tag */}
              <span
                className={`text-[11px] font-bold uppercase tracking-wider ${
                  isSelected
                    ? isOptionA
                      ? 'text-indigo-600 dark:text-indigo-300'
                      : isNeutral
                      ? 'text-emerald-600 dark:text-emerald-300'
                      : 'text-amber-600 dark:text-amber-300'
                    : isOptionA
                    ? 'text-indigo-500 dark:text-indigo-400'
                    : isNeutral
                    ? 'text-emerald-500 dark:text-emerald-400'
                    : 'text-amber-500 dark:text-amber-400'
                }`}
                aria-hidden="true"
              >
                {isOptionA ? 'Prefer Option A' : isNeutral ? 'Equal Interest' : 'Prefer Option B'}
              </span>

              {/* Label */}
              <span className="text-sm font-semibold leading-tight">{label}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
