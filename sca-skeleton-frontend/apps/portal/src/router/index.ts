import { createRouter, createWebHistory } from "vue-router";
import { staticRoutes } from "./routes";
import { setupRouterGuards } from "./guards";

export const router = createRouter({
  history: createWebHistory("/"),
  routes: staticRoutes
});

setupRouterGuards(router);
