import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { quasar, transformAssetUrls } from "@quasar/vite-plugin";

const appDir = fileURLToPath(new URL(".", import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, appDir, "");
  const proxyTarget = env.VITE_DEV_PROXY_TARGET;
  if (!proxyTarget) {
    throw new Error(
      "缺少 VITE_DEV_PROXY_TARGET：请在 apps/admin/.env.development 中配置开发网关地址"
    );
  }

  return {
    resolve: {
      alias: {
        src: new URL("./src", import.meta.url).pathname
      }
    },
    server: {
      host: true,
      port: 5173,
      proxy: {
        "/api-dev": {
          target: proxyTarget,
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
  };
});
