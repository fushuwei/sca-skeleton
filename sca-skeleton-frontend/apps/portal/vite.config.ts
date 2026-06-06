import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";

export default defineConfig({
  server: {
    host: true,
    port: 5174
  },
  plugins: [vue(), UnoCSS()]
});
