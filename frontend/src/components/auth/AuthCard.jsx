import { Compass } from 'lucide-react'
import { Link } from 'react-router-dom'

/**
 * AuthCard — shared card shell for Login and Register pages.
 * Renders a branded header (Compass icon + title), a content slot, and a footer link.
 */
export default function AuthCard({
  title,
  subtitle,
  children,
  footerText,
  footerLinkText,
  footerLinkTo,
  maxWidth = 'max-w-md',
}) {
  return (
    <div className={`mx-auto w-full ${maxWidth}`}>
      <div className="surface-card overflow-hidden">
        <div className="border-b border-slate-200/80 px-6 pt-8 pb-6 text-center dark:border-slate-800/80 sm:px-8">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-600/10 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400">
            <Compass className="h-6 w-6" strokeWidth={2.2} />
          </div>
          <h1 className="text-xl font-semibold tracking-tight text-slate-900 dark:text-white sm:text-2xl">
            {title}
          </h1>
          <p className="mt-1.5 text-sm text-slate-600 dark:text-slate-400">{subtitle}</p>
        </div>

        <div className="px-6 py-6 sm:px-8 sm:py-8">{children}</div>

        <div className="border-t border-slate-200/80 bg-slate-50/70 px-6 py-4 text-center text-xs text-slate-600 dark:border-slate-800/80 dark:bg-slate-950/50 dark:text-slate-400 sm:px-8">
          {footerText}{' '}
          <Link
            to={footerLinkTo}
            className="font-semibold text-indigo-600 transition duration-150 hover:text-indigo-500 hover:underline dark:text-indigo-400 dark:hover:text-indigo-300"
          >
            {footerLinkText}
          </Link>
        </div>
      </div>
    </div>
  )
}
