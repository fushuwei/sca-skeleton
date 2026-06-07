import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { quasar, transformAssetUrls } from "@quasar/vite-plugin";
import { createDevLandingPlugin } from "@repo/ui/vite-plugin";

export default defineConfig({
  base: "/admin/",
  resolve: {
    alias: {
      src: fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  server: {
    host: "localhost",
    port: 5173,
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
    vue({
      template: { transformAssetUrls }
    }),
    quasar({
      sassVariables: "./src/styles/quasar.variables.scss"
    }),
    createDevLandingPlugin({
      port: 5173,
      appName: "SCA Admin",
      targetUrl: "http://localhost:8080/admin/",
      gradientFrom: "#3b82f6",
      gradientTo: "#8b5cf6",
      iconEmoji: "\u{2699}\u{FE0F}"
    })
  ]
});
