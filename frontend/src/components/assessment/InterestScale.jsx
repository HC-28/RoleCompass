import { INTEREST_OPTIONS } from '../../constants/assessment.constants'

/**
 * InterestScale — 4-option interest + exposure scale for Sections 2 & 4.
 *
 * Psychology rationale: Technical questions are asked about domains a student
 * may not yet have studied. A 5-point Likert creates a "neutral dead zone"
 * where the student clicks 3 simply because they don't know the technology —
 * not because they're genuinely neutral. This 4-option scale forces a
 * directional signal: "I'm curious / learning" captures genuine passion for
 * something not yet practiced, eliminating familiarity bias entirely.
 *
 * Option → Likert value mapping (preserves backend aggregation math):
 *   🔥 Love it           → 5
 *   💡 Genuinely curious → 4
 *   🤷 Can work with it  → 2
 *   ❌ Not for me        → 1
 */
export default function InterestScale({ selectedValue, onSelect, disabled = false }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4" role="radiogroup" aria-label="Interest level">
      {INTEREST_OPTIONS.map(({ value, emoji, label }) => {
        const isSelected = selectedValue === value

        return (
          <button
            key={value}
            type="button"
            role="radio"
            disabled={disabled}
            onClick={() => onSelect(value)}
            aria-checked={isSelected}
            aria-label={label}
            className={`flex flex-col items-center justify-center gap-1.5 rounded-xl border py-3 px-2 text-center transition-all duration-150
              ${isSelected
                ? 'border-indigo-600 bg-indigo-50/95 text-indigo-950 dark:border-indigo-500 dark:bg-indigo-950/60 dark:text-white ring-2 ring-indigo-400/40 shadow-sm'
                : 'border-slate-200 bg-white/70 text-slate-700 hover:border-slate-300 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900/50 dark:text-slate-200 dark:hover:bg-slate-800/80'
              } ${disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer active:scale-95'}`}
          >
            {/* Emoji indicator */}
            <span className="text-2xl leading-none" aria-hidden="true">{emoji}</span>

            {/* Label */}
            <span className={`text-xs font-semibold leading-tight ${isSelected ? 'text-indigo-700 dark:text-indigo-300' : ''}`}>
              {label}
            </span>
          </button>
        )
      })}
    </div>
  )
}
