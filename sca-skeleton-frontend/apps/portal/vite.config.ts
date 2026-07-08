import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import UnoCSS from "unocss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const gatewayTarget = env.VITE_GATEWAY_TARGET || "";

  return {
    server: {
      host: "localhost",
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
      vue(),
      UnoCSS()
    ]
  };
});
