<script setup lang="ts">
import { ref, computed, reactive } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";
import ProfileSidebar from "../../components/ProfileSidebar.vue";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type AppStatus = "active" | "revoked" | "expired";

interface AppRow {
  id: string;
  name: string;
  clientId: string;
  clientSecret: string;
  scopes: string[];
  status: AppStatus;
  createTime: string;
}

const apps = ref<AppRow[]>([
  {
    id: "APP001",
    name: "教务管理系统",
    clientId: "APP001_d8f3a2b9c7e1",
    clientSecret: "sk_9f2a7c4e1b8d6a3f5c0e7b2d9a4f1c6e",
    scopes: ["profile", "offline_access"],
    status: "active",
    createTime: "2025-09-10"
  },
  {
    id: "APP002",
    name: "科研管理平台",
    clientId: "APP002_a1b3c5d7e9f0",
    clientSecret: "sk_2b4d6f8a0c2e4b6d8f0a2c4e6b8d0f2a",
    scopes: ["all"],
    status: "active",
    createTime: "2025-11-22"
  },
  {
    id: "APP003",
    name: "数据分析工具",
    clientId: "APP003_f0e2d4c6b8a0",
    clientSecret: "sk_1a3c5e7b9d1f3a5c7e9b1d3f5a7c9e1b",
    scopes: ["profile"],
    status: "revoked",
    createTime: "2025-06-15"
  },
  {
    id: "APP004",
    name: "移动校园 APP",
    clientId: "APP004_b9d7f5a3c1e0",
    clientSecret: "sk_7c9e1b3d5f7a9c1e3b5d7f9a1c3e5b7d",
    scopes: ["profile"],
    status: "expired",
    createTime: "2024-12-03"
  }
]);

const revealedSecrets = ref<Set<string>>(new Set());

function toggleSecret(id: string): void {
  if (revealedSecrets.value.has(id)) {
    revealedSecrets.value.delete(id);
  } else {
    revealedSecrets.value.add(id);
  }
}

const statusColorMap: Record<string, string> = {
  active: "teal",
  revoked: "red",
  expired: "orange"
};

function statusLabel(status: AppStatus): string {
  const map: Record<AppStatus, string> = {
    active: t("appIntegration.statusActive"),
    revoked: t("appIntegration.statusRevoked"),
    expired: t("appIntegration.statusExpired")
  };
  return map[status];
}

const scopeOptions = [
  { label: "profile", value: "profile" },
  { label: "offline_access", value: "offline_access" },
  { label: "all", value: "all" },
  { label: "data:read", value: "data:read" },
  { label: "data:write", value: "data:write" }
];

function scopeColor(scope: string): string {
  if (scope === "all") return "red";
  if (scope === "data:write") return "orange";
  if (scope === "data:read") return "blue";
  return "teal";
}

const columns = computed(() => [
  { name: "name", label: t("appIntegration.colAppName"), field: "name", align: "left" as const, sortable: true },
  { name: "clientId", label: t("appIntegration.colClientId"), field: "clientId", align: "left" as const },
  { name: "clientSecret", label: t("appIntegration.colClientSecret"), field: "clientSecret", align: "left" as const },
  { name: "scopes", label: t("appIntegration.colScopes"), field: "scopes", align: "left" as const },
  { name: "status", label: t("appIntegration.colStatus"), field: "status", align: "left" as const, sortable: true },
  { name: "createTime", label: t("appIntegration.colCreateTime"), field: "createTime", align: "left" as const, sortable: true },
  { name: "actions", label: t("appIntegration.colActions"), field: "actions", align: "right" as const }
]);

const pagination = ref({
  page: 1,
  rowsPerPage: 10
});

function copyToClipboard(text: string): void {
  navigator.clipboard.writeText(text).then(() => {
    $q.notify({ type: "positive", message: t("common.copied"), position: "top" });
  }).catch(() => {
    $q.notify({ type: "positive", message: t("common.copied"), position: "top" });
  });
}

function revokeApp(row: AppRow): void {
  row.status = "revoked";
  revealedSecrets.value.delete(row.id);
  $q.notify({ type: "warning", message: `${t("appIntegration.revoke")}: ${row.name}`, position: "top" });
}

// 创建应用对话框
const showCreateDialog = ref(false);
const newApp = reactive<{ name: string; scopes: string[] }>({ name: "", scopes: [] });

function openCreateDialog(): void {
  newApp.name = "";
  newApp.scopes = [];
  showCreateDialog.value = true;
}

function createApp(): void {
  if (!newApp.name.trim()) {
    $q.notify({ type: "negative", message: t("appIntegration.appNamePlaceholder"), position: "top" });
    return;
  }
  const seq = String(apps.value.length + 1).padStart(3, "0");
  const app: AppRow = {
    id: `APP${seq}`,
    name: newApp.name.trim(),
    clientId: `APP${seq}_${Math.random().toString(16).slice(2, 14)}`,
    clientSecret: `sk_${Math.random().toString(16).slice(2, 18)}`,
    scopes: newApp.scopes.length ? [...newApp.scopes] : ["profile"],
    status: "active",
    createTime: "2026-07-22"
  };
  apps.value.unshift(app);
  showCreateDialog.value = false;
  $q.notify({ type: "positive", message: t("appIntegration.createSuccess"), position: "top" });
}
</script>

<template>
  <div class="app-integration-page">
    <div class="profile-layout">
      <ProfileSidebar />
      <div class="profile-main">
    <header class="page-header">
      <div class="header-row">
        <div>
          <h1 class="page-title">{{ t("appIntegration.pageTitle") }}</h1>
          <p class="page-desc">{{ t("appIntegration.pageDesc") }}</p>
        </div>
        <q-btn
          unelevated
          no-caps
          :label="t('appIntegration.createApp')"
          icon="sym_r_add"
          class="btn-primary"
          @click="openCreateDialog"
        />
      </div>
    </header>

    <q-card flat class="page-card">
      <q-table
        :rows="apps"
        :columns="columns"
        row-key="id"
        flat
        dense
        :rows-per-page-options="[10, 20, 50]"
        v-model:pagination="pagination"
        class="data-table"
      >
        <template #body-cell-clientId="props">
          <q-td :props="props">
            <span class="mono-text">{{ props.row.clientId }}</span>
          </q-td>
        </template>

        <template #body-cell-clientSecret="props">
          <q-td :props="props">
            <div class="secret-cell">
              <span class="mono-text">
                {{ revealedSecrets.has(props.row.id) ? props.row.clientSecret : t("appIntegration.secretMasked") }}
              </span>
              <q-btn
                flat
                round
                dense
                size="sm"
                :icon="revealedSecrets.has(props.row.id) ? 'sym_r_visibility_off' : 'sym_r_visibility'"
                :color="revealedSecrets.has(props.row.id) ? 'teal' : 'grey-6'"
                @click="toggleSecret(props.row.id)"
              >
                <q-tooltip>{{ t("appIntegration.revealSecret") }}</q-tooltip>
              </q-btn>
            </div>
          </q-td>
        </template>

        <template #body-cell-scopes="props">
          <q-td :props="props">
            <div class="scope-chips">
              <q-chip
                v-for="scope in props.row.scopes"
                :key="scope"
                dense
                square
                :color="scopeColor(scope)"
                text-color="white"
                class="scope-chip"
              >
                {{ scope }}
              </q-chip>
            </div>
          </q-td>
        </template>

        <template #body-cell-status="props">
          <q-td :props="props">
            <q-chip dense square :color="statusColorMap[props.row.status]" text-color="white">
              {{ statusLabel(props.row.status) }}
            </q-chip>
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="action-cell">
            <q-btn
              flat
              no-caps
              dense
              :label="t('appIntegration.copySecret')"
              color="teal"
              icon="sym_r_content_copy"
              @click="copyToClipboard(props.row.clientId)"
            />
            <q-btn
              v-if="props.row.status === 'active'"
              flat
              no-caps
              dense
              :label="t('appIntegration.revoke')"
              color="red"
              @click="revokeApp(props.row)"
            />
          </q-td>
        </template>
      </q-table>
    </q-card>
      </div>
    </div>

    <!-- 创建应用对话框 -->
    <q-dialog v-model="showCreateDialog">
      <q-card flat class="create-dialog">
        <div class="create-dialog__header">
          <q-icon name="sym_r_add_circle" size="22px" class="create-dialog__icon" />
          <span class="create-dialog__title">{{ t("appIntegration.createDialog") }}</span>
        </div>
        <div class="create-dialog__body">
          <div class="dialog-field">
            <label class="dialog-label">{{ t("appIntegration.appNameLabel") }}</label>
            <q-input
              v-model="newApp.name"
              outlined
              dense
              :placeholder="t('appIntegration.appNamePlaceholder')"
              class="dialog-input"
            />
          </div>
          <div class="dialog-field">
            <label class="dialog-label">{{ t("appIntegration.scopesLabel") }}</label>
            <q-select
              v-model="newApp.scopes"
              :options="scopeOptions"
              emit-value
              map-options
              multiple
              outlined
              dense
              :placeholder="t('appIntegration.scopesPlaceholder')"
              class="dialog-input"
            >
              <template #selected-item="scope">
                <q-chip dense square :color="scopeColor(scope.opt.value)" text-color="white" class="scope-chip">
                  {{ scope.opt.label }}
                </q-chip>
              </template>
            </q-select>
          </div>
        </div>
        <div class="create-dialog__actions">
          <q-btn flat no-caps :label="t('common.cancel')" v-close-popup class="btn-cancel" />
          <q-btn unelevated no-caps :label="t('appIntegration.confirmCreate')" class="btn-primary" @click="createApp" />
        </div>
      </q-card>
    </q-dialog>
  </div>
</template>

<style scoped>
.app-integration-page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

.profile-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  align-items: start;
}

.profile-main {
  min-width: 0;
}

@media (max-width: 1024px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

.page-header {
  margin-bottom: 24px;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.body--dark .page-title {
  color: rgba(255, 255, 255, 0.92);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin: 0;
}

.body--dark .page-desc {
  color: rgba(255, 255, 255, 0.55);
}

.btn-primary {
  background: #009688 !important;
  color: #fff !important;
  padding: 0 20px;
  height: 38px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.btn-primary:hover {
  background: #00796b !important;
}

.body--dark .btn-primary {
  background: #4db6ac !important;
  color: #002b27 !important;
}

.body--dark .btn-primary:hover {
  background: #009688 !important;
  color: #fff !important;
}

.page-card {
  background: #fff !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .page-card {
  background: #2a2a2a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.data-table :deep(.q-table thead th) {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.55);
  background: rgba(0, 0, 0, 0.02);
  padding: 10px 16px;
}

.body--dark .data-table :deep(.q-table thead th) {
  color: rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.03);
}

.data-table :deep(.q-table tbody td) {
  font-size: 13px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.75);
}

.body--dark .data-table :deep(.q-table tbody td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.75);
}

.data-table :deep(.q-table tbody tr:hover td) {
  background: rgba(0, 150, 136, 0.04);
}

.body--dark .data-table :deep(.q-table tbody tr:hover td) {
  background: rgba(77, 182, 172, 0.06);
}

.data-table :deep(.q-table__bottom) {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .data-table :deep(.q-table__bottom) {
  color: rgba(255, 255, 255, 0.55);
  border-top-color: rgba(255, 255, 255, 0.06);
}

.mono-text {
  font-family: "JetBrains Mono", monospace;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.75);
}

.body--dark .mono-text {
  color: rgba(255, 255, 255, 0.75);
}

.secret-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scope-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.scope-chip {
  margin: 0;
}

.action-cell :deep(.q-btn) {
  font-size: 12px;
  font-weight: 500;
  padding: 0 8px;
}

/* ═══════════════ 创建应用对话框 ═══════════════ */
.create-dialog {
  width: 480px;
  max-width: 90vw;
  background: #fff !important;
}

.body--dark .create-dialog {
  background: #2a2a2a !important;
}

.create-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .create-dialog__header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.create-dialog__icon {
  color: #009688;
}

.body--dark .create-dialog__icon {
  color: #4db6ac;
}

.create-dialog__title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .create-dialog__title {
  color: rgba(255, 255, 255, 0.92);
}

.create-dialog__body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dialog-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dialog-label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}

.body--dark .dialog-label {
  color: rgba(255, 255, 255, 0.65);
}

.dialog-input :deep(.q-field__control) {
  background: rgba(0, 0, 0, 0.02);
}

.body--dark .dialog-input :deep(.q-field__control) {
  background: rgba(255, 255, 255, 0.04);
}

.create-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .create-dialog__actions {
  border-top-color: rgba(255, 255, 255, 0.08);
}

.btn-cancel {
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
  height: 38px;
}

.body--dark .btn-cancel {
  color: rgba(255, 255, 255, 0.65);
}

@media (max-width: 768px) {
  .app-integration-page {
    padding: 16px;
  }
  .header-row {
    flex-direction: column;
  }
}
</style>
