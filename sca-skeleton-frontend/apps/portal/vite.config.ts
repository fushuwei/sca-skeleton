import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { quasar, transformAssetUrls } from "@quasar/vite-plugin";
import { overrideFontDisplay } from "@repo/ui/vite-plugins/override-font-display";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const gatewayTarget = env.VITE_GATEWAY_TARGET || "";

  // 代理配置：转发浏览器真实 IP 到网关，确保登录日志/操作日志记录真实客户端 IP 而非代理 IP
  const configureProxy = (proxy: any) => {
    proxy.on("proxyReq", (proxyReq: any, req: any) => {
      const clientIp = req.socket.remoteAddress;
      if (clientIp) {
        // 处理 IPv6 映射的 IPv4 地址（::ffff:192.168.1.104 -> 192.168.1.104）
        // 处理 IPv6 本地回环地址（::1 -> 127.0.0.1）
        const normalizedIp = clientIp.startsWith("::ffff:")
          ? clientIp.substring(7)
          : clientIp === "::1"
            ? "127.0.0.1"
            : clientIp;
        proxyReq.setHeader("X-Forwarded-For", normalizedIp);
        proxyReq.setHeader("X-Real-IP", normalizedIp);
      }
    });
  };

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
          rewrite: (path: string) => path.replace(/^\/api/, ""),
          configure: configureProxy
        }
      }
    },
    plugins: [
      // 必须放在 vue/quasar 之前：在 Vite 处理 CSS 前替换 @quasar/extras 的 font-display: block → swap
      // 避免 Material Icons/Symbols 字体（5.2MB）在 block 模式下导致图标 3 秒不可见
      overrideFontDisplay(),
      vue({
        template: { transformAssetUrls }
      }),
      quasar({
        sassVariables: "./src/styles/quasar.variables.scss"
      })
    ]
  };
});
