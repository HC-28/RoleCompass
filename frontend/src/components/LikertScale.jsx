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
    <div className="grid gap-3 sm:grid-cols-5">
      {options.map((value) => {
        const isSelected = selectedValue === value

        return (
          <button
            key={value}
            type="button"
            disabled={disabled}
            onClick={() => onSelect(value)}
            className={[
              'group flex flex-col items-center gap-3 rounded-2xl border px-3 py-5 text-center transition',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-400 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900',
              isSelected
                ? 'border-indigo-400 bg-indigo-500/20 shadow-lg shadow-indigo-500/20'
                : 'border-white/10 bg-white/5 hover:border-indigo-400/40 hover:bg-white/10',
              disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
            ].join(' ')}
          >
            <span
              className={[
                'flex h-10 w-10 items-center justify-center rounded-full text-sm font-semibold transition',
                isSelected
                  ? 'bg-indigo-500 text-white'
                  : 'bg-slate-800 text-slate-300 group-hover:bg-indigo-500/80 group-hover:text-white',
              ].join(' ')}
            >
              {value}
            </span>
            <span
              className={[
                'text-xs leading-snug',
                isSelected ? 'text-indigo-100' : 'text-slate-400 group-hover:text-slate-200',
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
