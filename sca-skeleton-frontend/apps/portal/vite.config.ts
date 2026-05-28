import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";

const appDir = fileURLToPath(new URL(".", import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, appDir, "");
  const proxyTarget = env.VITE_DEV_PROXY_TARGET;
  if (!proxyTarget) {
    throw new Error(
      "缺少 VITE_DEV_PROXY_TARGET：请在 apps/portal/.env.development 中配置开发网关地址"
    );
  }

  return {
    server: {
      host: true,
      port: 5174,
      proxy: {
        "/api-dev": {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api-dev/, "")
        }
      }
    },
    plugins: [vue(), UnoCSS()]
  };
});
