# 问题清单

## 问题描述

1. /auth/logout 接口地址要求经过认证，未登录不允许访问

2. /auth/logout 接口要求在 HttpOnly Cookie 中携带刷新 Token，经过测试，发现当点击退出按钮时，请求头中并没有刷新 token，这是为什么？同时观察 Redis 服务，在 0 号库中并没有找到刷新 token 的记录，难道不用存储吗？如果不销毁刷新 token，那有心之人拿到该 token 后调用后端端点，获取新的访问 token 又当如何？

3. 访问 portal 前台登录，故意输错验证码，点击提交，浏览器地址会刷新，变成：http://localhost:9999/auth/login/portal?error&captcha-error ，但是此时没有任何提示效果，这个非常不合理，应该提示验证码错误，提示效果如果能和表单校验效果一样最好，实在做不了就只能弹框或 tips 提示

4. admin 后台点击退出按钮, 浏览器地址跳转到了 http://localhost:9999/auth/login/admin ，此时输入正确的用户名和密码，点击登录，浏览器会刷新，地址变成：http://localhost:9999/auth/login/admin?error&reason=oauth_session ，此时浏览器页面依旧是登录页，没有任何提示，无论怎么登陆都无法进入 admin 登录后的主界面

5. 在 admin 登录页面，输入正确的用户名和密码，点击登录，页面不要出现空白页然后中间显示几个正在跳转的的文案，请一律使用全局 loading 效果，我已经在 template 目录下提供了 loading.html 效果，如果你还没有将其封装到整个前端公共组件，请你先封装一下，然后应用到该需求中

6. 关于登录过程存在的一个体验问题（以后台 admin 页面为例）：访问 localhost:5173，浏览器跳转到 auth 的登录页，当在这个登录页停留一段时间，然后输入正确的用户名和密码，点击登录后，页面没有任何提示，浏览器会刷新，但是刷新后依旧停留在登录页，此时浏览器地址：

7. 在以上问题的基础上，有时候登录退出多了，在同一个浏览器中再次访问 localhost:5173，浏览器地址会跳转到：http://localhost:5173/oauth/callback?code=__PTsrKoN-5AdikFOrxGCv27LLJtr2loKgmaaIRJskO_OibCNgvg2UKyIMg0RBzM-RwTgawUdODOBPh6vl2VtYH0kYxn3a0i3iMyYKz-H2igJkLM3LHXlQQZOGfqwX4v&state=yBIeUJB3mQeRUMHqB816F-2mdIv~dWkm ，此时页面是白色背景，中间提示一行字：授权回调参数无效，请重新登录


## 处理要求

以上问题，要求按照企业级生产项目的最佳实践进行整改，要求从根本上解决问题，同时要求解释每一个问题为什么会出现的原因，并说明修复状态（例如已修复、部分修复、未修复等）
