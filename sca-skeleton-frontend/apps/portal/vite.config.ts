import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";
import { createDevLandingPlugin } from "@repo/ui/vite-plugin";

export default defineConfig({
  server: {
    host: "localhost",
    port: 5174,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/auth": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  },
  plugins: [
    vue(),
    UnoCSS(),
    createDevLandingPlugin({
      port: 5174,
      appName: "SCA Portal",
      targetUrl: "http://localhost:8080/",
      gradientFrom: "#10b981",
      gradientTo: "#06b6d4",
      iconEmoji: "\u{1F310}"
    })
  ]
});
