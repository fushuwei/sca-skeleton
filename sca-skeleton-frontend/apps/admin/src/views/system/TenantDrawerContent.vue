<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenant, TenantPackageOption } from "../../types/auth";
import { createTenantApi, updateTenantApi } from "../../apis/tenant";
import { getTenantPackageOptionsApi } from "../../apis/tenant-package";
import DateTimePicker from "../../components/DateTimePicker.vue";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  tenant?: SysTenant;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  name: "",
  code: "",
  packageId: "",
  status: "normal",
  contactName: "",
  contactPhone: "",
  contactEmail: "",
  domainName: "",
  effectiveTime: "" as string,
  expireTime: "" as string,
  remark: "",
  version: null as number | null
});

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("tenantMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("tenantMgmt.codeRequired")],
  packageId: [(v: string) => !!v || t("tenantMgmt.packageRequired")],
  status: [(v: string) => !!v || t("tenantMgmt.statusRequired")],
  contactPhone: [
    (v: string) => {
      if (!v) return true;
      return /^1[3-9]\d{9}$/.test(v) || t("tenantMgmt.phoneInvalid");
    }
  ],
  contactEmail: [
    (v: string) => {
      if (!v) return true;
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || t("tenantMgmt.emailInvalid");
    }
  ],
  domainName: [
    (v: string) => {
      if (!v) return true;
      return /^(https?:\/\/)?([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}(:\d+)?(\/.*)?$/.test(v) || t("tenantMgmt.domainInvalid");
    }
  ]
}));

/** 租户编码输入处理：自动转小写 */
function onCodeInput(val: string | number | null) {
  form.code = String(val || "").toLowerCase().trim();
}

// ── 租户状态选项 ──
// 创建时仅可选 normal / disabled；编辑时可选全部状态
const statusOptions = computed(() => {
  const all = [
    { label: t("tenantMgmt.statusNormal"), value: "normal" },
    { label: t("tenantMgmt.statusDisabled"), value: "disabled" },
    { label: t("tenantMgmt.statusExpired"), value: "expired" },
    { label: t("tenantMgmt.statusCancelled"), value: "cancelled" }
  ];
  if (props.mode === "add") {
    return all.filter((s) => s.value === "normal" || s.value === "disabled");
  }
  return all;
});

// ── 套餐列表（用于下拉选择） ──
const packageOptions = ref<TenantPackageOption[]>([]);
const packageLoading = ref(false);

async function loadPackageOptions() {
  packageLoading.value = true;
  try {
    const result = await getTenantPackageOptionsApi();
    if (result.code === 10_000 && result.data) {
      // 后端已过滤 status=enabled，前端直接信任后端数据
      packageOptions.value = result.data;
    }
  } catch {
    // 静默失败
  } finally {
    packageLoading.value = false;
  }
}

function resetForm() {
  form.id = "";
  form.name = "";
  form.code = "";
  form.packageId = "";
  form.status = "normal";
  form.contactName = "";
  form.contactPhone = "";
  form.contactEmail = "";
  form.domainName = "";
  form.effectiveTime = "";
  form.expireTime = "";
  form.remark = "";
  form.version = null;
}

function initForm() {
  resetForm();
  if (props.tenant) {
    form.id = props.tenant.id;
    form.name = props.tenant.name;
    form.code = props.tenant.code ?? "";
    form.packageId = props.tenant.packageId || "";
    form.status = props.tenant.status || "normal";
    form.contactName = props.tenant.contactName || "";
    form.contactPhone = props.tenant.contactPhone || "";
    form.contactEmail = props.tenant.contactEmail || "";
    form.domainName = props.tenant.domainName || "";
    form.effectiveTime = props.tenant.effectiveTime || "";
    form.expireTime = props.tenant.expireTime || "";
    form.remark = props.tenant.remark || "";
    form.version = props.tenant.version ?? null;
  }
}

watch(() => props.tenant, initForm, { immediate: true });

onMounted(() => {
  loadPackageOptions();
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    name: form.name,
    code: form.code,
    packageId: form.packageId,
    status: form.status,
    contactName: form.contactName || undefined,
    contactPhone: form.contactPhone || undefined,
    contactEmail: form.contactEmail || undefined,
    domainName: form.domainName || undefined,
    effectiveTime: form.effectiveTime || null,
    expireTime: form.expireTime || null,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createTenantApi(data);
    } else {
      data.id = form.id;
      data.version = form.version;
      result = await updateTenantApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("tenantMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("tenantMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("tenantMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="tenant-drawer-content">
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="tenant-drawer-form" class="tenant-drawer-form tenant-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 租户名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('tenantMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 租户编码 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.code"
            @update:model-value="onCodeInput"
            :label="t('tenantMgmt.code')"
            filled
            square
            :rules="formRules.code"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 套餐选择 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.packageId"
            :label="t('tenantMgmt.package')"
            filled
            square
            :options="packageOptions"
            :option-label="(o: TenantPackageOption) => o ? o.name : ''"
            option-value="id"
            emit-value
            map-options
            :rules="formRules.packageId"
            :disable="drawerReadonly"
            :loading="packageLoading"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 租户状态 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.status"
            :label="t('tenantMgmt.status')"
            filled
            square
            :options="statusOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.status"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 联系人姓名 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.contactName"
            :label="t('tenantMgmt.contactName')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 联系人电话 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.contactPhone"
            :label="t('tenantMgmt.contactPhone')"
            filled
            square
            :rules="formRules.contactPhone"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 联系人邮箱 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.contactEmail"
            :label="t('tenantMgmt.contactEmail')"
            filled
            square
            type="email"
            :rules="formRules.contactEmail"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 绑定独立域名 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.domainName"
            :label="t('tenantMgmt.domainName')"
            filled
            square
            :rules="formRules.domainName"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 生效时间 -->
        <div class="col-12 col-md-6">
          <DateTimePicker
            v-model="form.effectiveTime"
            :label="t('tenantMgmt.effectiveTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :max="form.expireTime"
            clearable
          />
          <div v-if="!form.effectiveTime" class="validity-hint q-mt-xs">{{ t('tenantMgmt.effectiveTimeHint') }}</div>
        </div>
        <!-- 过期时间 -->
        <div class="col-12 col-md-6">
          <DateTimePicker
            v-model="form.expireTime"
            :label="t('tenantMgmt.expireTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :min="form.effectiveTime"
            clearable
          />
          <div v-if="!form.expireTime" class="validity-hint q-mt-xs">{{ t('tenantMgmt.expireTimeHint') }}</div>
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('tenantMgmt.remark')"
            filled
            square
            type="textarea"
            rows="3"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
      </div>
    </q-form>

    <!-- 底部操作栏：固定第三段，不随内容滚动；按钮直接挂在页脚容器上，间距由 CSS gap 控制 -->
    <div v-if="!drawerReadonly" class="tenant-drawer-footer row items-center justify-end no-wrap">
      <q-btn
        color="grey-7"
        outline
        no-caps
        class="drawer-action-btn"
        @click="handleClose"
      >
        {{ t('common.cancel') }}
      </q-btn>
      <q-btn
        type="submit"
        form="tenant-drawer-form"
        color="primary"
        unelevated
        no-caps
        :loading="formLoading"
        class="drawer-action-btn"
      >
        {{ t('common.confirm') }}
      </q-btn>
    </div>
  </div>
</template>

<style scoped>
/* 三段式抽屉布局：外层 .tenant-drawer-body 不再滚动，
   中间内容区（q-form.tenant-drawer-main）独立滚动，底部操作栏固定为第三段 */
.tenant-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中间内容区：唯一的滚动区域 */
.tenant-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

/* 底部操作栏：固定第三段，不参与内容滚动（按钮直接子元素，gap 控制间距） */
.tenant-drawer-footer {
  flex-shrink: 0;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.drawer-action-btn {
  min-width: 72px;
}

/* 必填项星号红色高亮 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 有效期提示文案 */
.validity-hint {
  padding-left: 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.5;
}
</style>

<style>
.body--dark .tenant-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .tenant-drawer-form .q-field__native,
.body--dark .tenant-drawer-form .q-field__prefix,
.body--dark .tenant-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .tenant-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .tenant-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .tenant-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .tenant-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .tenant-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 有效期提示文案暗色模式 */
.body--dark .validity-hint {
  color: rgba(255, 255, 255, 0.45);
}
</style>
