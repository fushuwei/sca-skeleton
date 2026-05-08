import { defineConfig } from "vite"; // 导入 Vite 配置函数。
import vue from "@vitejs/plugin-vue"; // 导入 Vue 单文件组件支持插件。
import { quasar, transformAssetUrls } from "@quasar/vite-plugin"; // 导入 Quasar 的 Vite 插件与资源转换配置。

export default defineConfig({
  // 导出 admin 应用的 Vite 配置。
  resolve: {
    // 配置模块路径解析规则。
    alias: {
      // 配置路径别名集合。
      src: new URL("./src", import.meta.url).pathname
      // 将 src 别名映射到当前应用的源代码目录。
    }
    // 结束路径别名配置。
  },
  // 结束路径解析配置。
  plugins: [
    // 声明本项目使用的插件列表。
    vue({
      // 配置 Vue 插件并接入 Quasar 资源地址转换。
      template: { transformAssetUrls }
      // 将 Quasar 组件中的资源路径转换为 Vite 可识别格式。
    }),
    quasar({
      // 配置 Quasar 插件参数。
      sassVariables: "./src/styles/quasar.variables.scss"
      // 指定 Quasar SASS 变量覆盖文件。
    })
    // 注入 Quasar 插件以启用组件样式与按需能力。
  ]
  // 结束插件配置。
});
