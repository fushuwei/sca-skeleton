import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import { router } from "./router";
import { registerPortalTokenSync } from "./apis/http";
import { usePortalAuthStore } from "./stores/auth";
import "ant-design-vue/dist/reset.css";
import "uno.css";

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
registerPortalTokenSync((accessToken, refreshToken) => {
  usePortalAuthStore().syncOAuthTokens(accessToken, refreshToken);
});
app.use(router);
app.mount("#app");
