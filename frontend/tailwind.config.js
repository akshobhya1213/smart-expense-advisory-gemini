/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          950: '#0B0F14',
          900: '#111827',
          800: '#1F2937',
          700: '#374151',
        },
        brand: {
          500: '#4F7CFF',
          600: '#3B63E8',
        },
        surface: '#F7F8FA',
      },
      boxShadow: {
        card: '0 1px 2px rgba(16,24,40,0.04), 0 1px 3px rgba(16,24,40,0.06)',
      },
    },
  },
  plugins: [],
}
