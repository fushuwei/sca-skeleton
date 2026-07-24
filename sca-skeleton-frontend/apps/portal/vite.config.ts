import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { quasar, transformAssetUrls } from "@quasar/vite-plugin";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const gatewayTarget = env.VITE_GATEWAY_TARGET || "";

  return {
    base: env.VITE_BASE_URL || "/",
    resolve: {
      alias: {
        src: fileURLToPath(new URL("./src", import.meta.url))
      }
    },
    server: {
      host: true,
      port: 9090,
      proxy: {
        "/api": {
          target: gatewayTarget,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/api/, "")
        },
        "/auth": {
          target: gatewayTarget,
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
      })
    ]
  };
});
