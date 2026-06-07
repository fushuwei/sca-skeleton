import type { Plugin } from "vite";

export interface DevLandingOptions {
  port: number;
  appName: string;
  targetUrl: string;
  gradientFrom: string;
  gradientTo: string;
  iconEmoji: string;
}

function landingHtml(opts: DevLandingOptions): string {
  const { appName, targetUrl, gradientFrom, gradientTo, iconEmoji } = opts;
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${appName} - 开发环境入口</title>
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
body{
  font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,"PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif;
  background:linear-gradient(135deg,#0f172a 0%,#1e293b 50%,#0f172a 100%);
  color:#e2e8f0;min-height:100vh;display:flex;align-items:center;justify-content:center;overflow:hidden
}
.bg-grid{
  position:fixed;inset:0;
  background-image:linear-gradient(rgba(148,163,184,.05) 1px,transparent 1px),linear-gradient(90deg,rgba(148,163,184,.05) 1px,transparent 1px);
  background-size:60px 60px;
  mask-image:radial-gradient(ellipse 80% 80% at 50% 50%,black 40%,transparent 70%)
}
.container{position:relative;z-index:1;text-align:center;padding:2rem;max-width:600px;width:100%}
.icon{
  width:80px;height:80px;margin:0 auto 1.5rem;
  background:linear-gradient(135deg,${gradientFrom},${gradientTo});
  border-radius:20px;display:flex;align-items:center;justify-content:center;
  font-size:40px;box-shadow:0 8px 32px rgba(from ${gradientFrom} r g b/.3)
}
.icon span{font-size:40px;line-height:1}
h1{
  font-size:2rem;font-weight:700;line-height:1.3;margin-bottom:.75rem;
  background:linear-gradient(135deg,${gradientFrom},${gradientTo});
  -webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text
}
p{font-size:1rem;color:#94a3b8;line-height:1.6;margin-bottom:2rem;max-width:420px;margin-left:auto;margin-right:auto}
.card{
  background:rgba(30,41,59,.8);border:1px solid rgba(148,163,184,.1);
  border-radius:16px;padding:1.5rem;margin-bottom:1.5rem;backdrop-filter:blur(12px)
}
.card-title{font-size:.85rem;font-weight:600;color:#94a3b8;text-transform:uppercase;letter-spacing:.05em;margin-bottom:.75rem}
.card-body{display:flex;align-items:center;justify-content:center;gap:.5rem}
.code{
  background:rgba(15,23,42,.6);color:${gradientFrom};padding:.5rem 1rem;border-radius:8px;
  font-family:"JetBrains Mono","Fira Code",monospace;font-size:.95rem;font-weight:600;
  letter-spacing:.02em;border:1px solid rgba(from ${gradientFrom} r g b/.2);user-select:all
}
.btn{
  display:inline-flex;align-items:center;gap:.5rem;padding:.75rem 2rem;
  background:linear-gradient(135deg,${gradientFrom},${gradientTo});
  color:#fff;font-size:1rem;font-weight:600;border:none;border-radius:12px;
  cursor:pointer;text-decoration:none;transition:all .2s ease;
  box-shadow:0 4px 16px rgba(from ${gradientFrom} r g b/.3)
}
.btn:hover{transform:translateY(-1px);box-shadow:0 6px 24px rgba(from ${gradientFrom} r g b/.45)}
.btn:active{transform:translateY(0)}
.btn svg{width:18px;height:18px}
.divider{display:flex;align-items:center;gap:1rem;margin:1.5rem 0}
.divider::before,.divider::after{content:'';flex:1;height:1px;background:rgba(148,163,184,.15)}
.divider span{font-size:.8rem;color:#64748b;text-transform:uppercase;letter-spacing:.05em}
.footer-text{font-size:.8rem;color:#475569;margin-top:2rem}
</style>
</head>
<body>
<div class="bg-grid"></div>
<div class="container">
<div class="icon"><span>${iconEmoji}</span></div>
<h1>${appName} 开发服务器</h1>
<p>这是 Vite 开发服务器，仅用于前端热更新开发。请通过统一入口访问完整应用。</p>
<div class="card">
<div class="card-title">正确访问地址</div>
<div class="card-body">
<a class="btn" href="${targetUrl}">
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
前往 ${targetUrl.replace("http://", "")}
</a>
</div>
</div>
<div class="divider"><span>或复制以下地址</span></div>
<div class="card"><span class="code">${targetUrl}</span></div>
<p class="footer-text">Vite HMR 可通过代理模式与后端联调，详见项目文档</p>
</div>
</body>
</html>`;
}

/**
 * Vite 插件：当用户直接访问 Vite 开发服务器时，
 * 展示引导页面，指引用户前往 nginx 统一入口（localhost:8080）。
 */
export function createDevLandingPlugin(options: DevLandingOptions): Plugin {
  const html = landingHtml(options);
  const expectedHost = `localhost:${options.port}`;

  return {
    name: "vite-plugin-dev-landing",
    apply: "serve",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url ?? "/";
        const isRoot = url === "/";
        if (!isRoot) return next();

        // 仅拦截浏览器直连 Vite 端口（Host 含端口号），
        // 经过 nginx 代理的请求（Host 不含端口）正常放行
        const host = req.headers.host ?? "";
        if (host !== expectedHost) return next();

        const accept = req.headers.accept ?? "";
        if (!accept.includes("text/html")) return next();

        res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
        res.end(html);
      });
    }
  };
}
