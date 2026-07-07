<script setup lang="ts">
import { computed } from "vue"; // 导入计算属性函数。
import { useAuthStore } from "../../stores/auth"; // 导入鉴权仓库。
import { flattenRoutableMenus } from "../../utils/menu-tree"; // 导入菜单扁平化工具。

const authStore = useAuthStore(); // 获取鉴权仓库实例。

type MenuLeaf = ReturnType<typeof flattenRoutableMenus>[number]; // 与可路由叶子菜单节点一致的类型别名。

/** 菜单尚未加载时的演示快捷入口（与 mock 菜单路径一致） */
const FALLBACK_SHORTCUTS: MenuLeaf[] = [
  { id: "1", name: "数据源列表", path: "/datasource/list", icon: "sym_r_database", component: "PlaceholderView" },
  { id: "2", name: "主题域模型", path: "/asset/model", icon: "sym_r_inventory_2", component: "PlaceholderView" },
  { id: "3", name: "任务编排", path: "/dev/job/flow", icon: "sym_r_account_tree", component: "PlaceholderView" },
  { id: "4", name: "告警中心", path: "/ops/alert", icon: "sym_r_monitor_heart", component: "PlaceholderView" },
  { id: "5", name: "用户管理", path: "/system/user", icon: "sym_r_manage_accounts", component: "UserListView" },
  { id: "6", name: "元数据目录", path: "/datasource/catalog", icon: "sym_r_folder_open", component: "PlaceholderView" }
];

const nickname = computed(() => authStore.profile?.nickname ?? "管理员"); // 展示昵称。
const loginHint = computed(() => authStore.profile?.username ?? "admin"); // 登录名提示。

const shortcutLeaves = computed(() => {
  const leaves = flattenRoutableMenus(authStore.menus);
  return leaves.length ? leaves.slice(0, 8) : FALLBACK_SHORTCUTS;
});

/** 工作台指标占位（骨架演示，非真实业务统计） */
const metricCards = [
  { label: "已接入数据源", value: "12", unit: "个", icon: "sym_r_database", hint: "含测试环境 3 个" },
  { label: "注册数据资产", value: "1.8k", unit: "张表", icon: "sym_r_inventory_2", hint: "近 7 日新增 42" },
  { label: "今日调度实例", value: "326", unit: "次", icon: "sym_r_schedule", hint: "成功率 98.2%" },
  { label: "待处理告警", value: "3", unit: "条", icon: "sym_r_notifications_active", hint: "含 1 条严重" }
] as const;

/** 系统动态占位列表 */
const activityFeed = [
  { id: "1", title: "「数据标准」模块规则模板已更新", time: "今天 09:12", tone: "primary" },
  { id: "2", title: "夜间批任务窗口执行完毕", time: "昨天 23:05", tone: "grey" },
  { id: "3", title: "系统将于本周日凌晨 02:00–04:00 维护", time: "前天 14:30", tone: "warning" }
] as const;
</script>

<template>
  <div class="workbench-page column q-gutter-y-lg">
    <!-- 欢迎区 -->
    <q-card flat bordered class="workbench-hero overflow-hidden">
      <q-card-section class="workbench-hero__inner row items-center no-wrap">
        <q-avatar rounded color="white" text-color="primary" size="56px" class="workbench-hero__avatar">
          <span class="text-h6 text-weight-medium">{{ nickname.slice(0, 1) || "U" }}</span>
        </q-avatar>
        <div class="column q-ml-md min-w-0">
          <div class="text-h6 text-white text-weight-medium ellipsis">
            {{ nickname }}，欢迎使用工作台
          </div>
          <div class="text-body2 workbench-hero__sub q-mt-xs ellipsis">
            数据中台管理系统 · 登录账号 {{ loginHint }}
          </div>
        </div>
        <q-space />
        <div class="gt-xs text-body2 text-white text-weight-medium workbench-hero__date">
          {{ new Date().toLocaleDateString("zh-CN", { weekday: "long", year: "numeric", month: "long", day: "numeric" }) }}
        </div>
      </q-card-section>
    </q-card>

    <!-- 指标卡 -->
    <div class="row q-col-gutter-md">
      <div v-for="m in metricCards" :key="m.label" class="col-12 col-sm-6 col-lg-3">
        <q-card flat bordered class="workbench-metric full-height">
          <q-card-section class="row items-start no-wrap">
            <q-avatar rounded color="primary" text-color="white" icon-size="28px" size="48px">
              <q-icon :name="m.icon" size="28px" />
            </q-avatar>
            <div class="column q-ml-md min-w-0">
              <div class="text-caption text-grey-7">{{ m.label }}</div>
              <div class="row items-baseline q-mt-xs">
                <span class="text-h5 text-weight-medium text-primary">{{ m.value }}</span>
                <span class="text-body2 text-grey-7 q-ml-xs">{{ m.unit }}</span>
              </div>
              <div class="text-caption text-grey-6 q-mt-xs">{{ m.hint }}</div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <div class="row q-col-gutter-md">
      <!-- 常用入口 -->
      <div class="col-12 col-lg-7">
        <q-card flat bordered class="full-height">
          <q-card-section class="row items-center q-pb-none">
            <q-icon name="sym_r_rocket_launch" size="22px" color="primary" class="q-mr-sm" />
            <span class="text-subtitle1 text-weight-medium">常用入口</span>
          </q-card-section>
          <q-card-section>
            <div class="row q-col-gutter-sm">
              <div v-for="leaf in shortcutLeaves" :key="leaf.name" class="col-12 col-sm-6">
                <q-btn
                  flat
                  no-caps
                  align="left"
                  class="workbench-shortcut-btn full-width"
                  padding="sm md"
                  :to="{ name: leaf.name }"
                >
                  <q-icon :name="leaf.icon ?? 'sym_r_nest_eco_leaf'" size="22px" class="q-mr-md text-primary" />
                  <div class="column items-start min-w-0">
                    <span class="text-body2 text-weight-medium ellipsis full-width">{{ leaf.name }}</span>
                    <span class="text-caption text-grey-6 ellipsis full-width">{{ leaf.path }}</span>
                  </div>
                  <q-icon name="sym_r_chevron_right" size="20px" class="q-ml-auto text-grey-5" />
                </q-btn>
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- 系统动态 -->
      <div class="col-12 col-lg-5">
        <q-card flat bordered class="full-height">
          <q-card-section class="row items-center q-pb-none">
            <q-icon name="sym_r_campaign" size="22px" color="primary" class="q-mr-sm" />
            <span class="text-subtitle1 text-weight-medium">系统动态</span>
          </q-card-section>
          <q-card-section class="q-pt-sm">
            <q-list separator padding class="workbench-feed rounded-borders">
              <q-item v-for="row in activityFeed" :key="row.id" dense class="q-px-none">
                <q-item-section avatar class="workbench-feed__dot">
                  <q-badge :color="row.tone" rounded class="workbench-feed__badge" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-body2">{{ row.title }}</q-item-label>
                  <q-item-label caption>{{ row.time }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
            <div class="text-caption text-grey-6 q-mt-md">
              以上为演示占位内容，接入通知中心后可替换为实时数据。
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 快捷说明 -->
    <q-banner rounded class="workbench-tip bg-grey-2 text-grey-9">
      <template #avatar>
        <q-icon name="sym_r_lightbulb" color="primary" size="28px" />
      </template>
      <div class="text-body2">
        工作台为默认首页，可通过左侧<strong class="text-weight-medium">系统导航</strong>打开各业务模块；打开的页面会以标签页形式保留在主区域顶部，便于来回切换。
      </div>
    </q-banner>
  </div>
</template>

<style scoped>
.workbench-page {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.workbench-hero {
  border: none;
  background: linear-gradient(120deg, #00796b 0%, #009688 55%, #26a69a 100%);
}

.workbench-hero__inner {
  padding: 20px 24px;
}

.workbench-hero__sub {
  opacity: 0.92;
}

.workbench-hero__date {
  opacity: 0.95;
}

.workbench-hero__avatar {
  border: 2px solid rgba(255, 255, 255, 0.35);
}

.workbench-metric {
  transition: box-shadow 0.2s ease;
}

.workbench-metric:hover {
  box-shadow: 0 2px 8px rgba(0, 121, 107, 0.12);
}

.workbench-shortcut-btn {
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.workbench-shortcut-btn:hover {
  background: #e5f2f0;
}

.workbench-feed {
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.workbench-feed__dot {
  min-width: 14px;
}

.workbench-feed__badge {
  width: 8px;
  height: 8px;
  padding: 0;
}

.workbench-tip {
  border: 1px solid rgba(0, 0, 0, 0.06);
}
</style>
