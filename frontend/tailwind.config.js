/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      colors: {
        background: 'hsl(var(--colors-background) / <alpha-value>)',
        foreground: 'hsl(var(--colors-foreground) / <alpha-value>)',
        card: 'hsl(var(--colors-card) / <alpha-value>)',
        popover: 'hsl(var(--colors-popover) / <alpha-value>)',
        primary: {
          DEFAULT: 'hsl(var(--colors-primary) / <alpha-value>)',
          foreground: 'hsl(var(--colors-primary-foreground) / <alpha-value>)',
        },
        secondary: {
          DEFAULT: 'hsl(var(--colors-secondary) / <alpha-value>)',
          foreground: 'hsl(var(--colors-secondary-foreground) / <alpha-value>)',
        },
        muted: {
          DEFAULT: 'hsl(var(--colors-muted) / <alpha-value>)',
          foreground: 'hsl(var(--colors-muted-foreground) / <alpha-value>)',
        },
        accent: 'hsl(var(--colors-accent) / <alpha-value>)',
        border: 'hsl(var(--colors-border) / <alpha-value>)',
        input: 'hsl(var(--colors-input) / <alpha-value>)',
        ring: 'hsl(var(--colors-ring) / <alpha-value>)',
        success: 'hsl(var(--colors-success) / <alpha-value>)',
        warning: 'hsl(var(--colors-warning) / <alpha-value>)',
        info: 'hsl(var(--colors-info) / <alpha-value>)',
        destructive: 'hsl(var(--colors-destructive) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['Quicksand', 'Inter', 'Roboto', 'system-ui', 'sans-serif'],
        hanzi: ['var(--font-hanzi)', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', 'sans-serif'],
      },
      borderRadius: {
        sm: '8px',
        md: '10px',
        lg: '14px',
        xl: '16px',
        pill: '9999px',
      },
    },
  },
  plugins: [],
};
