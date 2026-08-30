export const LIKERT_LABELS = {
  1: 'Strongly Disagree',
  2: 'Disagree',
  3: 'Neutral',
  4: 'Agree',
  5: 'Strongly Agree',
}

export default function LikertScale({
  options,
  selectedValue,
  onSelect,
  disabled = false,
}) {
  return (
    <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-5">
      {options.map((value) => {
        const isSelected = selectedValue === value

        return (
          <button
            key={value}
            type="button"
            disabled={disabled}
            onClick={() => onSelect(value)}
            className={[
              'group relative flex flex-col items-center justify-between gap-3 rounded-xl border p-4 text-center transition duration-150',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-50 dark:focus-visible:ring-offset-slate-900',
              isSelected
                ? 'border-indigo-600 bg-indigo-50/80 text-indigo-950 shadow-sm shadow-indigo-500/10 dark:border-indigo-500 dark:bg-indigo-950/40 dark:text-white dark:shadow-glow-indigo'
                : 'border-slate-200/80 bg-white/70 text-slate-700 hover:border-slate-300 hover:bg-slate-50/80 dark:border-slate-800 dark:bg-slate-900/50 dark:text-slate-300 dark:hover:border-slate-700 dark:hover:bg-slate-800/60',
              disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer active:scale-[0.98]',
            ].join(' ')}
          >
            {/* Number indicator badge */}
            <span
              className={[
                'flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold transition duration-150',
                isSelected
                  ? 'bg-indigo-600 text-white dark:bg-indigo-500'
                  : 'bg-slate-100 text-slate-600 group-hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400 dark:group-hover:bg-slate-700',
              ].join(' ')}
            >
              {value}
            </span>

            {/* Descriptive label */}
            <span
              className={[
                'text-xs font-medium leading-tight transition duration-150',
                isSelected
                  ? 'text-indigo-900 dark:text-indigo-200'
                  : 'text-slate-600 group-hover:text-slate-900 dark:text-slate-400 dark:group-hover:text-slate-200',
              ].join(' ')}
            >
              {LIKERT_LABELS[value]}
            </span>
          </button>
        )
      })}
    </div>
  )
}
