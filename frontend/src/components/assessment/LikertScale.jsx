import { LIKERT_LABELS } from '../../constants/assessment.constants'

export default function LikertScale({
  options = [1, 2, 3, 4, 5],
  selectedValue,
  onSelect,
  disabled = false,
}) {
  return (
    <div className="grid grid-cols-5 gap-2 sm:gap-3" role="radiogroup">
      {options.map((value) => {
        const isSelected = selectedValue === value

        return (
          <button
            key={value}
            type="button"
            role="radio"
            disabled={disabled}
            onClick={() => onSelect(value)}
            aria-checked={isSelected}
            aria-label={`${LIKERT_LABELS[value]} (${value})`}
            className={`flex flex-col items-center justify-between rounded-xl border p-3 text-center transition duration-150 ${
              isSelected
                ? 'border-indigo-600 bg-indigo-50/80 text-indigo-950 dark:border-indigo-500 dark:bg-indigo-950/40 dark:text-white'
                : 'border-slate-200 bg-white/70 text-slate-700 hover:border-slate-300 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900/50 dark:text-slate-300 dark:hover:bg-slate-800'
            } ${disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer active:scale-95'}`}
          >
            <span
              className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold ${
                isSelected
                  ? 'bg-indigo-600 text-white dark:bg-indigo-500'
                  : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
              }`}
            >
              {value}
            </span>
            <span className="mt-2 text-xs font-medium leading-tight">
              {LIKERT_LABELS[value]}
            </span>
          </button>
        )
      })}
    </div>
  )
}
