# Auth 服务的登录页改造

## 前言

以下需求实施过程要求严格遵守 /Users/fuwei/Documents/Workspace/work/sca-skeleton/.cursor/rules/karpathy-guidelines.mdc
中的规则约束。

## 需求描述

1. 我在 template 目录下使用 html、css 和 js 写了一个登录页，请在 auth 服务中一比一复制并使用这个登录页，如果必须使用
   thymeleaf 模版引擎，可以直接将我写的 html 复制到 auth 服务的 templates 中，使用 thymeleaf 模版套起来即可。

2. admin 后台登录页要求 100% 与模版中的 login.html 保持一致。除了适配 thymeleaf，其他不做任何改动

3. portal 前台登录页要求 100% 与模版中的 login.html 保持一致。
   另外，在密码输入框下面增加一个图片验证码，布局要求左边是验证码输入框，右边是验证码图片，样式和尺寸与用户名、密码输入框保持风格完全一致，不要出现任何多余的效果，验证码图片通过调用后端接口生成

4. 前后台登录页面加载完成之前，显示 loading 效果，同理，我已经在 template 目录下写了一个全局默认的 loading 效果，参考
   template/loading.html，请将其应用到当前项目中，建议在 monorepo 中封装成公共功能或组件
