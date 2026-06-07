import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";

export default defineConfig({
  server: {
    host: "localhost",
    port: 5174
  },
  plugins: [vue(), UnoCSS()]
});
