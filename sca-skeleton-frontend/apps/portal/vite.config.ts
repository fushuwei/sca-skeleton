import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";

export default defineConfig({
  server: {
    port: 5174,
    proxy: {
      "/api-dev": {
        target: "http://localhost:9999",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api-dev/, "")
      }
    }
  },
  plugins: [vue(), UnoCSS()]
});
