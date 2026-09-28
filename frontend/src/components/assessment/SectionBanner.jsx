import { FSM_SECTION_LABELS, TOTAL_SECTIONS } from '../../constants/assessment.constants'

export default function SectionBanner({ fsmState, subsectionLabel }) {
  const meta = FSM_SECTION_LABELS[fsmState] ?? FSM_SECTION_LABELS['SECTION_1_RIASEC']
  const { number: sectionNumber, label: sectionLabel, description } = meta

  return (
    <div className="surface-card p-4 space-y-3">
      {/* 4-Section Indicator Bar */}
      <div className="grid grid-cols-4 gap-2">
        {Array.from({ length: TOTAL_SECTIONS }, (_, i) => {
          const stepNum = i + 1
          const isCurrent = stepNum === sectionNumber
          const isDone = stepNum < sectionNumber

          return (
            <div
              key={stepNum}
              className={`h-1.5 rounded-full transition ${
                isCurrent
                  ? 'bg-indigo-600'
                  : isDone
                    ? 'bg-emerald-500'
                    : 'bg-slate-200 dark:bg-slate-800'
              }`}
            />
          )
        })}
      </div>

      {/* Section Info */}
      <div>
        <div className="flex items-center gap-2 mb-1">
          <span className="rounded bg-indigo-50 px-2 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300">
            Section {sectionNumber} of {TOTAL_SECTIONS}
          </span>
          {subsectionLabel && (
            <span className="text-xs text-slate-500 dark:text-slate-400">
              &bull; {subsectionLabel}
            </span>
          )}
        </div>
        <h2 className="text-base font-bold text-slate-900 dark:text-white">
          {sectionLabel}
        </h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          {description}
        </p>
      </div>
    </div>
  )
}
