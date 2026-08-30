/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        'glow-indigo': '0 0 20px -3px rgba(99, 102, 241, 0.3)',
        'glow-emerald': '0 0 20px -3px rgba(16, 185, 129, 0.3)',
        'card-light': '0 1px 3px 0 rgba(0, 0, 0, 0.05), 0 10px 25px -5px rgba(0, 0, 0, 0.05)',
        'card-dark': '0 0 0 1px rgba(255, 255, 255, 0.08), 0 12px 32px -4px rgba(0, 0, 0, 0.6)',
      },
    },
  },
  plugins: [],
}
