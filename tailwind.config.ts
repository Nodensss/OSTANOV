import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: "#0a0d0f",
        "bg-grid": "#0f1316",
        surface: "#141a1d",
        "surface-2": "#1a2125",
        border: "#2a343a",
        text: "#ece9e0",
        "text-dim": "#98a3a8",
        "text-mute": "#5d686d",
        amber: "#f5a623",
        "amber-soft": "#fbbf24",
        cool: "#60a5fa",
        steel: "#6ba0b8",
        danger: "#ef4444",
        warn: "#f59e0b",
        safe: "#10b981",
      },
      fontFamily: {
        sans: ["var(--font-plex-sans)", "system-ui", "sans-serif"],
        mono: ["var(--font-plex-mono)", "ui-monospace", "monospace"],
        display: ["var(--font-rubik-mono)", "var(--font-plex-mono)", "monospace"],
      },
      boxShadow: {
        panel: "0 20px 60px rgba(0,0,0,0.32)",
      },
    },
  },
  plugins: [],
};
export default config;
