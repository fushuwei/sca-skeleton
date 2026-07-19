<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysPost, SysTenant } from "../../types/auth";
import { createPostApi, updatePostApi } from "../../apis/post";
import { getTenantListApi } from "../../apis/tenant";
import { useAuthStore } from "../../stores/auth";

const { t } = useI18n({ useScope: "global" });
const authStore = useAuthStore();

const props = defineProps<{
  mode: "add" | "edit" | "view";
  post?: SysPost;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");
/** 当前登录用户是否为超管（超管创建时需选择目标租户） */
const isSuperadmin = computed(() => authStore.isSuperadmin);

const formLoading = ref(false);
const form = reactive({
  id: "",
  tenantId: "",
  name: "",
  code: "",
  sort: 100,
  remark: ""
});

// ── 租户下拉数据（仅超管加载） ──
const tenantOptions = ref<SysTenant[]>([]);

async function loadTenantOptions() {
  if (!isSuperadmin.value) return;
  try {
    const result = await getTenantListApi();
    if (result.code === 10_000 && result.data) {
      tenantOptions.value = result.data;
    }
  } catch {
    // 静默失败，下拉为空
  }
}

const tenantOptionsFormatted = computed(() =>
  tenantOptions.value.map(t => ({ label: t.name, value: t.id }))
);

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("postMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("postMgmt.codeRequired")]
}));

/** 岗位编码前缀常量 */
const CODE_PREFIX = "POST_";

/** 岗位编码输入处理：自动转大写 */
function onCodeInput(val: string | number | null) {
  form.code = String(val || "").toUpperCase().trim();
}

function resetForm() {
  form.id = "";
  form.tenantId = "";
  form.name = "";
  form.code = "";
  form.sort = 100;
  form.remark = "";
}

function initForm() {
  resetForm();
  if (props.post) {
    form.id = props.post.id;
    form.name = props.post.name;
    // 编辑/查看时剥离 POST_ 前缀，仅展示后缀部分
    form.code = props.post.code?.startsWith(CODE_PREFIX)
      ? props.post.code.slice(CODE_PREFIX.length)
      : (props.post.code ?? "");
    form.sort = props.post.sort ?? 100;
    form.remark = props.post.remark || "";
  }
}

watch(() => props.post, initForm, { immediate: true });

onMounted(() => {
  loadTenantOptions();
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    name: form.name?.trim(),
    code: `${CODE_PREFIX}${form.code?.trim()}`,
    sort: form.sort,
    remark: form.remark?.trim() || undefined
  };

  // 超管创建时传目标租户 ID
  if (props.mode === "add" && isSuperadmin.value) {
    data.tenantId = form.tenantId;
  }

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createPostApi(data);
    } else {
      data.id = form.id;
      data.version = props.post?.version;
      result = await updatePostApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("postMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("postMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("postMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="post-drawer-content">
    <q-form class="post-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 目标租户（仅超管创建时显示） -->
        <div v-if="mode === 'add' && isSuperadmin" class="col-12">
          <q-select
            v-model="form.tenantId"
            :label="t('common.targetTenant')"
            filled
            square
            :options="tenantOptionsFormatted"
            emit-value
            map-options
            :rules="[(v: string) => !!v || t('common.targetTenantRequired')]"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 岗位名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('postMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 岗位编码 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.code"
            @update:model-value="onCodeInput"
            :prefix="CODE_PREFIX"
            :label="t('postMgmt.code')"
            filled
            square
            :rules="formRules.code"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('postMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('postMgmt.remark')"
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

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="post-drawer-footer row justify-end q-gutter-sm">
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
.post-drawer-content {
  padding: 0;
}

.post-drawer-footer {
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

/* 修复 prefix 右侧多余间距 */
:deep(.q-field__prefix) {
  padding-right: 0 !important;
}
</style>

<style>
.body--dark .post-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .post-drawer-form .q-field__native,
.body--dark .post-drawer-form .q-field__prefix,
.body--dark .post-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .post-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .post-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .post-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .post-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .post-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
