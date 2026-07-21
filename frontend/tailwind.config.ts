import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{js,ts,jsx,tsx,mdx}", "./lib/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#132229",
        paper: "#f4f1e9",
        signal: "#d85b36",
        teal: "#0f6766",
        mist: "#dce9e5"
      },
      boxShadow: {
        panel: "0 18px 50px rgba(19, 34, 41, 0.10)"
      }
    }
  },
  plugins: []
};

export default config;
