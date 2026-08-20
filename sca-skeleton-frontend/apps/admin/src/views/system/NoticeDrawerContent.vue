<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysNotice, NoticeTargetItem, DeptOption, RoleOption } from "../../types/auth";
import { createNoticeApi, updateNoticeApi } from "../../apis/notice";
import { getDeptOptionsApi } from "../../apis/dept";
import { getRoleOptionsApi } from "../../apis/role";
import DateTimePicker from "../../components/DateTimePicker.vue";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  notice?: SysNotice;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);

const form = reactive({
  id: "",
  title: "",
  type: "",
  content: "",
  level: "",
  status: "draft",
  effectiveTime: "",
  expireTime: "",
  isTop: 0,
  topExpireTime: "",
  isPopup: 0,
  targetType: "",
  targets: [] as NoticeTargetItem[],
  sort: 0,
  remark: "",
  version: 0
});

const formRules = {
  title: [
    (v: string) => !!v?.trim() || t("noticeMgmt.titleRequired"),
    (v: string) => v.length <= 200 || t("noticeMgmt.titleRequired")
  ],
  type: [(v: string) => !!v || t("noticeMgmt.typeRequired")],
  level: [(v: string) => !!v || t("noticeMgmt.levelRequired")],
  targetType: [(v: string) => !!v || t("noticeMgmt.targetTypeRequired")],
  targets: [
    (v: NoticeTargetItem[]) => {
      if (form.targetType === "all") return true;
      return (v?.length ?? 0) > 0 || t("noticeMgmt.targetsRequired");
    }
  ]
};

// ── 类型选项 ──
const typeOptions = computed(() => [
  { label: t("noticeMgmt.typeNotice"), value: "notice" },
  { label: t("noticeMgmt.typeAnnouncement"), value: "announcement" },
  { label: t("noticeMgmt.typeSystem"), value: "system" },
  { label: t("noticeMgmt.typeOther"), value: "other" }
]);

// ── 级别选项 ──
const levelOptions = computed(() => [
  { label: t("noticeMgmt.levelNormal"), value: "normal" },
  { label: t("noticeMgmt.levelImportant"), value: "important" },
  { label: t("noticeMgmt.levelUrgent"), value: "urgent" }
]);

// ── 接收范围选项 ──
const targetTypeOptions = computed(() => [
  { label: t("noticeMgmt.targetTypeAll"), value: "all" },
  { label: t("noticeMgmt.targetTypeDept"), value: "dept" },
  { label: t("noticeMgmt.targetTypeRole"), value: "role" },
  { label: t("noticeMgmt.targetTypeUser"), value: "user" }
]);

// ── 是否选项 ──
const yesNoOptions = computed(() => [
  { label: t("common.yes"), value: 1 },
  { label: t("common.no"), value: 0 }
]);

// ── 下拉数据 ──
const deptOptions = ref<DeptOption[]>([]);
const roleOptions = ref<RoleOption[]>([]);

// 部门多选选项
const deptMultiOptions = computed(() =>
  deptOptions.value.map((d) => ({ label: d.name, value: d.id }))
);

// 角色多选选项
const roleMultiOptions = computed(() =>
  roleOptions.value.map((r) => ({ label: r.name, value: r.id }))
);

// 当前接收范围对应的已选目标 ID 列表
const selectedTargetIds = computed({
  get: () => form.targets.map((t) => t.targetId),
  set: (ids: string[]) => {
    const targetType = form.targetType;
    form.targets = ids.map((id) => {
      const existing = form.targets.find((t) => t.targetId === id);
      return existing ?? { targetType, targetId: id };
    });
  }
});

// 切换接收范围时清空已选目标
watch(() => form.targetType, (val, oldVal) => {
  if (val !== oldVal) {
    form.targets = [];
    // 按需加载选项数据
    if (val === "dept") {
      loadDeptOptions();
    } else if (val === "role") {
      loadRoleOptions();
    }
  }
});

async function loadDeptOptions() {
  try {
    const result = await getDeptOptionsApi();
    if (result.code === 10_000 && result.data) {
      deptOptions.value = result.data;
    }
  } catch {
    // 静默失败
  }
}

async function loadRoleOptions() {
  try {
    const result = await getRoleOptionsApi();
    if (result.code === 10_000 && result.data) {
      roleOptions.value = result.data;
    }
  } catch {
    // 静默失败
  }
}

function resetForm() {
  form.id = "";
  form.title = "";
  form.type = "";
  form.content = "";
  form.level = "";
  form.status = "draft";
  form.effectiveTime = "";
  form.expireTime = "";
  form.isTop = 0;
  form.topExpireTime = "";
  form.isPopup = 0;
  form.targetType = "";
  form.targets = [];
  form.sort = 0;
  form.remark = "";
  form.version = 0;
}

function initForm() {
  resetForm();
  if (props.notice) {
    form.id = props.notice.id;
    form.title = props.notice.title;
    form.type = props.notice.type;
    form.content = props.notice.content;
    form.level = props.notice.level;
    form.status = props.notice.status;
    form.effectiveTime = props.notice.effectiveTime || "";
    form.expireTime = props.notice.expireTime || "";
    form.isTop = props.notice.isTop ?? 0;
    form.topExpireTime = props.notice.topExpireTime || "";
    form.isPopup = props.notice.isPopup ?? 0;
    form.targetType = props.notice.targetType || "";
    form.targets = props.notice.targets
      ? props.notice.targets.map((t) => ({
          targetType: t.targetType,
          targetId: t.targetId
        }))
      : [];
    form.sort = props.notice.sort ?? 0;
    form.remark = props.notice.remark || "";
    form.version = props.notice.version ?? 0;
    // 编辑/查看模式按接收范围加载选项数据
    if (form.targetType === "dept") {
      loadDeptOptions();
    } else if (form.targetType === "role") {
      loadRoleOptions();
    }
  }
}

watch(() => props.notice, initForm, { immediate: true });

onMounted(() => {
  if (props.mode === "add") {
    // 新增模式使用默认值
  }
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    title: form.title,
    type: form.type,
    content: form.content,
    level: form.level,
    status: form.status || "draft",
    effectiveTime: form.effectiveTime || undefined,
    expireTime: form.expireTime || undefined,
    isTop: form.isTop,
    topExpireTime: form.isTop === 1 ? (form.topExpireTime || undefined) : undefined,
    isPopup: form.isPopup,
    targetType: form.targetType,
    targets: form.targetType === "all" ? undefined : form.targets,
    sort: form.sort,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createNoticeApi(data);
    } else {
      data.id = form.id;
      data.version = form.version;
      result = await updateNoticeApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("noticeMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("noticeMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("noticeMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="notice-drawer-content">
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="notice-drawer-form" class="notice-drawer-form notice-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 标题 -->
        <div class="col-12">
          <q-input
            v-model.trim="form.title"
            :label="t('noticeMgmt.title')"
            filled
            autogrow
            square
            :rules="formRules.title"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 类型 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.type"
            :label="t('noticeMgmt.type')"
            filled
            square
            :options="typeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.type"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 重要级别 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.level"
            :label="t('noticeMgmt.level')"
            filled
            square
            :options="levelOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.level"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 内容 -->
        <div class="col-12">
          <q-input
            v-model="form.content"
            :label="t('noticeMgmt.content')"
            filled
            square
            type="textarea"
            rows="6"
            :placeholder="t('noticeMgmt.contentPlaceholder')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="notice-content-editor"
          />
        </div>
        <!-- 接收范围（单独占一行） -->
        <div class="col-12">
          <q-select
            v-model="form.targetType"
            :label="t('noticeMgmt.targetType')"
            filled
            square
            :options="targetTypeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.targetType"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 接收目标 — 部门多选（单独占一行） -->
        <div v-if="form.targetType === 'dept'" class="col-12">
          <q-select
            v-model="selectedTargetIds"
            :label="t('noticeMgmt.targets')"
            filled
            square
            :options="deptMultiOptions"
            emit-value
            map-options
            multiple
            use-chips
            clearable
            :rules="formRules.targets"
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 接收目标 — 角色多选（单独占一行） -->
        <div v-if="form.targetType === 'role'" class="col-12">
          <q-select
            v-model="selectedTargetIds"
            :label="t('noticeMgmt.targets')"
            filled
            square
            :options="roleMultiOptions"
            emit-value
            map-options
            multiple
            use-chips
            clearable
            :rules="formRules.targets"
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 接收目标 — 用户ID（手动输入，单独占一行） -->
        <div v-if="form.targetType === 'user'" class="col-12">
          <q-select
            v-model="selectedTargetIds"
            :label="t('noticeMgmt.targets')"
            filled
            square
            use-input
            use-chips
            multiple
            hide-dropdown-icon
            input-debounce="0"
            new-value-mode="add-unique"
            :rules="formRules.targets"
            :disable="drawerReadonly"
            hide-bottom-space
            :placeholder="t('noticeMgmt.targetsPlaceholder')"
          />
        </div>
        <!-- 是否置顶 + 登录弹窗（占一行） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.isTop"
            :label="t('noticeMgmt.isTop')"
            filled
            square
            :options="yesNoOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.isPopup"
            :label="t('noticeMgmt.isPopup')"
            filled
            square
            :options="yesNoOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 置顶到期时间 + 排序（仅当是否置顶选“是”时显示，占一行） -->
        <div v-if="form.isTop === 1" class="col-12 col-md-6">
          <DateTimePicker
            v-model="form.topExpireTime"
            :label="t('noticeMgmt.topExpireTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :placeholder="t('noticeMgmt.topExpireTimePlaceholder')"
          />
        </div>
        <div v-if="form.isTop === 1" class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('noticeMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 生效时间 -->
        <div class="col-12 col-md-6">
          <DateTimePicker
            v-model="form.effectiveTime"
            :label="t('noticeMgmt.effectiveTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :placeholder="t('noticeMgmt.effectiveTimePlaceholder')"
            :max="form.expireTime"
          />
        </div>
        <!-- 失效时间 -->
        <div class="col-12 col-md-6">
          <DateTimePicker
            v-model="form.expireTime"
            :label="t('noticeMgmt.expireTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :placeholder="t('noticeMgmt.expireTimePlaceholder')"
            :min="form.effectiveTime"
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('noticeMgmt.remark')"
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

    <!-- 底部操作栏 -->
    <div v-if="!drawerReadonly" class="notice-drawer-footer row items-center justify-end no-wrap">
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
        form="notice-drawer-form"
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
/* 三段式抽屉布局 */
.notice-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中间内容区：唯一的滚动区域 */
.notice-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

/* 底部操作栏 */
.notice-drawer-footer {
  flex-shrink: 0;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.drawer-action-btn {
  min-width: 72px;
}

/* 多选下拉框 display-value 文本过长省略 */
.notice-drawer-form :deep(.q-select .q-field__native) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 必填项星号红色高亮 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

/* 下拉箭头旋转动画 */
:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>
