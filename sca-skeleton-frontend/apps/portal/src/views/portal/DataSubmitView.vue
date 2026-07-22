<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type FieldType = "input" | "select" | "number" | "date";

interface FormField {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  options?: string[];
  placeholder?: string;
}

interface SubmitTemplate {
  id: string;
  name: string;
  icon: string;
  color: string;
  description: string;
  fields: FormField[];
}

const templates: SubmitTemplate[] = [
  {
    id: "teaching-plan",
    name: "2025年秋季学期教学计划填报",
    icon: "sym_r_menu_book",
    color: "#009688",
    description: "用于填报本学期各课程教学安排",
    fields: [
      {
        key: "college",
        label: "院系",
        type: "select",
        required: true,
        options: ["计算机学院", "经济管理学院", "机械工程学院", "外国语学院", "理学院"]
      },
      {
        key: "academicYear",
        label: "学年",
        type: "select",
        required: true,
        options: ["2025-2026", "2026-2027"]
      },
      {
        key: "semester",
        label: "学期",
        type: "select",
        required: true,
        options: ["第一学期", "第二学期", "暑期学期"]
      },
      {
        key: "courseName",
        label: "课程名称",
        type: "input",
        required: true,
        placeholder: "请输入课程名称"
      },
      {
        key: "courseCode",
        label: "课程代码",
        type: "input",
        required: true,
        placeholder: "如 CS101"
      },
      {
        key: "hours",
        label: "学时",
        type: "number",
        required: true,
        placeholder: "请输入总学时"
      },
      {
        key: "credits",
        label: "学分",
        type: "number",
        required: true,
        placeholder: "请输入学分"
      },
      {
        key: "startDate",
        label: "开课日期",
        type: "date",
        required: true
      }
    ]
  },
  {
    id: "research-progress",
    name: "科研项目进展季度报告",
    icon: "sym_r_science",
    color: "#1976d2",
    description: "季度科研项目执行情况上报",
    fields: [
      {
        key: "projectName",
        label: "项目名称",
        type: "input",
        required: true,
        placeholder: "请输入项目名称"
      },
      {
        key: "projectCode",
        label: "项目编号",
        type: "input",
        required: true,
        placeholder: "如 KY2025-001"
      },
      {
        key: "leader",
        label: "项目负责人",
        type: "input",
        required: true,
        placeholder: "请输入负责人姓名"
      },
      {
        key: "quarter",
        label: "报告季度",
        type: "select",
        required: true,
        options: ["2025年Q1", "2025年Q2", "2025年Q3", "2025年Q4"]
      },
      {
        key: "budget",
        label: "本季度经费(万元)",
        type: "number",
        required: true,
        placeholder: "请输入经费金额"
      },
      {
        key: "progress",
        label: "完成进度(%)",
        type: "number",
        required: true,
        placeholder: "0-100"
      }
    ]
  },
  {
    id: "budget-execution",
    name: "院系年度预算执行填报",
    icon: "sym_r_account_balance",
    color: "#e65100",
    description: "年度预算执行情况统计",
    fields: [
      {
        key: "college",
        label: "院系",
        type: "select",
        required: true,
        options: ["计算机学院", "经济管理学院", "机械工程学院", "外国语学院", "理学院"]
      },
      {
        key: "fiscalYear",
        label: "预算年度",
        type: "select",
        required: true,
        options: ["2025", "2026"]
      },
      {
        key: "totalBudget",
        label: "预算总额(万元)",
        type: "number",
        required: true,
        placeholder: "请输入预算总额"
      },
      {
        key: "executed",
        label: "已执行金额(万元)",
        type: "number",
        required: true,
        placeholder: "请输入已执行金额"
      },
      {
        key: "executionDate",
        label: "统计截止日期",
        type: "date",
        required: true
      }
    ]
  },
  {
    id: "teacher-info",
    name: "教师基本信息变更登记",
    icon: "sym_r_badge",
    color: "#7b1fa2",
    description: "教师信息变更申报",
    fields: [
      {
        key: "teacherName",
        label: "教师姓名",
        type: "input",
        required: true,
        placeholder: "请输入教师姓名"
      },
      {
        key: "teacherId",
        label: "工号",
        type: "input",
        required: true,
        placeholder: "请输入工号"
      },
      {
        key: "college",
        label: "所属院系",
        type: "select",
        required: true,
        options: ["计算机学院", "经济管理学院", "机械工程学院", "外国语学院", "理学院"]
      },
      {
        key: "changeType",
        label: "变更类型",
        type: "select",
        required: true,
        options: ["职称变动", "院系调动", "联系方式变更", "学历变更", "其他"]
      },
      {
        key: "effectiveDate",
        label: "生效日期",
        type: "date",
        required: true
      }
    ]
  },
  {
    id: "internship",
    name: "学生实习实践数据登记",
    icon: "sym_r_work_history",
    color: "#00838f",
    description: "学生实习实践情况登记",
    fields: [
      {
        key: "studentName",
        label: "学生姓名",
        type: "input",
        required: true,
        placeholder: "请输入学生姓名"
      },
      {
        key: "studentId",
        label: "学号",
        type: "input",
        required: true,
        placeholder: "请输入学号"
      },
      {
        key: "major",
        label: "专业",
        type: "input",
        required: true,
        placeholder: "请输入专业名称"
      },
      {
        key: "company",
        label: "实习单位",
        type: "input",
        required: true,
        placeholder: "请输入实习单位全称"
      },
      {
        key: "startDate",
        label: "实习开始日期",
        type: "date",
        required: true
      },
      {
        key: "endDate",
        label: "实习结束日期",
        type: "date",
        required: true
      }
    ]
  }
];

const activeTemplateId = ref<string>(templates[0].id);
const formName = ref("");
const remark = ref("");
const files = ref<File[]>([]);
const submitting = ref(false);

const activeTemplate = computed<SubmitTemplate>(
  () => templates.find((tp) => tp.id === activeTemplateId.value) ?? templates[0]
);

const templateOptions = computed(() =>
  templates.map((tp) => ({ value: tp.id, label: tp.name }))
);

const formData = ref<Record<string, string>>({});

function selectTemplate(id: string): void {
  activeTemplateId.value = id;
  formData.value = {};
}

function onFileSelected(newFiles: File[]): void {
  files.value = newFiles;
}

function buildSelectOptions(opts?: string[]) {
  return (opts ?? []).map((o) => ({ value: o, label: o }));
}

function validate(): boolean {
  if (!formName.value.trim()) {
    $q.notify({ type: "negative", message: "请填写填报名称", position: "top" });
    return false;
  }
  for (const field of activeTemplate.value.fields) {
    if (field.required && !formData.value[field.key]) {
      $q.notify({ type: "negative", message: `请填写「${field.label}」`, position: "top" });
      return false;
    }
  }
  return true;
}

function saveDraft(): void {
  if (!formName.value.trim()) {
    $q.notify({ type: "warning", message: "请填写填报名称后再保存草稿", position: "top" });
    return;
  }
  submitting.value = true;
  setTimeout(() => {
    submitting.value = false;
    $q.notify({ type: "positive", message: t("dataSubmit.draftSaved"), position: "top" });
  }, 600);
}

function submitForm(): void {
  if (!validate()) return;
  submitting.value = true;
  setTimeout(() => {
    submitting.value = false;
    $q.notify({ type: "positive", message: t("dataSubmit.submitSuccess"), position: "top" });
    formName.value = "";
    formData.value = {};
    remark.value = "";
    files.value = [];
  }, 800);
}
</script>

<template>
  <div class="data-submit-page">
    <!-- ═══════════════ 页面头部 ═══════════════ -->
    <header class="page-header">
      <h1 class="page-title">{{ t('dataSubmit.pageTitle') }}</h1>
      <p class="page-desc">{{ t('dataSubmit.pageDesc') }}</p>
    </header>

    <!-- ═══════════════ 主体布局 ═══════════════ -->
    <div class="submit-layout">
      <!-- 左侧：模板列表 -->
      <aside class="template-panel">
        <div class="panel-header">
          <q-icon name="sym_r_list_alt" size="18px" />
          <span>{{ t('dataSubmit.selectTemplate') }}</span>
        </div>
        <q-list class="template-list">
          <q-item
            v-for="tp in templates"
            :key="tp.id"
            clickable
            v-ripple
            active-class="template-item--active"
            :active="tp.id === activeTemplateId"
            class="template-item"
            @click="selectTemplate(tp.id)"
          >
            <q-item-section avatar class="template-item__avatar">
              <div class="template-icon" :style="{ backgroundColor: tp.color }">
                <q-icon :name="tp.icon" size="20px" color="white" />
              </div>
            </q-item-section>
            <q-item-section class="template-item__body">
              <q-item-label class="template-item__name" lines="2">{{ tp.name }}</q-item-label>
              <q-item-label class="template-item__desc" caption lines="1">
                {{ tp.description }}
              </q-item-label>
            </q-item-section>
            <q-item-section side class="template-item__indicator">
              <q-icon
                v-if="tp.id === activeTemplateId"
                name="sym_r_check_circle"
                size="18px"
                color="teal"
              />
            </q-item-section>
          </q-item>
        </q-list>
      </aside>

      <!-- 右侧：表单 -->
      <section class="form-panel">
        <div class="panel-header">
          <q-icon :name="activeTemplate.icon" size="18px" :style="{ color: activeTemplate.color }" />
          <span>{{ activeTemplate.name }}</span>
        </div>

        <q-form class="submit-form" @submit.prevent="submitForm">
          <!-- 基础信息 -->
          <div class="form-section">
            <h3 class="form-section__title">基础信息</h3>
            <div class="form-grid">
              <div class="form-field">
                <label class="field-label">
                  {{ t('dataSubmit.formName') }}
                  <span class="field-required">*</span>
                </label>
                <q-input
                  v-model="formName"
                  dense
                  outlined
                  :placeholder="activeTemplate.name"
                  class="field-control"
                />
              </div>

              <div class="form-field">
                <label class="field-label">{{ t('dataSubmit.formTemplate') }}</label>
                <q-select
                  v-model="activeTemplateId"
                  :options="templateOptions"
                  emit-value
                  map-options
                  dense
                  outlined
                  readonly
                  class="field-control"
                />
              </div>
            </div>
          </div>

          <!-- 填报数据 -->
          <div class="form-section">
            <h3 class="form-section__title">{{ t('dataSubmit.formData') }}</h3>
            <div class="form-grid">
              <div
                v-for="field in activeTemplate.fields"
                :key="field.key"
                class="form-field"
              >
                <label class="field-label">
                  {{ field.label }}
                  <span v-if="field.required" class="field-required">*</span>
                </label>

                <q-select
                  v-if="field.type === 'select'"
                  v-model="formData[field.key]"
                  :options="buildSelectOptions(field.options)"
                  emit-value
                  map-options
                  dense
                  outlined
                  :placeholder="field.placeholder ?? '请选择'"
                  class="field-control"
                />
                <q-input
                  v-else-if="field.type === 'number'"
                  v-model="formData[field.key]"
                  type="number"
                  dense
                  outlined
                  :placeholder="field.placeholder ?? '请输入'"
                  class="field-control"
                />
                <q-input
                  v-else-if="field.type === 'date'"
                  v-model="formData[field.key]"
                  dense
                  outlined
                  :placeholder="field.placeholder ?? '请选择日期'"
                  class="field-control"
                >
                  <template #prepend>
                    <q-icon name="sym_r_event" size="18px" class="cursor-pointer">
                      <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                        <q-date v-model="formData[field.key]" mask="YYYY-MM-DD" flat square>
                          <div class="row items-center justify-end">
                            <q-btn v-close-popup :label="t('common.confirm')" color="teal" flat no-caps dense />
                          </div>
                        </q-date>
                      </q-popup-proxy>
                    </q-icon>
                  </template>
                </q-input>
                <q-input
                  v-else
                  v-model="formData[field.key]"
                  dense
                  outlined
                  :placeholder="field.placeholder ?? '请输入'"
                  class="field-control"
                />
              </div>
            </div>
          </div>

          <!-- 附件上传 -->
          <div class="form-section">
            <h3 class="form-section__title">{{ t('dataSubmit.formAttachment') }}</h3>
            <q-file
              v-model="files"
              multiple
              dense
              outlined
              clearable
              accept=".xlsx,.xls,.csv,.doc,.docx,.pdf,.zip"
              placeholder="选择或拖拽文件到此处上传"
              class="file-control"
              @update:model-value="onFileSelected"
            >
              <template #prepend>
                <q-icon name="sym_r_cloud_upload" size="22px" />
              </template>
              <template #file="{ file }">
                <q-chip
                  dense
                  square
                  removable
                  class="file-chip"
                  @remove="files = files.filter((f: File) => f.name !== file.name)"
                >
                  <q-icon name="sym_r_description" size="14px" class="q-mr-xs" />
                  <span class="file-chip__name">{{ file.name }}</span>
                  <span class="file-chip__size">{{ (file.size / 1024).toFixed(1) }} KB</span>
                </q-chip>
              </template>
            </q-file>
            <p class="file-hint">支持 xlsx / csv / doc / pdf / zip 等格式，单个文件不超过 50MB</p>
          </div>

          <!-- 备注 -->
          <div class="form-section">
            <h3 class="form-section__title">{{ t('dataSubmit.formRemark') }}</h3>
            <q-input
              v-model="remark"
              type="textarea"
              dense
              outlined
              autogrow
              :placeholder="'请填写需要补充说明的内容，便于审核人员了解填报背景...'"
              :input-style="{ minHeight: '80px' }"
              class="remark-control"
            />
          </div>

          <!-- 操作按钮 -->
          <div class="form-actions">
            <q-btn
              outline
              no-caps
              dense
              icon="sym_r_save"
              :label="t('dataSubmit.saveDraft')"
              color="teal"
              class="action-btn action-btn--draft"
              :loading="submitting"
              @click="saveDraft"
            />
            <q-btn
              unelevated
              no-caps
              dense
              icon="sym_r_send"
              :label="t('dataSubmit.submit')"
              color="teal"
              class="action-btn action-btn--submit"
              type="submit"
              :loading="submitting"
            />
          </div>
        </q-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.data-submit-page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

/* ═══════════════ 页面头部 ═══════════════ */
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 6px 0;
  line-height: 1.4;
}

.body--dark .page-title {
  color: rgba(255, 255, 255, 0.92);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin: 0;
  line-height: 1.5;
}

.body--dark .page-desc {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══════════════ 主体布局 ═══════════════ */
.submit-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  align-items: start;
}

/* ═══════════════ 模板面板 ═══════════════ */
.template-panel {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 24px;
}

.body--dark .template-panel {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .panel-header {
  color: rgba(255, 255, 255, 0.92);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.template-list {
  padding: 8px;
}

.template-item {
  padding: 12px 12px;
  min-height: 64px;
  border-left: 3px solid transparent;
  transition: all 0.2s ease;
}

.template-item:hover {
  background: rgba(0, 150, 136, 0.04);
  border-left-color: rgba(0, 150, 136, 0.4);
}

.body--dark .template-item:hover {
  background: rgba(77, 182, 172, 0.06);
}

.template-item--active {
  background: rgba(0, 150, 136, 0.06);
  border-left-color: #009688;
}

.body--dark .template-item--active {
  background: rgba(77, 182, 172, 0.08);
  border-left-color: #4db6ac;
}

.template-item__avatar {
  padding-right: 12px;
  min-width: 44px;
}

.template-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.template-item__body {
  min-width: 0;
}

.template-item__name {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.8);
  line-height: 1.4;
  margin-bottom: 2px;
}

.body--dark .template-item__name {
  color: rgba(255, 255, 255, 0.8);
}

.template-item--active .template-item__name {
  color: #009688;
  font-weight: 600;
}

.body--dark .template-item--active .template-item__name {
  color: #4db6ac;
}

.template-item__desc {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.45);
}

.body--dark .template-item__desc {
  color: rgba(255, 255, 255, 0.45);
}

.template-item__indicator {
  justify-content: flex-end;
}

/* ═══════════════ 表单面板 ═══════════════ */
.form-panel {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .form-panel {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.submit-form {
  padding: 24px 28px;
}

.form-section {
  margin-bottom: 28px;
}

.form-section__title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 16px 0;
  padding-left: 10px;
  border-left: 3px solid #009688;
  line-height: 1.2;
}

.body--dark .form-section__title {
  color: rgba(255, 255, 255, 0.92);
  border-left-color: #4db6ac;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.field-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.7);
  margin-bottom: 6px;
  font-weight: 500;
  line-height: 1.4;
}

.body--dark .field-label {
  color: rgba(255, 255, 255, 0.7);
}

.field-required {
  color: #e53935;
  margin-left: 2px;
}

.field-control {
  width: 100%;
}

/* 让 outlined 输入框聚焦时使用主色 */
.field-control :deep(.q-field--outlined.q-field--focused .q-field__control) {
  border-color: #009688 !important;
}

.body--dark .field-control :deep(.q-field--outlined.q-field--focused .q-field__control) {
  border-color: #4db6ac !important;
}

/* —— 文件上传 —— */
.file-control {
  width: 100%;
}

.file-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.4);
  margin: 8px 0 0 0;
  line-height: 1.5;
}

.body--dark .file-hint {
  color: rgba(255, 255, 255, 0.4);
}

.file-chip {
  background: rgba(0, 150, 136, 0.08) !important;
  color: rgba(0, 0, 0, 0.75) !important;
  font-size: 12px;
  margin: 4px 4px 0 0;
}

.body--dark .file-chip {
  background: rgba(77, 182, 172, 0.12) !important;
  color: rgba(255, 255, 255, 0.75) !important;
}

.file-chip__name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.file-chip__size {
  color: rgba(0, 0, 0, 0.45);
  margin-left: 6px;
  font-family: "JetBrains Mono", monospace;
  font-size: 11px;
}

.body--dark .file-chip__size {
  color: rgba(255, 255, 255, 0.45);
}

/* —— 备注 —— */
.remark-control {
  width: 100%;
}

/* —— 操作按钮 —— */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .form-actions {
  border-top-color: rgba(255, 255, 255, 0.06);
}

.action-btn {
  font-size: 14px;
  font-weight: 500;
  min-height: 38px;
  padding: 0 24px;
  min-width: 120px;
}

.action-btn--draft {
  font-weight: 500;
}

.action-btn--submit {
  font-weight: 600;
}

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 1024px) {
  .submit-layout {
    grid-template-columns: 1fr;
  }
  .template-panel {
    position: static;
  }
  .template-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 4px;
  }
}

@media (max-width: 768px) {
  .data-submit-page {
    padding: 16px;
  }
  .submit-form {
    padding: 20px 16px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .template-list {
    grid-template-columns: 1fr;
  }
  .form-actions {
    flex-direction: column;
  }
  .action-btn {
    width: 100%;
  }
}
</style>
