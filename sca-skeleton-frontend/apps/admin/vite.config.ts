import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { quasar, transformAssetUrls } from "@quasar/vite-plugin";

export default defineConfig({
  resolve: {
    alias: {
      src: new URL("./src", import.meta.url).pathname
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api-dev": {
        target: "http://localhost:9999",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api-dev/, "")
      }
    }
  },
  plugins: [
    vue({
      template: { transformAssetUrls }
    }),
    quasar({
      sassVariables: "./src/styles/quasar.variables.scss"
    })
  ]
});
