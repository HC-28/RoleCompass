import { FSM_SECTION_LABELS, TOTAL_SECTIONS } from '../../constants/assessment.constants'

/**
 * SectionBanner
 *
 * Displays the current section number, section label, subsection label (Section 2 only),
 * and a mini section step-tracker. Reads fsmState and subsectionLabel from the current
 * question delivered by the backend.
 *
 * Props:
 *   fsmState       {string}  — FSM state name from API, e.g. "SECTION_2_TECH_CORE"
 *   subsectionLabel {string|null} — Subsection label from current QuestionDTO, e.g. "Server Logic"
 */
export default function SectionBanner({ fsmState, subsectionLabel }) {
  const meta = FSM_SECTION_LABELS[fsmState] ?? FSM_SECTION_LABELS['SECTION_1_RIASEC']
  const { number: sectionNumber, label: sectionLabel, description } = meta

  return (
    <div className="mb-5 rounded-2xl border border-slate-200/60 bg-white/60 px-5 py-4 backdrop-blur-sm dark:border-slate-800/60 dark:bg-slate-900/60">
      {/* Section step tracker */}
      <div className="mb-3 flex items-center gap-2">
        {Array.from({ length: TOTAL_SECTIONS }, (_, i) => {
          const stepNum = i + 1
          const isActive = stepNum === sectionNumber
          const isDone   = stepNum < sectionNumber
          return (
            <div key={stepNum} className="flex items-center gap-2">
              <div
                className={[
                  'flex h-6 w-6 items-center justify-center rounded-full text-[11px] font-bold transition-all',
                  isActive
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/30 dark:bg-indigo-500'
                    : isDone
                      ? 'bg-emerald-500 text-white dark:bg-emerald-600'
                      : 'bg-slate-200 text-slate-500 dark:bg-slate-700 dark:text-slate-400',
                ].join(' ')}
              >
                {isDone ? '✓' : stepNum}
              </div>
              {stepNum < TOTAL_SECTIONS && (
                <div
                  className={[
                    'h-0.5 w-6 rounded-full transition-all',
                    isDone
                      ? 'bg-emerald-400 dark:bg-emerald-600'
                      : 'bg-slate-200 dark:bg-slate-700',
                  ].join(' ')}
                />
              )}
            </div>
          )
        })}
      </div>

      {/* Section label */}
      <div className="flex flex-col gap-0.5">
        <div className="flex items-center gap-2">
          <span className="text-[11px] font-semibold uppercase tracking-widest text-indigo-600 dark:text-indigo-400">
            Section {sectionNumber}
          </span>
          {subsectionLabel && (
            <>
              <span className="text-slate-300 dark:text-slate-600">·</span>
              <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                {subsectionLabel}
              </span>
            </>
          )}
        </div>
        <p className="text-sm font-semibold text-slate-800 dark:text-white">
          {sectionLabel}
        </p>
        <p className="text-xs text-slate-500 dark:text-slate-400">{description}</p>
      </div>
    </div>
  )
}
