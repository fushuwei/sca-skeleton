<script setup lang="ts">
import { watch, onBeforeUnmount, computed, ref } from "vue";
import { useEditor, EditorContent } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Link from "@tiptap/extension-link";
import Image from "@tiptap/extension-image";
import TextAlign from "@tiptap/extension-text-align";
import Placeholder from "@tiptap/extension-placeholder";
import Underline from "@tiptap/extension-underline";
import { TextStyle } from "@tiptap/extension-text-style";
import Color from "@tiptap/extension-color";
import Highlight from "@tiptap/extension-highlight";
import { Table } from "@tiptap/extension-table";
import TableRow from "@tiptap/extension-table-row";
import TableCell from "@tiptap/extension-table-cell";
import TableHeader from "@tiptap/extension-table-header";
import { useI18n } from "vue-i18n";

defineOptions({
  name: "RichTextEditor"
});

const { t } = useI18n({ useScope: "global" });

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    disable?: boolean;
    readonly?: boolean;
  }>(),
  {
    modelValue: "",
    placeholder: "",
    disable: false,
    readonly: false
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: string];
}>();

const isDisabled = computed(() => props.disable || props.readonly);

const editor = useEditor({
  content: props.modelValue || "",
  editable: !isDisabled.value,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] }
    }),
    Underline,
    TextStyle,
    Color,
    Highlight.configure({ multicolor: true }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: { rel: "noopener noreferrer", target: "_blank" }
    }),
    Image.configure({ inline: false }),
    TextAlign.configure({ types: ["heading", "paragraph"] }),
    Placeholder.configure({
      placeholder: props.placeholder || t("richText.placeholder")
    }),
    Table.configure({ resizable: false }),
    TableRow,
    TableCell,
    TableHeader
  ],
  onUpdate({ editor }) {
    emit("update:modelValue", editor.getHTML());
  }
});

// 外部 modelValue 变化时同步到编辑器
watch(
  () => props.modelValue,
  (val) => {
    if (!editor.value) return;
    const current = editor.value.getHTML();
    if (val !== current) {
      editor.value.commands.setContent(val || "", { emitUpdate: false });
    }
  }
);

watch(
  isDisabled,
  (val) => {
    editor.value?.setEditable(!val);
  }
);

onBeforeUnmount(() => {
  editor.value?.destroy();
});

// ── 颜色选择器弹出状态 ──
const textColorPopup = ref(false);
const highlightColorPopup = ref(false);

// ── 预设颜色列表 ──
const presetColors = [
  "#000000", "#434343", "#666666", "#999999", "#b7b7b7", "#cccccc", "#d9d9d9", "#efefef", "#f3f3f3", "#ffffff",
  "#980000", "#ff0000", "#ff9900", "#ffff00", "#00ff00", "#00ffff", "#4a86e8", "#0000ff", "#9900ff", "#ff00ff",
  "#e6b8af", "#f4cccc", "#fce5cd", "#fff2cc", "#d9ead3", "#d0e0e3", "#c9daf8", "#cfe2f3", "#d9d2e9", "#ead1dc",
  "#cc4125", "#dd7e6b", "#e69138", "#f1c232", "#6aa84f", "#45818e", "#3d85c6", "#674ea7", "#a64d79", "#85929e"
];

// ── 自定义颜色输入值 ──
const customTextColor = ref("");
const customHighlightColor = ref("");

// ── 工具栏命令 ──
function toggleBold() { editor.value?.chain().focus().toggleBold().run(); }
function toggleItalic() { editor.value?.chain().focus().toggleItalic().run(); }
function toggleUnderline() { editor.value?.chain().focus().toggleUnderline().run(); }
function toggleStrike() { editor.value?.chain().focus().toggleStrike().run(); }
function toggleHeading(level: 1 | 2 | 3) { editor.value?.chain().focus().toggleHeading({ level }).run(); }
function toggleBulletList() { editor.value?.chain().focus().toggleBulletList().run(); }
function toggleOrderedList() { editor.value?.chain().focus().toggleOrderedList().run(); }
function toggleBlockquote() { editor.value?.chain().focus().toggleBlockquote().run(); }
function toggleCodeBlock() { editor.value?.chain().focus().toggleCodeBlock().run(); }
function setTextAlign(align: "left" | "center" | "right") { editor.value?.chain().focus().setTextAlign(align).run(); }
function toggleLink() {
  const href = window.prompt(t("richText.linkPrompt"));
  if (href === null) return;
  if (href === "") {
    editor.value?.chain().focus().unsetLink().run();
  } else {
    editor.value?.chain().focus().setLink({ href }).run();
  }
}
function insertImage() {
  const src = window.prompt(t("richText.imagePrompt"));
  if (src) {
    editor.value?.chain().focus().setImage({ src }).run();
  }
}
function undo() { editor.value?.chain().focus().undo().run(); }
function redo() { editor.value?.chain().focus().redo().run(); }

// ── 颜色命令 ──
function setTextColor(color: string) {
  editor.value?.chain().focus().setColor(color).run();
  textColorPopup.value = false;
}
function unsetTextColor() {
  editor.value?.chain().focus().unsetColor().run();
  textColorPopup.value = false;
}
function setHighlight(color: string) {
  editor.value?.chain().focus().toggleHighlight({ color }).run();
  highlightColorPopup.value = false;
}
function unsetHighlight() {
  editor.value?.chain().focus().unsetHighlight().run();
  highlightColorPopup.value = false;
}

// ── 表格命令 ──
function insertTable() {
  const rowsStr = window.prompt(t("richText.tableRowsPrompt"), "3");
  if (!rowsStr) return;
  const colsStr = window.prompt(t("richText.tableColsPrompt"), "3");
  if (!colsStr) return;
  const rows = Math.max(1, parseInt(rowsStr, 10) || 1);
  const cols = Math.max(1, parseInt(colsStr, 10) || 1);
  editor.value?.chain().focus().insertTable({ rows, cols, withHeaderRow: true }).run();
}
function addColumnBefore() { editor.value?.chain().focus().addColumnBefore().run(); }
function addColumnAfter() { editor.value?.chain().focus().addColumnAfter().run(); }
function deleteColumn() { editor.value?.chain().focus().deleteColumn().run(); }
function addRowBefore() { editor.value?.chain().focus().addRowBefore().run(); }
function addRowAfter() { editor.value?.chain().focus().addRowAfter().run(); }
function deleteRow() { editor.value?.chain().focus().deleteRow().run(); }
function deleteTable() { editor.value?.chain().focus().deleteTable().run(); }
function toggleHeaderCell() { editor.value?.chain().focus().toggleHeaderCell().run(); }
function mergeCells() { editor.value?.chain().focus().mergeCells().run(); }
function splitCell() { editor.value?.chain().focus().splitCell().run(); }

// ── 活跃状态检测 ──
const isActive = computed(() => ({
  bold: editor.value?.isActive("bold") ?? false,
  italic: editor.value?.isActive("italic") ?? false,
  underline: editor.value?.isActive("underline") ?? false,
  strike: editor.value?.isActive("strike") ?? false,
  h1: editor.value?.isActive("heading", { level: 1 }) ?? false,
  h2: editor.value?.isActive("heading", { level: 2 }) ?? false,
  h3: editor.value?.isActive("heading", { level: 3 }) ?? false,
  bulletList: editor.value?.isActive("bulletList") ?? false,
  orderedList: editor.value?.isActive("orderedList") ?? false,
  blockquote: editor.value?.isActive("blockquote") ?? false,
  codeBlock: editor.value?.isActive("codeBlock") ?? false,
  link: editor.value?.isActive("link") ?? false,
  alignLeft: editor.value?.isActive({ textAlign: "left" }) ?? false,
  alignCenter: editor.value?.isActive({ textAlign: "center" }) ?? false,
  alignRight: editor.value?.isActive({ textAlign: "right" }) ?? false,
  table: editor.value?.isActive("table") ?? false
}));

const canUndo = computed(() => editor.value?.can().undo() ?? false);
const canRedo = computed(() => editor.value?.can().redo() ?? false);
</script>

<template>
  <div class="rich-text-editor" :class="{ 'is-disabled': isDisabled }">
    <!-- 工具栏 -->
    <div v-if="!isDisabled" class="rte-toolbar">
      <q-btn flat dense round size="xs" icon="sym_r_format_bold" :color="isActive.bold ? 'primary' : 'grey-7'" @click="toggleBold">
        <q-tooltip>{{ t('richText.bold') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_italic" :color="isActive.italic ? 'primary' : 'grey-7'" @click="toggleItalic">
        <q-tooltip>{{ t('richText.italic') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_underlined" :color="isActive.underline ? 'primary' : 'grey-7'" @click="toggleUnderline">
        <q-tooltip>{{ t('richText.underline') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_strikethrough_s" :color="isActive.strike ? 'primary' : 'grey-7'" @click="toggleStrike">
        <q-tooltip>{{ t('richText.strikethrough') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_format_h1" :color="isActive.h1 ? 'primary' : 'grey-7'" @click="toggleHeading(1)">
        <q-tooltip>{{ t('richText.heading1') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_h2" :color="isActive.h2 ? 'primary' : 'grey-7'" @click="toggleHeading(2)">
        <q-tooltip>{{ t('richText.heading2') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_h3" :color="isActive.h3 ? 'primary' : 'grey-7'" @click="toggleHeading(3)">
        <q-tooltip>{{ t('richText.heading3') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_format_list_bulleted" :color="isActive.bulletList ? 'primary' : 'grey-7'" @click="toggleBulletList">
        <q-tooltip>{{ t('richText.bulletList') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_list_numbered" :color="isActive.orderedList ? 'primary' : 'grey-7'" @click="toggleOrderedList">
        <q-tooltip>{{ t('richText.orderedList') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_quote" :color="isActive.blockquote ? 'primary' : 'grey-7'" @click="toggleBlockquote">
        <q-tooltip>{{ t('richText.quote') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_code_blocks" :color="isActive.codeBlock ? 'primary' : 'grey-7'" @click="toggleCodeBlock">
        <q-tooltip>{{ t('richText.codeBlock') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <!-- 字体颜色 -->
      <q-btn flat dense round size="xs" color="grey-7" @click="textColorPopup = !textColorPopup">
        <q-icon name="sym_r_format_color_text" size="18px" />
        <q-tooltip>{{ t('richText.textColor') }}</q-tooltip>
      </q-btn>
      <!-- 背景色 -->
      <q-btn flat dense round size="xs" color="grey-7" @click="highlightColorPopup = !highlightColorPopup">
        <q-icon name="sym_r_format_color_fill" size="18px" />
        <q-tooltip>{{ t('richText.highlightColor') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_format_align_left" :color="isActive.alignLeft ? 'primary' : 'grey-7'" @click="setTextAlign('left')">
        <q-tooltip>{{ t('richText.alignLeft') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_align_center" :color="isActive.alignCenter ? 'primary' : 'grey-7'" @click="setTextAlign('center')">
        <q-tooltip>{{ t('richText.alignCenter') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_format_align_right" :color="isActive.alignRight ? 'primary' : 'grey-7'" @click="setTextAlign('right')">
        <q-tooltip>{{ t('richText.alignRight') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_link" :color="isActive.link ? 'primary' : 'grey-7'" @click="toggleLink">
        <q-tooltip>{{ t('richText.link') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_image" color="grey-7" @click="insertImage">
        <q-tooltip>{{ t('richText.image') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_table" color="grey-7" @click="insertTable">
        <q-tooltip>{{ t('richText.table') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_undo" :disable="!canUndo" color="grey-7" @click="undo">
        <q-tooltip>{{ t('richText.undo') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_redo" :disable="!canRedo" color="grey-7" @click="redo">
        <q-tooltip>{{ t('richText.redo') }}</q-tooltip>
      </q-btn>
    </div>

    <!-- 颜色选择弹出面板 -->
    <div v-if="textColorPopup" class="rte-color-popup">
      <div class="rte-color-grid">
        <button
          v-for="color in presetColors"
          :key="'text-' + color"
          class="rte-color-swatch"
          :style="{ background: color }"
          @click="setTextColor(color)"
        />
      </div>
      <div class="rte-color-actions">
        <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_format_clear" :label="t('richText.clearColor')" @click="unsetTextColor" />
        <q-input
          v-model="customTextColor"
          dense
          filled
          square
          :label="t('richText.customColor')"
          class="rte-color-input"
          @keyup.enter="setTextColor(customTextColor); customTextColor = ''"
        >
          <template #append>
            <q-icon name="sym_r_check" class="cursor-pointer" @click="setTextColor(customTextColor); customTextColor = ''" />
          </template>
        </q-input>
      </div>
    </div>

    <div v-if="highlightColorPopup" class="rte-color-popup">
      <div class="rte-color-grid">
        <button
          v-for="color in presetColors"
          :key="'hl-' + color"
          class="rte-color-swatch"
          :style="{ background: color }"
          @click="setHighlight(color)"
        />
      </div>
      <div class="rte-color-actions">
        <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_format_clear" :label="t('richText.clearColor')" @click="unsetHighlight" />
        <q-input
          v-model="customHighlightColor"
          dense
          filled
          square
          :label="t('richText.customColor')"
          class="rte-color-input"
          @keyup.enter="setHighlight(customHighlightColor); customHighlightColor = ''"
        >
          <template #append>
            <q-icon name="sym_r_check" class="cursor-pointer" @click="setHighlight(customHighlightColor); customHighlightColor = ''" />
          </template>
        </q-input>
      </div>
    </div>

    <!-- 表格操作栏（仅在光标位于表格内时显示） -->
    <div v-if="!isDisabled && isActive.table" class="rte-table-toolbar">
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_add_column_left" :label="t('richText.addColumnBefore')" @click="addColumnBefore" />
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_add_column_right" :label="t('richText.addColumnAfter')" @click="addColumnAfter" />
      <q-btn flat dense no-caps size="sm" color="negative" icon="sym_r_delete_column" :label="t('richText.deleteColumn')" @click="deleteColumn" />
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_add_row_above" :label="t('richText.addRowBefore')" @click="addRowBefore" />
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_add_row_below" :label="t('richText.addRowAfter')" @click="addRowAfter" />
      <q-btn flat dense no-caps size="sm" color="negative" icon="sym_r_delete_row" :label="t('richText.deleteRow')" @click="deleteRow" />
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_call_merge" :label="t('richText.mergeCells')" @click="mergeCells" />
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_split" :label="t('richText.splitCell')" @click="splitCell" />
      <q-btn flat dense no-caps size="sm" color="grey-7" icon="sym_r_table_chart" :label="t('richText.toggleHeaderCell')" @click="toggleHeaderCell" />
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense no-caps size="sm" color="negative" icon="sym_r_delete" :label="t('richText.deleteTable')" @click="deleteTable" />
    </div>

    <!-- 编辑区 -->
    <EditorContent :editor="editor" class="rte-content" />
  </div>
</template>


<style scoped>
.rich-text-editor {
  border: 1px solid rgba(0, 0, 0, 0.15);
  background: #fff;
  border-radius: 0;
  overflow: hidden;
  position: relative;
}

.rich-text-editor.is-disabled {
  opacity: 0.75;
  pointer-events: none;
}

/* 工具栏 */
.rte-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1px;
  padding: 4px 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: #fafafa;
}

.rte-toolbar .q-btn {
  width: 28px;
  height: 28px;
  min-width: 28px;
  min-height: 28px;
}

.rte-toolbar .q-btn .q-icon {
  font-size: 18px;
}

.rte-separator {
  height: 18px;
  margin: 0 2px;
  align-self: center;
}

/* 颜色弹出面板 */
.rte-color-popup {
  position: absolute;
  z-index: 10;
  top: 36px;
  left: 6px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  padding: 8px;
  border-radius: 4px;
}

.rte-color-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 3px;
  margin-bottom: 8px;
}

.rte-color-swatch {
  width: 18px;
  height: 18px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 2px;
  cursor: pointer;
  padding: 0;
}

.rte-color-swatch:hover {
  border-color: #009688;
  transform: scale(1.15);
}

.rte-color-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rte-color-input {
  width: 120px;
}

/* 表格操作栏 */
.rte-table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: #f5f5f5;
}

.rte-table-toolbar .q-btn {
  height: 28px;
  font-size: 11px;
  padding: 0 6px;
}

.rte-table-toolbar .q-btn .q-icon {
  font-size: 16px;
}

/* 编辑区 */
.rte-content {
  min-height: 200px;
  max-height: 500px;
  overflow-y: auto;
}

/* Tiptap 编辑器内容区样式 */
.rte-content :deep(.tiptap) {
  padding: 12px 16px;
  min-height: 200px;
  outline: none;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.87);
}

.rte-content :deep(.tiptap p) {
  margin: 0 0 0.5em;
}

.rte-content :deep(.tiptap h1) {
  font-size: 1.6em;
  font-weight: 700;
  margin: 0.8em 0 0.4em;
}

.rte-content :deep(.tiptap h2) {
  font-size: 1.4em;
  font-weight: 700;
  margin: 0.7em 0 0.4em;
}

.rte-content :deep(.tiptap h3) {
  font-size: 1.2em;
  font-weight: 600;
  margin: 0.6em 0 0.3em;
}

.rte-content :deep(.tiptap ul),
.rte-content :deep(.tiptap ol) {
  padding-left: 1.5em;
  margin: 0 0 0.5em;
}

.rte-content :deep(.tiptap ul) { list-style: disc; }
.rte-content :deep(.tiptap ol) { list-style: decimal; }

.rte-content :deep(.tiptap blockquote) {
  border-left: 3px solid rgba(0, 0, 0, 0.15);
  padding-left: 1em;
  margin: 0 0 0.5em;
  color: rgba(0, 0, 0, 0.6);
  font-style: italic;
}

.rte-content :deep(.tiptap pre) {
  background: #f5f5f5;
  border-radius: 4px;
  padding: 0.75em 1em;
  margin: 0 0 0.5em;
  font-family: "JetBrains Mono", "Fira Code", "Consolas", monospace;
  font-size: 0.9em;
  overflow-x: auto;
}

.rte-content :deep(.tiptap pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.rte-content :deep(.tiptap code) {
  background: rgba(0, 0, 0, 0.06);
  border-radius: 3px;
  padding: 2px 5px;
  font-family: "JetBrains Mono", "Fira Code", "Consolas", monospace;
  font-size: 0.9em;
}

.rte-content :deep(.tiptap a) {
  color: #009688;
  text-decoration: underline;
  cursor: pointer;
}

.rte-content :deep(.tiptap img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 0.5em 0;
}

.rte-content :deep(.tiptap img.ProseMirror-selectednode) {
  outline: 2px solid #009688;
}

/* 表格样式 */
.rte-content :deep(.tiptap table) {
  border-collapse: collapse;
  table-layout: fixed;
  width: 100%;
  margin: 0 0 0.5em;
  overflow: hidden;
}

.rte-content :deep(.tiptap td),
.rte-content :deep(.tiptap th) {
  border: 1px solid rgba(0, 0, 0, 0.12);
  padding: 6px 10px;
  vertical-align: top;
  min-width: 60px;
}

.rte-content :deep(.tiptap th) {
  background: #f5f5f5;
  font-weight: 600;
  text-align: left;
}

.rte-content :deep(.tiptap .selectedCell) {
  background: rgba(0, 150, 136, 0.08);
}

.rte-content :deep(.tiptap .column-resize-handle) {
  position: absolute;
  right: -2px;
  top: 0;
  bottom: -2px;
  width: 4px;
  background: rgba(0, 150, 136, 0.3);
  pointer-events: none;
}

.rte-content :deep(.tiptap.resize-cursor) {
  cursor: ew-resize;
}

/* Placeholder 样式 */
.rte-content :deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: rgba(0, 0, 0, 0.35);
  pointer-events: none;
  height: 0;
}
</style>

<!-- 非 scoped：暗黑模式样式 -->
<style>
/* 暗黑模式 — 编辑器外壳 */
.body--dark .rich-text-editor {
  background: #2d2d2d;
  border-color: rgba(255, 255, 255, 0.22);
}

/* 暗黑模式 — 工具栏 */
.body--dark .rte-toolbar {
  background: #252525;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.body--dark .rte-toolbar .q-btn {
  color: rgba(255, 255, 255, 0.55) !important;
}

.body--dark .rte-toolbar .q-btn.text-primary {
  color: #80cbc4 !important;
}

/* 暗黑模式 — 颜色弹出面板 */
.body--dark .rte-color-popup {
  background: #2d2d2d;
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.body--dark .rte-color-popup .q-field--filled .q-field__control {
  background: #383838 !important;
}

.body--dark .rte-color-popup .q-field__native,
.body--dark .rte-color-popup .q-field__label {
  color: rgba(255, 255, 255, 0.87) !important;
}

/* 暗黑模式 — 表格操作栏 */
.body--dark .rte-table-toolbar {
  background: #252525;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.body--dark .rte-table-toolbar .q-btn {
  color: rgba(255, 255, 255, 0.55) !important;
}

/* 暗黑模式 — 编辑区内容 */
.body--dark .rte-content .tiptap {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .rte-content .tiptap blockquote {
  border-left-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.6);
}

.body--dark .rte-content .tiptap pre {
  background: #1e1e1e;
}

.body--dark .rte-content .tiptap code {
  background: rgba(255, 255, 255, 0.08);
}

.body--dark .rte-content .tiptap a {
  color: #80cbc4;
}

.body--dark .rte-content .tiptap th {
  background: #333;
}

.body--dark .rte-content .tiptap td,
.body--dark .rte-content .tiptap th {
  border-color: rgba(255, 255, 255, 0.1);
}

.body--dark .rte-content .tiptap .selectedCell {
  background: rgba(0, 200, 180, 0.12);
}

.body--dark .rte-content .tiptap p.is-editor-empty:first-child::before {
  color: rgba(255, 255, 255, 0.35);
}
</style>
