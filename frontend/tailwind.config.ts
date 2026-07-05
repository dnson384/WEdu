import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./presentation/**/*.{js,ts,jsx,tsx,mdx}", // 👈 Phải có dòng này để nhận diện thư mục presentation của bạn
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {},
  plugins: [],
};
export default config;
