import { Link } from 'react-router-dom'

export default function AuthCard({
  title,
  subtitle,
  children,
  footerText,
  footerLinkText,
  footerLinkTo,
}) {
  return (
    <div className="mx-auto w-full max-w-md">
      <div className="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/70 shadow-2xl shadow-black/40 backdrop-blur-xl">
        <div className="border-b border-white/10 bg-gradient-to-r from-indigo-500/10 to-violet-500/10 px-8 py-8">
          <h1 className="text-2xl font-semibold tracking-tight text-white">{title}</h1>
          <p className="mt-2 text-sm text-slate-400">{subtitle}</p>
        </div>

        <div className="px-8 py-8">{children}</div>

        <div className="border-t border-white/10 px-8 py-5 text-center text-sm text-slate-400">
          {footerText}{' '}
          <Link
            to={footerLinkTo}
            className="font-medium text-indigo-400 transition hover:text-indigo-300"
          >
            {footerLinkText}
          </Link>
        </div>
      </div>
    </div>
  )
}
