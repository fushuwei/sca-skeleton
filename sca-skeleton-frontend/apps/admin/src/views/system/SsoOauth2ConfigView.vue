<script setup lang="ts">
/**
 * 统一身份认证 - OAuth2/OIDC 配置页
 *
 * 与 CAS 页对称为「只读表单 + 编辑保存」；顶部启用开关互斥。
 * clientSecret 后端回显为掩码（clientSecretMasked），未改动则保存时不覆盖。
 */
import { ref, reactive, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { QForm } from "quasar";
import { getOauth2ConfigApi, saveOauth2ConfigApi } from "../../apis/sso-config";
import { defaultOauth2Config, type Oauth2Config } from "../../types/sso";
import { useSsoEnable } from "../../composables/useSsoEnable";
import SsoExtraParamsEditor from "./sso/SsoExtraParamsEditor.vue";
import SsoSection from "./sso/SsoSection.vue";
import SsoSectionGroup from "./sso/SsoSectionGroup.vue";
import SsoStatusBadge from "./sso/SsoStatusBadge.vue";

const { t } = useI18n({ useScope: "global" });

const loading = ref(false);
const saving = ref(false);
const editing = ref(false);
const revealSecret = ref(false);
const formRef = ref<QForm | null>(null);

const form = reactive<Oauth2Config>(defaultOauth2Config());
let snapshot: Oauth2Config = defaultOauth2Config();

/**
 * 折叠分组默认展开态：与 CAS 页一致，把「服务端连接信息」（发现 issuer + 手动端点）
 * 和备注并入「基本信息」，不再单列成组；保留客户端参数/属性映射/扩展参数。
 */
const SECTION_DEFAULTS = {
  basic: true,
  client: true,
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

const { enabled, toggling, handleToggle } = useSsoEnable("OAUTH2", () =>
  t('ssoConfig.providerCas')
);

const authMethodOptions = computed(() => [
  { label: t('ssoConfig.authMethodBasic'), value: "client_secret_basic" },
  { label: t('ssoConfig.authMethodPost'), value: "client_secret_post" },
  { label: t('ssoConfig.authMethodJwt'), value: "client_secret_jwt" },
  { label: t('ssoConfig.authMethodPrivateKeyJwt'), value: "private_key_jwt" }
]);

const pkceMethodOptions = computed(() => [
  { label: "S256", value: "S256" },
  { label: "plain", value: "plain" }
]);

const bindingPolicyOptions = computed(() => [
  { label: t('ssoConfig.bindingAutoCreate'), value: "AUTO_CREATE" },
  { label: t('ssoConfig.bindingAutoBind'), value: "AUTO_BIND_BY_MATCH" },
  { label: t('ssoConfig.bindingRequirePrebind'), value: "REQUIRE_PREBIND" }
]);

/** 需要手动填写端点的条件：未启用 OIDC Discovery */
const manualEndpoints = computed(() => !form.useDiscovery);

const rules = computed(() => ({
  clientId: [(v: string) => !!v?.trim() || t('ssoConfig.required')],
  redirectUri: [
    (v: string) => /^https?:\/\//i.test(v?.trim() ?? "") || t('ssoConfig.urlHttpRule')
  ],
  issuer: [
    (v: string) =>
      !form.useDiscovery || /^https?:\/\//i.test(v?.trim() ?? "") || t('ssoConfig.urlHttpRule')
  ],
  authorizationUri: [
    (v: string) => !manualEndpoints.value || !!v?.trim() || t('ssoConfig.required')
  ],
  tokenUri: [(v: string) => !manualEndpoints.value || !!v?.trim() || t('ssoConfig.required')],
  userInfoUri: [(v: string) => !manualEndpoints.value || !!v?.trim() || t('ssoConfig.required')]
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

async function loadConfig() {
  loading.value = true;
  try {
    const result = await getOauth2ConfigApi();
    if (result.code === 10_000 && result.data) {
      Object.assign(form, defaultOauth2Config(), result.data);
      enabled.value = !!result.data.enabled;
    } else {
      Object.assign(form, defaultOauth2Config());
      enabled.value = false;
      if (result.code !== 10_000) {
        showToast(result.message || t('ssoConfig.loadFail'), "negative");
      }
    }
  } catch (error) {
    Object.assign(form, defaultOauth2Config());
    if (!isNotificationHandled(error)) {
      showToast(t('ssoConfig.loadFail'), "negative");
    }
  } finally {
    loading.value = false;
  }
}

function startEdit() {
  snapshot = JSON.parse(JSON.stringify(form)) as Oauth2Config;
  revealSecret.value = false;
  expandAllSections();
  editing.value = true;
}

async function cancelEdit() {
  Object.assign(form, JSON.parse(JSON.stringify(snapshot)) as Oauth2Config);
  editing.value = false;
  revealSecret.value = false;
  resetSections();
  await formRef.value?.resetValidation();
}

async function handleSave() {
  const valid = await formRef.value?.validate();
  if (!valid) return;
  saving.value = true;
  try {
    const copy: Oauth2Config = JSON.parse(JSON.stringify(form));
    copy.enabled = enabled.value;
    // clientSecret 仍为掩码（用户未改）时剔除，交由服务端保留原值
    if (copy.clientSecretMasked) {
      copy.clientSecret = "";
    }
    delete copy.clientSecretMasked;
    const result = await saveOauth2ConfigApi(copy);
    if (result.code === 10_000) {
      showToast(t('ssoConfig.saveSuccess'), "positive");
      editing.value = false;
      revealSecret.value = false;
      resetSections();
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
    <q-spinner-dots v-if="loading" size="40px" color="teal" class="page-loading" />

    <div v-else class="sso-config-shell">
      <!-- 头部：标题 + 启用开关 + 编辑/保存动作（不再套卡片，手风琴卡片即唯一外框） -->
      <div class="sso-config-header row items-center no-wrap">
        <div class="col">
          <div class="row items-center no-wrap">
            <div class="text-h6">{{ t('ssoConfig.oauth2Title') }}</div>
            <SsoStatusBadge :enabled="enabled" class="q-ml-sm" />
          </div>
          <div class="text-caption text-grey-7">{{ t('ssoConfig.oauth2Subtitle') }}</div>
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
            <q-btn outline no-caps color="primary" icon="sym_r_edit" :label="t('common.edit')" @click="startEdit" />
          </template>
          <template v-else>
            <q-btn flat no-caps :label="t('common.cancel')" @click="cancelEdit" />
            <q-btn unelevated no-caps color="primary" icon="sym_r_save" :label="t('common.save')" :loading="saving" @click="handleSave" />
          </template>
        </div>
      </div>

      <!-- 表单主体：分组折叠，进入编辑态自动全展开 -->
      <q-form ref="formRef" class="sso-form">
        <SsoSectionGroup>
          <!-- 基本信息：名称/绑定策略 + 服务端连接信息（发现 issuer、手动端点）+ 备注 -->
          <SsoSection
            v-model:open="openSections.basic"
            icon="sym_r_dns"
            :title="t('ssoConfig.sectionBasic')"
          >
            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.name" :label="t('ssoConfig.configName')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
              <div class="col-12 col-md-6">
                <q-select
                  v-model="form.bindingPolicy" :label="t('ssoConfig.bindingPolicy')"
                  :options="bindingPolicyOptions" option-label="label" option-value="value" emit-value map-options
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-4">
                <q-toggle v-model="form.useDiscovery" :label="t('ssoConfig.useDiscovery')" :disable="!editing" dense />
              </div>
              <div class="col-12">
                <q-input
                  v-model.trim="form.issuer" :label="t('ssoConfig.issuer')"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                  :rules="editing ? rules.issuer : []"
                  :placeholder="t('ssoConfig.issuerPlaceholder')"
                  :hint="t('ssoConfig.issuerHint')"
                />
              </div>
              <template v-if="manualEndpoints">
                <div class="col-12 col-md-6">
                  <q-input v-model.trim="form.authorizationUri" :label="t('ssoConfig.authorizationUri')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :rules="editing ? rules.authorizationUri : []" />
                </div>
                <div class="col-12 col-md-6">
                  <q-input v-model.trim="form.tokenUri" :label="t('ssoConfig.tokenUri')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :rules="editing ? rules.tokenUri : []" />
                </div>
                <div class="col-12 col-md-6">
                  <q-input v-model.trim="form.userInfoUri" :label="t('ssoConfig.userInfoUri')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :rules="editing ? rules.userInfoUri : []" />
                </div>
                <div class="col-12 col-md-6">
                  <q-input v-model.trim="form.jwkSetUri" :label="t('ssoConfig.jwkSetUri')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :hint="t('ssoConfig.jwkSetUriHint')" />
                </div>
              </template>
              <div class="col-12">
                <q-input v-model="form.remark" :label="t('ssoConfig.remark')" type="textarea" rows="2" autogrow filled square :disable="!editing" :readonly="!editing" hide-bottom-space />
              </div>
            </div>
          </SsoSection>

          <!-- 客户端参数 -->
          <SsoSection
            v-model:open="openSections.client"
            icon="sym_r_vpn_key"
            :title="t('ssoConfig.sectionClient')"
          >
            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.clientId" :label="t('ssoConfig.clientId')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :rules="editing ? rules.clientId : []" class="required-field" />
              </div>
              <div class="col-12 col-md-6">
                <q-input
                  v-model.trim="form.clientSecret"
                  :label="t('ssoConfig.clientSecret')"
                  :type="revealSecret ? 'text' : 'password'"
                  :password-toggle="false"
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                >
                  <template #append>
                    <q-btn flat dense round size="sm" :icon="revealSecret ? 'sym_r_visibility_off' : 'sym_r_visibility'" :disable="!editing" @click="revealSecret = !revealSecret" />
                  </template>
                </q-input>
                <div v-if="form.clientSecretMasked && editing" class="text-caption text-grey-7 q-mt-xs">
                  {{ t('ssoConfig.clientSecretMaskedHint') }}
                </div>
              </div>
              <div class="col-12">
                <q-input v-model.trim="form.redirectUri" :label="t('ssoConfig.redirectUri')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :rules="editing ? rules.redirectUri : []" class="required-field" :placeholder="t('ssoConfig.redirectUriPlaceholder')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model.trim="form.scope" :label="t('ssoConfig.scope')" filled square :disable="!editing" :readonly="!editing" hide-bottom-space :hint="t('ssoConfig.scopeHint')" />
              </div>
              <div class="col-12 col-md-6">
                <q-input v-model="form.responseType" :label="t('ssoConfig.responseType')" filled square readonly disable :hint="t('ssoConfig.responseTypeHint')" />
              </div>
              <div class="col-12 col-md-6">
                <q-select
                  v-model="form.clientAuthenticationMethod" :label="t('ssoConfig.clientAuthMethod')"
                  :options="authMethodOptions" option-label="label" option-value="value" emit-value map-options
                  filled square :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
              <div class="col-12 col-md-3">
                <q-toggle v-model="form.usePkce" :label="t('ssoConfig.usePkce')" :disable="!editing" dense />
              </div>
              <div class="col-12 col-md-3">
                <q-select
                  v-if="form.usePkce" v-model="form.pkceMethod" :label="t('ssoConfig.pkceMethod')"
                  :options="pkceMethodOptions" option-label="label" option-value="value" emit-value map-options
                  filled square dense :disable="!editing" :readonly="!editing" hide-bottom-space
                />
              </div>
            </div>
          </SsoSection>

          <!-- 属性映射 -->
          <SsoSection
            v-model:open="openSections.attr"
            icon="sym_r_account_tree"
            :title="t('ssoConfig.sectionAttrMapping')"
            :caption="t('ssoConfig.oauth2AttrHint')"
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

.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}
</style>
