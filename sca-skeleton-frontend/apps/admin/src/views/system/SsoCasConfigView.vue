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
import type { QForm } from "quasar";
import { getCasConfigApi, saveCasConfigApi } from "../../apis/sso-config";
import { defaultCasConfig, type CasConfig } from "../../types/sso";
import { useSsoEnable } from "../../composables/useSsoEnable";
import SsoExtraParamsEditor from "./sso/SsoExtraParamsEditor.vue";
import SsoSection from "./sso/SsoSection.vue";
import SsoSectionGroup from "./sso/SsoSectionGroup.vue";

const { t } = useI18n({ useScope: "global" });

const loading = ref(false);
const saving = ref(false);
const editing = ref(false);
const formRef = ref<QForm | null>(null);

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
 * 校验选项/属性映射/扩展参数收起以缩短页面。
 * CAS 服务端连接、备注本就属于「配置这个 CAS」的基础信息，不再单列成组。
 */
const SECTION_DEFAULTS = {
  basic: true,
  advanced: false,
  attr: false,
  extra: false
};
const openSections = reactive<Record<keyof typeof SECTION_DEFAULTS, boolean>>({
  ...SECTION_DEFAULTS
});

function expandAllSections() {
  (Object.keys(openSections) as (keyof typeof openSections)[]).forEach(
    (k) => (openSections[k] = true)
  );
}
function resetSections() {
  Object.assign(openSections, SECTION_DEFAULTS);
}

const { enabled, toggling, handleToggle } = useSsoEnable("CAS", () =>
  t('ssoConfig.providerOauth2')
);

const protocolOptions = computed(() => [
  { label: t('ssoConfig.protocolV2'), value: "V2" },
  { label: t('ssoConfig.protocolV3'), value: "V3" }
]);

const bindingPolicyOptions = computed(() => [
  { label: t('ssoConfig.bindingAutoCreate'), value: "AUTO_CREATE" },
  { label: t('ssoConfig.bindingAutoBind'), value: "AUTO_BIND_BY_MATCH" },
  { label: t('ssoConfig.bindingRequirePrebind'), value: "REQUIRE_PREBIND" }
]);

const rules = computed(() => ({
  serverUrlPrefix: [
    (v: string) => /^https?:\/\//i.test(v?.trim() ?? "") || t('ssoConfig.urlHttpRule')
  ],
  serviceUrl: [
    (v: string) => /^https?:\/\//i.test(v?.trim() ?? "") || t('ssoConfig.urlHttpRule')
  ],
  validateUrl: [(v: string) => !!v?.trim() || t('ssoConfig.required')]
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

async function loadConfig() {
  loading.value = true;
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
  } finally {
    loading.value = false;
  }
}

function startEdit() {
  snapshot = JSON.parse(JSON.stringify(form)) as CasConfig;
  snapshotDirty = validateUrlDirty.value;
  expandAllSections();
  editing.value = true;
}

async function cancelEdit() {
  Object.assign(form, JSON.parse(JSON.stringify(snapshot)) as CasConfig);
  validateUrlDirty.value = snapshotDirty;
  editing.value = false;
  resetSections();
  await formRef.value?.resetValidation();
}

async function handleSave() {
  const valid = await formRef.value?.validate();
  if (!valid) return;
  saving.value = true;
  try {
    const payload: CasConfig = { ...JSON.parse(JSON.stringify(form)), enabled: enabled.value };
    const result = await saveCasConfigApi(payload);
    if (result.code === 10_000) {
      showToast(t('ssoConfig.saveSuccess'), "positive");
      editing.value = false;
      resetSections();
      snapshot = JSON.parse(JSON.stringify(payload)) as CasConfig;
      snapshotDirty = validateUrlDirty.value;
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
    <q-spinner-dots v-if="loading" size="40px" color="teal" class="page-loading" />

    <div v-else class="sso-config-shell">
      <!-- 头部：标题 + 启用开关 + 编辑/保存动作（不再包卡片，手风琴卡片本身即唯一外框） -->
      <div class="sso-config-header row items-center no-wrap">
        <div class="col">
          <div class="row items-center no-wrap">
            <div class="text-h6">{{ t('ssoConfig.casTitle') }}</div>
            <q-badge
              class="q-ml-sm"
              :color="enabled ? 'positive' : 'grey-6'"
              :label="enabled ? t('ssoConfig.enabledBadge') : t('ssoConfig.disabledBadge')"
            />
          </div>
          <div class="text-caption text-grey-7">{{ t('ssoConfig.casSubtitle') }}</div>
        </div>

        <div class="row items-center no-wrap q-gutter-x-md">
          <div class="row items-center no-wrap">
            <span class="text-body2 q-mr-sm">{{ t('ssoConfig.enableSwitch') }}</span>
            <q-toggle
              :model-value="enabled"
              :disable="toggling"
              color="positive"
              @update:model-value="(v: boolean) => handleToggle(v)"
            />
            <q-spinner v-if="toggling" size="18px" class="q-ml-xs" />
          </div>

          <template v-if="!editing">
            <q-badge v-if="!enabled" color="grey-6" :label="t('ssoConfig.readonlyBadge')" />
            <q-btn
              outline
              no-caps
              color="primary"
              icon="sym_r_edit"
              :label="t('common.edit')"
              @click="startEdit"
            />
          </template>
          <template v-else>
            <q-btn flat no-caps :label="t('common.cancel')" @click="cancelEdit" />
            <q-btn
              unelevated
              no-caps
              color="primary"
              icon="sym_r_save"
              :label="t('common.save')"
              :loading="saving"
              @click="handleSave"
            />
          </template>
        </div>
      </div>

      <!-- 表单主体：分组折叠，进入编辑态自动全展开 -->
      <q-form ref="formRef" class="sso-form">
        <SsoSectionGroup>
          <!-- 基本信息：配置名称/绑定策略 + CAS 服务端连接 + 备注，同属「配置这个 CAS」 -->
          <SsoSection
            v-model:open="openSections.basic"
            icon="sym_r_dns"
            :title="t('ssoConfig.sectionBasic')"
          >
            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <q-input
                  v-model.trim="form.name"
                  :label="t('ssoConfig.configName')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-6">
                <q-select
                  v-model="form.bindingPolicy"
                  :label="t('ssoConfig.bindingPolicy')"
                  :options="bindingPolicyOptions"
                  option-label="label" option-value="value" emit-value map-options
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12">
                <q-input
                  v-model.trim="form.serverUrlPrefix"
                  :label="t('ssoConfig.serverUrlPrefix')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.serverUrlPrefix : []"
                  :placeholder="t('ssoConfig.serverUrlPrefixPlaceholder')"
                />
              </div>
              <div class="col-12">
                <q-input
                  v-model.trim="form.serviceUrl"
                  :label="t('ssoConfig.serviceUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.serviceUrl : []"
                  :placeholder="t('ssoConfig.serviceUrlPlaceholder')"
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
              <div class="col-12 col-md-4">
                <q-input
                  v-model.trim="form.loginUrl"
                  :label="t('ssoConfig.loginUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-4">
                <q-input
                  v-model.trim="form.logoutUrl"
                  :label="t('ssoConfig.logoutUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12">
                <q-input
                  :model-value="form.validateUrl"
                  :label="t('ssoConfig.validateUrl')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.validateUrl : []"
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

          <!-- 高级选项 -->
          <SsoSection
            v-model:open="openSections.advanced"
            icon="sym_r_tune"
            :title="t('ssoConfig.sectionCasAdvanced')"
          >
            <div class="row q-col-gutter-md items-center">
              <div class="col-12 col-md-4">
                <q-toggle v-model="form.renew" :label="t('ssoConfig.renew')" :disable="!editing" dense />
              </div>
              <div class="col-12 col-md-4">
                <q-toggle v-model="form.gateway" :label="t('ssoConfig.gateway')" :disable="!editing" dense />
              </div>
              <div class="col-12 col-md-4">
                <q-toggle v-model="form.ignoreSslErrors" :label="t('ssoConfig.ignoreSsl')" :disable="!editing" dense />
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
                <q-input v-model.trim="form.attributeMapping.principal" :label="t('ssoConfig.attrPrincipal')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.username" :label="t('ssoConfig.attrUsername')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.realName" :label="t('ssoConfig.attrRealName')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.email" :label="t('ssoConfig.attrEmail')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.phone" :label="t('ssoConfig.attrPhone')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.dept" :label="t('ssoConfig.attrDept')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.attributeMapping.roles" :label="t('ssoConfig.attrRoles')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
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

.page-loading {
  display: block;
  margin: 15vh auto;
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
</style>
