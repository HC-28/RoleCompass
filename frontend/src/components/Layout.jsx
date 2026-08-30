import Header from './Header'

export default function Layout({ children }) {
  return (
    <div className="app-background flex min-h-screen flex-col font-sans">
      {/* Clean Background Layer */}
      <div className="bg-layer-artwork" aria-hidden="true" />

      {/* Interactive Content Layer */}
      <div className="relative z-10 flex min-h-screen flex-col">
        <Header />
        <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center px-4 pt-24 pb-16 sm:px-6 lg:px-8">
          {children}
        </main>
      </div>
    </div>
  )
}
