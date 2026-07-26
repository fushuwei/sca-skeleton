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
      port: 8080,
      proxy: {
        "/api": {
          target: gatewayTarget,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/api/, ""),
          configure: (proxy) => {
            // 转发浏览器真实 IP 到网关，确保登录日志/操作日志记录真实客户端 IP 而非代理 IP
            proxy.on("proxyReq", (proxyReq, req) => {
              const clientIp = req.socket.remoteAddress;
              if (clientIp) {
                // 处理 IPv6 映射的 IPv4 地址（::ffff:192.168.1.104 -> 192.168.1.104）
                const normalizedIp = clientIp.startsWith("::ffff:")
                  ? clientIp.substring(7)
                  : clientIp;
                proxyReq.setHeader("X-Forwarded-For", normalizedIp);
                proxyReq.setHeader("X-Real-IP", normalizedIp);
              }
            });
          }
        },
        "/auth": {
          target: gatewayTarget,
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on("proxyReq", (proxyReq, req) => {
              const clientIp = req.socket.remoteAddress;
              if (clientIp) {
                const normalizedIp = clientIp.startsWith("::ffff:")
                  ? clientIp.substring(7)
                  : clientIp;
                proxyReq.setHeader("X-Forwarded-For", normalizedIp);
                proxyReq.setHeader("X-Real-IP", normalizedIp);
              }
            });
          }
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
