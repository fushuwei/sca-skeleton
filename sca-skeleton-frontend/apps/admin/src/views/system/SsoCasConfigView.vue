<script setup lang="ts">
/**
 * 统一身份认证 - CAS 配置页
 *
 * 交互形态：默认「只读表单」，右上角「编辑」进入可编辑态，保存后回到只读态。
 * 顶部「启用」开关独立于编辑态，与 OAuth2/OIDC 互斥（见 useSsoEnable）。
 */
import { ref, reactive, computed, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { QForm, QInput } from "quasar";
import { getCasConfigApi, saveCasConfigApi } from "../../apis/sso-config";
import { defaultCasConfig, type CasConfig } from "../../types/sso";
import { useSsoEnable } from "../../composables/useSsoEnable";
import SsoExtraParamsEditor from "./sso/SsoExtraParamsEditor.vue";
import SsoSection from "./sso/SsoSection.vue";
import SsoSectionGroup from "./sso/SsoSectionGroup.vue";
import SsoStatusBadge from "./sso/SsoStatusBadge.vue";

const { t } = useI18n({ useScope: "global" });

const saving = ref(false);
const editing = ref(false);
const formRef = ref<QForm | null>(null);
const adminServiceUrlRef = ref<QInput | null>(null);
const portalServiceUrlRef = ref<QInput | null>(null);

const form = reactive<CasConfig>(defaultCasConfig());
/** 进入编辑前的快照，用于「取消」回滚 */
let snapshot = defaultCasConfig();
/**
 * validateUrl 是否被用户手动改写。false=跟随 serverUrlPrefix+protocolVersion 自动重算；
 * true=用户已定制（例如非标路径 /v3/serviceValidate 或自定义网关），不再自动覆盖，
 * 编辑态下通过输入框右侧「恢复自动」按钮一键复位。
 */
const validateUrlDirty = ref(false);
let snapshotDirty = false;

/**
 * 折叠分组默认展开态：只放开「基本信息」（已并入服务端连接与备注），
 * 属性映射/扩展参数收起以缩短页面。
 * CAS 服务端连接、备注本就属于「配置这个 CAS」的基础信息，不再单列成组；
 * 原「校验选项」（renew/gateway/ignoreSsl）与「默认一律跳 SSO、已登录即免登」的
 * 校园统一登录场景相冲突或冗余，已整体移除。
 */
const openSections = reactive({
  basic: true,
  attr: false,
  extra: false
});

const { enabled, toggling, handleToggle } = useSsoEnable("CAS", () =>
  t('ssoConfig.providerOauth2')
);

const protocolOptions = computed(() => [
  { label: t('ssoConfig.protocolV2'), value: "V2" },
  { label: t('ssoConfig.protocolV3'), value: "V3" }
]);

const bindingPolicyOptions = computed(() => [
  { label: t('ssoConfig.bindingAutoBind'), description: t('ssoConfig.bindingAutoBindDesc'), value: "AUTO_BIND_BY_MATCH" },
  { label: t('ssoConfig.bindingAutoCreate'), description: t('ssoConfig.bindingAutoCreateDesc'), value: "AUTO_CREATE" },
  { label: t('ssoConfig.bindingRequirePrebind'), description: t('ssoConfig.bindingRequirePrebindDesc'), value: "REQUIRE_PREBIND" }
]);

const rules = computed(() => ({
  serverUrlPrefix: [
    (v: string) => !!v?.trim() || t('ssoConfig.required'),
    (v: string) => /^https?:\/\//i.test(v?.trim() ?? "") || t('ssoConfig.urlHttpRule')
  ],
  adminServiceUrl: [
    (v: string) => !v?.trim() || /^https?:\/\//i.test(v.trim()) || t('ssoConfig.urlHttpRule'),
    () => (form.adminServiceUrl?.trim() || form.portalServiceUrl?.trim()) ? true : t('ssoConfig.callbackRequiredHint')
  ],
  portalServiceUrl: [
    (v: string) => !v?.trim() || /^https?:\/\//i.test(v.trim()) || t('ssoConfig.urlHttpRule'),
    () => (form.adminServiceUrl?.trim() || form.portalServiceUrl?.trim()) ? true : t('ssoConfig.callbackRequiredHint')
  ],
  validateUrl: [
    (v: string) => !!v?.trim() || t('ssoConfig.required')
  ]
}));

/** 属性映射已配置项数（非空字段），收起时在组头徽标提示 */
const attrConfiguredCount = computed(
  () =>
    Object.values(form.attributeMapping).filter(
      (v) => typeof v === "string" && v.trim() !== ""
    ).length
);

/** 扩展参数已配置行数（key 非空即计入） */
const extraParamCount = computed(
  () => form.extraParams.filter((p) => p.key.trim() !== "").length
);

/**
 * 按 CAS 协议约定给出票据校验端点「路径」：V2 → /serviceValidate，V3 → /p3/serviceValidate。
 * 前端只保存路径，最终调用地址由后端用 serverUrlPrefix + 本路径拼装。
 */
function computeValidateUrl(): string {
  return form.protocolVersion === "V3" ? "/p3/serviceValidate" : "/serviceValidate";
}

function resetValidateUrl() {
  validateUrlDirty.value = false;
  form.validateUrl = computeValidateUrl();
}

/**
 * protocolVersion 变化时，仅当用户未手改 validateUrl 才跟随重算。
 * 程序赋值不会触发 q-input 的 update:model-value，所以不会误标 dirty。
 */
watch(
  () => form.protocolVersion,
  () => {
    if (!validateUrlDirty.value) {
      form.validateUrl = computeValidateUrl();
    }
  }
);

/**
 * 跨字段联动校验：admin 和 portal 回调地址「至少填一个」。
 * 当用户修改其中一个时，另一个若曾标红也需自动清除，避免填了一个另一个还红着。
 * 只校验这两个字段，不触发表单其他字段。
 */
watch(
  () => [form.adminServiceUrl, form.portalServiceUrl],
  () => {
    adminServiceUrlRef.value?.validate();
    portalServiceUrlRef.value?.validate();
  }
);

async function loadConfig() {
  try {
    const result = await getCasConfigApi();
    if (result.code === 10_000 && result.data) {
      Object.assign(form, defaultCasConfig(), result.data);
      enabled.value = !!result.data.enabled;
      // 已存配置：与自动派生值一致则视为跟随，否则视为历史定制
      validateUrlDirty.value =
        !!form.validateUrl && form.validateUrl !== computeValidateUrl();
    } else {
      Object.assign(form, defaultCasConfig());
      enabled.value = false;
      validateUrlDirty.value = false;
      form.validateUrl = computeValidateUrl();
      if (result.code !== 10_000) {
        showToast(result.message || t('ssoConfig.loadFail'), "negative");
      }
    }
  } catch (error) {
    Object.assign(form, defaultCasConfig());
    validateUrlDirty.value = false;
    if (!isNotificationHandled(error)) {
      showToast(t('ssoConfig.loadFail'), "negative");
    }
  }
}

function startEdit() {
  snapshot = JSON.parse(JSON.stringify(form)) as CasConfig;
  snapshotDirty = validateUrlDirty.value;
  editing.value = true;
}

async function cancelEdit() {
  Object.assign(form, JSON.parse(JSON.stringify(snapshot)) as CasConfig);
  validateUrlDirty.value = snapshotDirty;
  editing.value = false;
  await formRef.value?.resetValidation();
}

async function handleSave() {
  const valid = await formRef.value?.validate();
  if (!valid) return;
  saving.value = true;
  try {
    // enabled 由独立开关接口单点管理，保存配置时不提交，避免携带页面陈旧值绕过互斥
    const clone = JSON.parse(JSON.stringify(form)) as Partial<CasConfig>;
    delete clone.enabled;
    const payload = clone as Omit<CasConfig, "enabled">;
    const result = await saveCasConfigApi(payload);
    if (result.code === 10_000) {
      showToast(t('ssoConfig.saveSuccess'), "positive");
      editing.value = false;
      await loadConfig();
    } else {
      showToast(result.message || t('ssoConfig.saveFail'), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t('ssoConfig.saveFail'), "negative");
    }
  } finally {
    saving.value = false;
  }
}

onMounted(loadConfig);
</script>

<template>
  <div class="sso-config-page">
    <div class="sso-config-shell">
      <!-- 头部：标题 + 启用开关 + 编辑/保存动作（不再包卡片，手风琴卡片本身即唯一外框） -->
      <div class="sso-config-header row items-center no-wrap">
        <div class="col">
          <div class="row items-center no-wrap">
            <div class="text-h6">{{ t('ssoConfig.casTitle') }}</div>
            <SsoStatusBadge :enabled="enabled" class="q-ml-sm" />
            <span v-if="!editing" class="sso-readonly-pill q-ml-sm">
              <q-icon name="sym_r_lock" size="13px" />
              {{ t("ssoConfig.readonlyBadge") }}
            </span>
          </div>
          <div class="text-caption text-grey-7">{{ t('ssoConfig.casSubtitle') }}</div>
        </div>

        <div class="row items-center no-wrap q-gutter-x-md">
          <div class="sso-enable-control">
            <span class="sso-enable-control__label">{{ t('ssoConfig.enableSwitch') }}</span>
            <q-toggle
              :model-value="enabled"
              :disable="toggling"
              color="positive"
              dense
              @update:model-value="(v: boolean) => handleToggle(v)"
            />
            <q-spinner v-if="toggling" size="16px" />
          </div>

          <template v-if="!editing">
            <q-btn
              unelevated
              no-caps
              color="primary"
              :label="t('common.edit')"
              class="sso-action-btn"
              @click="startEdit"
            />
          </template>
          <template v-else>
            <q-btn
              outline
              no-caps
              color="grey-7"
              :label="t('common.cancel')"
              class="sso-action-btn"
              @click="cancelEdit"
            />
            <q-btn
              unelevated
              no-caps
              color="primary"
              :label="t('common.save')"
              :loading="saving"
              class="sso-action-btn"
              @click="handleSave"
            />
          </template>
        </div>
      </div>

      <!-- 表单主体：分组折叠，进入编辑态自动全展开 -->
      <q-form ref="formRef" class="sso-form">
        <SsoSectionGroup>
          <!-- 基本信息：绑定策略 + CAS 服务端连接 + 备注，同属「配置这个 CAS」 -->
          <SsoSection
            v-model:open="openSections.basic"
            icon="sym_r_dns"
            :title="t('ssoConfig.sectionBasic')"
          >
            <div class="row q-col-gutter-md">
              <div class="col-12">
                <q-select
                  v-model="form.bindingPolicy"
                  :label="t('ssoConfig.bindingPolicy')"
                  :options="bindingPolicyOptions"
                  option-label="label" option-value="value" emit-value map-options
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                >
                  <template #option="scope">
                    <q-item v-bind="scope.itemProps">
                      <q-item-section>
                        <q-item-label>{{ scope.opt.label }}</q-item-label>
                        <q-item-label caption>{{ scope.opt.description }}</q-item-label>
                      </q-item-section>
                    </q-item>
                  </template>
                </q-select>
              </div>
              <div class="col-12">
                <q-input
                  v-model.trim="form.serverUrlPrefix"
                  :label="t('ssoConfig.serverUrlPrefix')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.serverUrlPrefix : []"
                  class="required-field"
                  :placeholder="t('ssoConfig.serverUrlPrefixPlaceholder')"
                />
              </div>
              <div class="col-12 col-md-6">
                <q-input
                  ref="adminServiceUrlRef"
                  v-model.trim="form.adminServiceUrl"
                  :label="t('ssoConfig.adminServiceUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.adminServiceUrl : []"
                  :placeholder="t('ssoConfig.adminServiceUrlPlaceholder')"
                  :hint="t('ssoConfig.adminServiceUrlHint')"
                />
              </div>
              <div class="col-12 col-md-6">
                <q-input
                  ref="portalServiceUrlRef"
                  v-model.trim="form.portalServiceUrl"
                  :label="t('ssoConfig.portalServiceUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.portalServiceUrl : []"
                  :placeholder="t('ssoConfig.portalServiceUrlPlaceholder')"
                  :hint="t('ssoConfig.portalServiceUrlHint')"
                />
              </div>
              <div class="col-12 col-md-6">
                <q-input
                  v-model.trim="form.loginUrl"
                  :label="t('ssoConfig.loginUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-6">
                <q-input
                  v-model.trim="form.logoutUrl"
                  :label="t('ssoConfig.logoutUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-4">
                <q-select
                  v-model="form.protocolVersion"
                  :label="t('ssoConfig.protocolVersion')"
                  :options="protocolOptions"
                  option-label="label" option-value="value" emit-value map-options
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-8">
                <q-input
                  :model-value="form.validateUrl"
                  :label="t('ssoConfig.validateUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.validateUrl : []"
                  class="required-field"
                  :hint="validateUrlDirty
                    ? t('ssoConfig.validateUrlHintManual')
                    : t('ssoConfig.validateUrlHintAuto', { v: form.protocolVersion })"
                  @update:model-value="
                    (v: unknown) => {
                      form.validateUrl = String(v ?? '').trim();
                      validateUrlDirty = true;
                    }
                  "
                >
                  <template v-if="editing && validateUrlDirty" #append>
                    <q-btn
                      flat
                      dense
                      round
                      size="sm"
                      icon="sym_r_restart_alt"
                      @click="resetValidateUrl"
                    >
                      <q-tooltip>{{ t("ssoConfig.resetValidateUrl") }}</q-tooltip>
                    </q-btn>
                  </template>
                </q-input>
              </div>
              <div class="col-12">
                <q-input
                  v-model="form.remark"
                  :label="t('ssoConfig.remark')"
                  type="textarea" rows="2" autogrow
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
            </div>
          </SsoSection>

          <!-- 属性映射 -->
          <SsoSection
            v-model:open="openSections.attr"
            icon="sym_r_account_tree"
            :title="t('ssoConfig.sectionAttrMapping')"
            :caption="t('ssoConfig.casAttrHint')"
            :badge="
              attrConfiguredCount > 0
                ? t('ssoConfig.configuredN', { n: attrConfiguredCount })
                : undefined
            "
          >
            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.principal" :label="t('ssoConfig.attrPrincipal')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhPrincipal')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.username" :label="t('ssoConfig.attrUsername')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhUsername')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.realName" :label="t('ssoConfig.attrRealName')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhRealName')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.email" :label="t('ssoConfig.attrEmail')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhEmail')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.phone" :label="t('ssoConfig.attrPhone')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhPhone')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.dept" :label="t('ssoConfig.attrDept')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhDept')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.roles" :label="t('ssoConfig.attrRoles')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :placeholder="t('ssoConfig.casAttrPhRoles')" />
              </div>
            </div>
          </SsoSection>

          <!-- 扩展参数 -->
          <SsoSection
            v-model:open="openSections.extra"
            icon="sym_r_extension"
            :title="t('ssoConfig.sectionExtraParams')"
            :badge="
              extraParamCount > 0
                ? t('ssoConfig.configuredN', { n: extraParamCount })
                : undefined
            "
          >
            <SsoExtraParamsEditor v-model="form.extraParams" :readonly="!editing" />
          </SsoSection>
        </SsoSectionGroup>
      </q-form>
    </div>
  </div>
</template>

<style scoped>
.sso-config-page {
  height: calc(100vh - 64px - 40px - 44px - 16px);
  overflow-y: auto;
  padding: 20px 24px;
}

/* 页面级容器：不再套卡片，避免与手风琴卡片形成「框中框」 */
.sso-config-shell {
  max-width: 1120px;
  margin: 0 auto;
}

.sso-config-header {
  gap: 12px;
  padding: 4px 4px 16px;
}

.sso-form {
  width: 100%;
}

/* 头部操作按钮统一最小宽度，对齐「添加用户」抽屉底部按钮 */
.sso-action-btn {
  min-width: 72px;
}

/* 必填字段标签后追加红色星号 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

/* 启用开关做成药丸控件，避免「文字 + 裸开关」的松散感 */
.sso-enable-control {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px 8px 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.045);
}

.sso-enable-control__label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.6);
}

/* 只读模式徽标：非编辑态提示表单当前不可直接修改，软底 + 锁图标，弱化不抢状态徽章 */
.sso-readonly-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.7;
  white-space: nowrap;
  color: rgba(0, 0, 0, 0.5);
  background: rgba(0, 0, 0, 0.05);
}
</style>

<style>
.body--dark .sso-config-page .sso-enable-control {
  background: rgba(255, 255, 255, 0.08);
}

.body--dark .sso-config-page .sso-enable-control__label {
  color: rgba(255, 255, 255, 0.65);
}

.body--dark .sso-config-page .sso-readonly-pill {
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.08);
}

/*
 * 暗色态表单字段整体覆写：比照各抽屉「添加/修改」表单（UserDrawerContent 等）的
 * .body--dark 字段块，统一 .sso-form 内所有 filled/square q-input、q-select 在深底下的
 * 底色、输入文字、标签(标题)、下边框与聚焦强调色，修复沿用浅色配色导致的
 * 标签看不清、主题不兼容问题。聚焦标签与下划线用 #80cbc4（全站暗色表单统一 teal），
 * 手风琴组头 accent 仍用 #4db6ac（与 MysqlDatasourceForm 一致），二者各有分工、不混用。
 * 注意：不覆盖 ::placeholder，让 Quasar dark 模式自行处理占位符颜色（与 UserDrawerContent 一致）。
 */
.body--dark .sso-config-page .sso-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .sso-config-page .sso-form .q-field__native,
.body--dark .sso-config-page .sso-form .q-field__prefix,
.body--dark .sso-config-page .sso-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .sso-config-page .sso-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .sso-config-page .sso-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .sso-config-page .sso-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .sso-config-page .sso-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}
</style>
