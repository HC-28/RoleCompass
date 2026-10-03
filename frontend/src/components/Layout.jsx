import Header from './Header'

export default function Layout({ children, isAssessmentActive = false, onExitConfirm }) {
  return (
    <div className="app-background flex min-h-screen flex-col font-sans selection:bg-indigo-500/20 selection:text-indigo-600 dark:selection:bg-indigo-500/30 dark:selection:text-indigo-400">
      {/* Ambient background mesh gradient effects */}
      <div className="pointer-events-none fixed inset-0 z-0 overflow-hidden" aria-hidden="true">
        {/* Top center indigo glow */}
        <div className="absolute -top-[20%] left-1/2 h-[500px] w-[800px] -translate-x-1/2 rounded-full bg-gradient-to-b from-indigo-500/15 via-purple-500/10 to-transparent blur-3xl dark:from-indigo-600/25 dark:via-blue-600/15" />
        {/* Left cyan accent */}
        <div className="absolute top-[30%] -left-[10%] h-[450px] w-[500px] rounded-full bg-gradient-to-tr from-cyan-500/10 to-transparent blur-3xl dark:from-cyan-500/15" />
        {/* Right violet accent */}
        <div className="absolute top-[45%] -right-[10%] h-[450px] w-[500px] rounded-full bg-gradient-to-tl from-violet-500/10 to-transparent blur-3xl dark:from-violet-600/20" />
        {/* Subtle grid pattern overlay */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#8080800a_1px,transparent_1px),linear-gradient(to_bottom,#8080800a_1px,transparent_1px)] bg-[size:32px_32px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] dark:bg-[linear-gradient(to_right,#ffffff05_1px,transparent_1px),linear-gradient(to_bottom,#ffffff05_1px,transparent_1px)]" />
      </div>

      {/* Main App Canvas */}
      <div className="relative z-10 flex min-h-screen flex-col">
        <Header isAssessmentActive={isAssessmentActive} onExitConfirm={onExitConfirm} />

        <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center px-4 pt-20 pb-12 sm:px-6 sm:pt-24 sm:pb-16 lg:px-8">
          {children}
        </main>

        {/* Footer */}
        <footer className="mt-auto border-t border-slate-200 py-4 text-center text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          <div className="mx-auto max-w-5xl px-4 sm:px-6">
            <span>RoleCompass &bull; AI Job Role Prediction</span>
          </div>
        </footer>
      </div>
    </div>
  )
}
