<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenant, SysTenantPackage } from "../../types/auth";
import { createTenantApi, updateTenantApi } from "../../apis/tenant";
import { getTenantPackageListApi } from "../../apis/tenant-package";
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
  accountLimit: null as number | null,
  expireTime: "" as string,
  remark: "",
  version: null as number | null
});

// ── 账号数量限制无限制开关：true 时对应字段为 -1（不限），false 时可输入具体值 ──
const accountLimitUnlimited = ref(false);

function onAccountLimitToggle(val: boolean | null) {
  const checked = !!val;
  accountLimitUnlimited.value = checked;
  form.accountLimit = checked ? -1 : null;
}

// ── 过期时间无限制开关：true 时 expireTime 为空（永不过期），false 时可选择日期 ──
const expireTimeUnlimited = ref(false);

function onExpireTimeToggle(val: boolean | null) {
  const checked = !!val;
  expireTimeUnlimited.value = checked;
  form.expireTime = checked ? "" : "";
}

/** 账号限额校验：勾选无限制时跳过；未勾选时必填且必须为大于 0 的整数 */
function validateAccountLimit(unlimited: boolean, val: number | null): true | string {
  if (unlimited) return true;
  if (val === null) {
    return t("tenantMgmt.limitRequired");
  }
  if (!Number.isInteger(val) || val <= 0) {
    return t("tenantMgmt.limitPositiveInteger");
  }
  return true;
}

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("tenantMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("tenantMgmt.codeRequired")],
  packageId: [(v: string) => !!v || t("tenantMgmt.packageRequired")],
  status: [(v: string) => !!v || t("tenantMgmt.statusRequired")],
  accountLimit: [(v: number | null) => validateAccountLimit(accountLimitUnlimited.value, v)]
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
const packageOptions = ref<SysTenantPackage[]>([]);
const packageLoading = ref(false);

async function loadPackageOptions() {
  packageLoading.value = true;
  try {
    const result = await getTenantPackageListApi();
    if (result.code === 10_000 && result.data) {
      // 仅展示启用状态的套餐
      packageOptions.value = result.data.filter((p) => p.status === "enabled");
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
  form.accountLimit = null;
  form.expireTime = "";
  form.remark = "";
  form.version = null;
  accountLimitUnlimited.value = false;
  expireTimeUnlimited.value = false;
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
    form.accountLimit = props.tenant.accountLimit ?? -1;
    form.expireTime = props.tenant.expireTime || "";
    form.remark = props.tenant.remark || "";
    form.version = props.tenant.version ?? null;

    // 同步无限制开关状态
    accountLimitUnlimited.value = form.accountLimit === -1;
    expireTimeUnlimited.value = !form.expireTime;
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
    accountLimit: form.accountLimit,
    expireTime: expireTimeUnlimited.value ? null : (form.expireTime || null),
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
    <q-form class="tenant-drawer-form" @submit="handleSave">
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
            :option-label="(o: SysTenantPackage) => o ? o.name : ''"
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
      </div>

      <!-- ── 联系信息 ── -->
      <div class="contact-section q-mt-md">
        <div class="contact-section-header row items-center no-wrap q-mb-sm">
          <q-icon name="sym_r_contact_phone" size="20px" class="q-mr-xs" color="grey-8" />
          <span class="contact-section-title">{{ t('tenantMgmt.contactInfo') }}</span>
        </div>
        <div class="row q-col-gutter-md">
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
              :disable="drawerReadonly"
              :readonly="drawerReadonly"
              hide-bottom-space
            />
          </div>
        </div>
      </div>

      <!-- ── 限额配置 ── -->
      <div class="limit-section q-mt-md">
        <div class="limit-section-header row items-center no-wrap q-mb-sm">
          <q-icon name="sym_r_tune" size="20px" class="q-mr-xs" color="grey-8" />
          <span class="limit-section-title">{{ t('tenantMgmt.limitConfig') }}</span>
        </div>
        <div class="row q-col-gutter-md">
          <!-- 账号数量限制 -->
          <div class="col-12 col-md-6">
            <q-input
              v-model.number="form.accountLimit"
              :label="t('tenantMgmt.accountLimit')"
              filled
              square
              type="number"
              :disable="drawerReadonly"
              :readonly="drawerReadonly || accountLimitUnlimited"
              :rules="formRules.accountLimit"
              hide-bottom-space
            >
              <template #append>
                <q-checkbox
                  :model-value="accountLimitUnlimited"
                  @update:model-value="onAccountLimitToggle"
                  :label="t('tenantMgmt.unlimited')"
                  :disable="drawerReadonly"
                />
              </template>
            </q-input>
          </div>
          <!-- 过期时间 -->
          <div class="col-12 col-md-6">
            <DateTimePicker
              v-if="!expireTimeUnlimited"
              v-model="form.expireTime"
              :label="t('tenantMgmt.expireTime')"
              :disable="drawerReadonly"
              :readonly="drawerReadonly"
              clearable
            />
            <q-input
              v-else
              :model-value="t('tenantMgmt.neverExpires')"
              :label="t('tenantMgmt.expireTime')"
              filled
              square
              readonly
              hide-bottom-space
            />
            <q-checkbox
              :model-value="expireTimeUnlimited"
              @update:model-value="onExpireTimeToggle"
              :label="t('tenantMgmt.neverExpires')"
              :disable="drawerReadonly"
              class="q-mt-xs"
            />
          </div>
        </div>
      </div>

      <!-- 备注 -->
      <div class="q-mt-md">
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

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="tenant-drawer-footer row justify-end q-gutter-sm">
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
          color="primary"
          unelevated
          no-caps
          :loading="formLoading"
          class="drawer-action-btn"
        >
          {{ t('common.confirm') }}
        </q-btn>
      </div>
    </q-form>
  </div>
</template>

<style scoped>
.tenant-drawer-content {
  padding: 0;
}

.tenant-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
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

/* 联系信息区域 */
.contact-section {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 4px;
  padding: 12px;
}

.contact-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

/* 限额配置区域 */
.limit-section {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 4px;
  padding: 12px;
}

.limit-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

/* 限额配置中"无限制"复选框尺寸与列表页保持一致 */
.limit-section :deep(.q-checkbox__inner) {
  font-size: 32px;
}

.limit-section :deep(.q-checkbox__label) {
  font-size: 14px;
}

/* 限额配置中勾选"无限制"后文本框保持实线底边框（覆盖 Quasar readonly 默认虚线） */
.limit-section :deep(.q-field--filled.q-field--readonly .q-field__control:before) {
  border-bottom-style: none;
}

/* 限额输入框 append 区域的 checkbox 不挤压 */
.limit-section :deep(.q-field__append) {
  white-space: nowrap;
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

/* 联系信息区域暗色模式 */
.body--dark .contact-section {
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .contact-section-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .contact-section-header .q-icon {
  color: rgba(255, 255, 255, 0.72) !important;
}

/* 限额配置区域暗色模式 */
.body--dark .limit-section {
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .limit-section-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .limit-section-header .q-icon {
  color: rgba(255, 255, 255, 0.72) !important;
}
</style>
