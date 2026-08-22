<script setup lang="ts">
import { watch, onBeforeUnmount, computed, ref, nextTick } from "vue";
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

// ── 通用弹出框管理 ──
type PopupType = "textColor" | "highlightColor" | "link" | "image" | "table" | null;
const activePopup = ref<PopupType>(null);
const popupStyle = ref<Record<string, string>>({});

// 每个触发按钮的 ref
const popupBtnRefs: Record<string, { $el: HTMLElement } | null> = {
  textColor: null,
  highlightColor: null,
  link: null,
  image: null,
  table: null
};

function setBtnRef(key: string, el: any) {
  popupBtnRefs[key] = el;
}

// ── 保存/恢复编辑器选区（弹出框打开时编辑器可能丢失焦点） ──
let savedSelection: { from: number; to: number } | null = null;

function saveSelection() {
  if (!editor.value) return;
  const { from, to } = editor.value.state.selection;
  savedSelection = { from, to };
}

function restoreSelection() {
  if (!editor.value || !savedSelection) return;
  const { from, to } = savedSelection;
  editor.value.commands.setTextSelection({ from, to });
  editor.value.commands.focus();
  savedSelection = null;
}

// ── 计算弹出框位置（自适应浏览器边界） ──
function computePopupPosition(btnEl: HTMLElement | null, popupWidth: number = 280, popupHeight: number = 200): Record<string, string> {
  if (!btnEl) return { display: "none" };
  const rect = btnEl.getBoundingClientRect();
  const editorEl = btnEl.closest(".rich-text-editor");
  if (!editorEl) return { display: "none" };
  const editorRect = editorEl.getBoundingClientRect();
  let left = rect.left - editorRect.left;
  const top = rect.bottom - editorRect.top + 2;
  // 自适应右边边界：如果弹出框超出浏览器右侧，则左移
  const viewportWidth = window.innerWidth;
  const absoluteRight = rect.left + popupWidth;
  if (absoluteRight > viewportWidth - 8) {
    left = Math.max(0, viewportWidth - 8 - popupWidth - editorRect.left);
  }
  // 自适应下边边界：如果弹出框超出浏览器底部，则显示在按钮上方
  const absoluteBottom = rect.bottom + popupHeight;
  let finalTop = top;
  if (absoluteBottom > window.innerHeight - 8) {
    finalTop = rect.top - editorRect.top - popupHeight - 2;
  }
  return {
    position: "absolute",
    left: `${left}px`,
    top: `${finalTop}px`,
    zIndex: "100"
  };
}

// ── 打开/关闭弹出框 ──
function togglePopup(type: Exclude<PopupType, null>) {
  if (activePopup.value === type) {
    closePopup();
    return;
  }
  // 打开弹出框前保存编辑器选区
  saveSelection();
  activePopup.value = type;
  nextTick(() => {
    const btnEl = popupBtnRefs[type]?.$el ?? null;
    // 根据弹出框类型估算尺寸
    const isColorPopup = type === "textColor" || type === "highlightColor";
    const popupW = isColorPopup ? 220 : 300;
    const popupH = isColorPopup ? 180 : 120;
    popupStyle.value = computePopupPosition(btnEl, popupW, popupH);
  });
}

function closePopup() {
  activePopup.value = null;
}

// ── 点击外部关闭弹出框 ──
function handleClickOutside(e: MouseEvent) {
  if (!activePopup.value) return;
  const target = e.target as Node;
  const popupEl = document.querySelector(".rte-color-popup, .rte-input-popup");
  const btnEl = popupBtnRefs[activePopup.value]?.$el as Node | undefined;
  if (popupEl && !popupEl.contains(target) && (!btnEl || !btnEl.contains(target))) {
    closePopup();
  }
}

watch(activePopup, (val) => {
  if (val) {
    document.addEventListener("mousedown", handleClickOutside);
  } else {
    document.removeEventListener("mousedown", handleClickOutside);
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleClickOutside);
});

// ── 预设颜色列表 ──
const presetColors = [
  "#000000", "#434343", "#666666", "#999999", "#b7b7b7", "#cccccc", "#d9d9d9", "#efefef", "#f3f3f3", "#ffffff",
  "#980000", "#ff0000", "#ff9900", "#ffff00", "#00ff00", "#00ffff", "#4a86e8", "#0000ff", "#9900ff", "#ff00ff",
  "#e6b8af", "#f4cccc", "#fce5cd", "#fff2cc", "#d9ead3", "#d0e0e3", "#c9daf8", "#cfe2f3", "#d9d2e9", "#ead1dc",
  "#cc4125", "#dd7e6b", "#e69138", "#f1c232", "#6aa84f", "#45818e", "#3d85c6", "#674ea7", "#a64d79", "#85929e"
];

// ── 颜色输入值 ──
const customTextColor = ref("");
const customHighlightColor = ref("");

// ── 链接/图片/表格输入值 ──
const linkUrl = ref("");
const imageUrl = ref("");
const tableRows = ref(3);
const tableCols = ref(3);

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

// ── 链接弹出框 ──
function openLinkPopup() {
  const currentHref = (editor.value?.getAttributes("link") as { href?: string })?.href || "";
  linkUrl.value = currentHref;
  togglePopup("link");
}

function applyLink() {
  const url = linkUrl.value.trim();
  restoreSelection();
  if (!url) {
    editor.value?.chain().focus().unsetLink().run();
  } else {
    editor.value?.chain().focus().setLink({ href: url }).run();
  }
  closePopup();
  linkUrl.value = "";
}

// ── 图片弹出框 ──
function applyImage() {
  const src = imageUrl.value.trim();
  restoreSelection();
  if (src) {
    editor.value?.chain().focus().setImage({ src }).run();
  }
  closePopup();
  imageUrl.value = "";
}

// ── 表格弹出框 ──
function applyTable() {
  const rows = Math.max(1, tableRows.value || 1);
  const cols = Math.max(1, tableCols.value || 1);
  restoreSelection();
  editor.value?.chain().focus().insertTable({ rows, cols, withHeaderRow: true }).run();
  closePopup();
  tableRows.value = 3;
  tableCols.value = 3;
}

// ── 颜色命令 ──
function applyTextColor() {
  const color = customTextColor.value.trim();
  if (!color) return;
  restoreSelection();
  editor.value?.chain().focus().setColor(color).run();
  closePopup();
  customTextColor.value = "";
}
function setTextColor(color: string) {
  restoreSelection();
  editor.value?.chain().focus().setColor(color).run();
  closePopup();
}
function unsetTextColor() {
  restoreSelection();
  editor.value?.chain().focus().unsetColor().run();
  closePopup();
}
function applyHighlightColor() {
  const color = customHighlightColor.value.trim();
  if (!color) return;
  restoreSelection();
  editor.value?.chain().focus().setHighlight({ color }).run();
  closePopup();
  customHighlightColor.value = "";
}
function setHighlight(color: string) {
  restoreSelection();
  editor.value?.chain().focus().setHighlight({ color }).run();
  closePopup();
}
function unsetHighlight() {
  restoreSelection();
  editor.value?.chain().focus().unsetHighlight().run();
  closePopup();
}

// ── 表格命令 ──
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
      <q-btn :ref="(el) => setBtnRef('textColor', el)" flat dense round size="xs" icon="sym_r_format_color_text" :color="activePopup === 'textColor' ? 'primary' : 'grey-7'" @click="togglePopup('textColor')">
        <q-tooltip>{{ t('richText.textColor') }}</q-tooltip>
      </q-btn>
      <q-btn :ref="(el) => setBtnRef('highlightColor', el)" flat dense round size="xs" icon="sym_r_format_color_fill" :color="activePopup === 'highlightColor' ? 'primary' : 'grey-7'" @click="togglePopup('highlightColor')">
        <q-tooltip>{{ t('richText.highlightColor') }}</q-tooltip>
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
      <q-btn :ref="(el) => setBtnRef('link', el)" flat dense round size="xs" icon="sym_r_link" :color="isActive.link ? 'primary' : 'grey-7'" @click="openLinkPopup">
        <q-tooltip>{{ t('richText.link') }}</q-tooltip>
      </q-btn>
      <q-btn :ref="(el) => setBtnRef('image', el)" flat dense round size="xs" icon="sym_r_image" color="grey-7" @click="togglePopup('image')">
        <q-tooltip>{{ t('richText.image') }}</q-tooltip>
      </q-btn>
      <q-btn :ref="(el) => setBtnRef('table', el)" flat dense round size="xs" icon="sym_r_table" color="grey-7" @click="togglePopup('table')">
        <q-tooltip>{{ t('richText.table') }}</q-tooltip>
      </q-btn>
    </div>

    <!-- 字体颜色选择弹出面板 -->
    <div v-if="activePopup === 'textColor'" class="rte-color-popup rte-input-popup" :style="popupStyle">
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
        <q-input
          v-model="customTextColor"
          dense
          filled
          square
          :label="t('richText.customColor')"
          class="rte-color-input"
          @keyup.enter="applyTextColor"
        />
        <q-btn flat dense round icon="sym_r_format_color_reset" class="rte-clear-color-btn" @click="unsetTextColor">
          <q-tooltip>{{ t('richText.clearColor') }}</q-tooltip>
        </q-btn>
      </div>
    </div>

    <!-- 背景颜色选择弹出面板 -->
    <div v-if="activePopup === 'highlightColor'" class="rte-color-popup rte-input-popup" :style="popupStyle">
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
        <q-input
          v-model="customHighlightColor"
          dense
          filled
          square
          :label="t('richText.customColor')"
          class="rte-color-input"
          @keyup.enter="applyHighlightColor"
        />
        <q-btn flat dense round icon="sym_r_format_color_reset" class="rte-clear-color-btn" @click="unsetHighlight">
          <q-tooltip>{{ t('richText.clearColor') }}</q-tooltip>
        </q-btn>
      </div>
    </div>

    <!-- 链接弹出面板 -->
    <div v-if="activePopup === 'link'" class="rte-input-popup rte-link-popup" :style="popupStyle">
      <q-input
        v-model="linkUrl"
        dense
        filled
        square
        :label="t('richText.linkPrompt')"
        autofocus
        @keyup.enter="applyLink"
      />
      <div class="rte-popup-actions">
        <q-btn flat no-caps color="grey-7" :label="t('common.cancel')" @click="closePopup" />
        <q-btn unelevated no-caps color="primary" :label="t('common.confirm')" @click="applyLink" />
      </div>
    </div>

    <!-- 图片弹出面板 -->
    <div v-if="activePopup === 'image'" class="rte-input-popup rte-image-popup" :style="popupStyle">
      <q-input
        v-model="imageUrl"
        dense
        filled
        square
        :label="t('richText.imagePrompt')"
        autofocus
        @keyup.enter="applyImage"
      />
      <div class="rte-popup-actions">
        <q-btn flat no-caps color="grey-7" :label="t('common.cancel')" @click="closePopup" />
        <q-btn unelevated no-caps color="primary" :label="t('common.confirm')" @click="applyImage" />
      </div>
    </div>

    <!-- 表格弹出面板 -->
    <div v-if="activePopup === 'table'" class="rte-input-popup rte-table-popup" :style="popupStyle">
      <div class="rte-table-inputs">
        <q-input
          v-model.number="tableRows"
          type="number"
          dense
          filled
          square
          min="1"
          max="20"
          :label="t('richText.tableRowsPrompt')"
        />
        <q-input
          v-model.number="tableCols"
          type="number"
          dense
          filled
          square
          min="1"
          max="20"
          :label="t('richText.tableColsPrompt')"
        />
      </div>
      <div class="rte-popup-actions">
        <q-btn flat no-caps color="grey-7" :label="t('common.cancel')" @click="closePopup" />
        <q-btn unelevated no-caps color="primary" :label="t('common.confirm')" @click="applyTable" />
      </div>
    </div>

    <!-- 表格操作栏（仅在光标位于表格内时显示） -->
    <div v-if="!isDisabled && isActive.table" class="rte-table-toolbar">
      <q-btn flat dense round size="xs" icon="sym_r_add_column_left" color="grey-7" @click="addColumnBefore">
        <q-tooltip>{{ t('richText.addColumnBefore') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_add_column_right" color="grey-7" @click="addColumnAfter">
        <q-tooltip>{{ t('richText.addColumnAfter') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_delete" color="grey-7" @click="deleteColumn">
        <q-tooltip>{{ t('richText.deleteColumn') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_add_row_above" color="grey-7" @click="addRowBefore">
        <q-tooltip>{{ t('richText.addRowBefore') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_add_row_below" color="grey-7" @click="addRowAfter">
        <q-tooltip>{{ t('richText.addRowAfter') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_remove" color="grey-7" @click="deleteRow">
        <q-tooltip>{{ t('richText.deleteRow') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_cell_merge" color="grey-7" @click="mergeCells">
        <q-tooltip>{{ t('richText.mergeCells') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_call_split" color="grey-7" @click="splitCell">
        <q-tooltip>{{ t('richText.splitCell') }}</q-tooltip>
      </q-btn>
      <q-btn flat dense round size="xs" icon="sym_r_table_chart" color="grey-7" @click="toggleHeaderCell">
        <q-tooltip>{{ t('richText.toggleHeaderCell') }}</q-tooltip>
      </q-btn>
      <q-separator vertical class="rte-separator" />
      <q-btn flat dense round size="xs" icon="sym_r_delete_forever" color="grey-7" @click="deleteTable">
        <q-tooltip>{{ t('richText.deleteTable') }}</q-tooltip>
      </q-btn>
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
  overflow: visible;
  position: relative;
}

.rich-text-editor.is-disabled {
  opacity: 0.75;
  pointer-events: none;
}

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

.rte-color-popup {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
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
  flex: 1 1 auto;
}

.rte-clear-color-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
}

.rte-clear-color-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.rte-clear-color-btn :deep(.q-icon) {
  font-size: 20px;
}

.rte-clear-color-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.rte-input-popup {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 12px;
  border-radius: 4px;
  min-width: 280px;
}

.rte-table-inputs {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.rte-table-inputs .q-input {
  flex: 1 1 0;
}

.rte-popup-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.rte-table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1px;
  padding: 4px 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: #f5f5f5;
}

.rte-table-toolbar .q-btn {
  width: 28px;
  height: 28px;
  min-width: 28px;
  min-height: 28px;
}

.rte-table-toolbar .q-btn .q-icon {
  font-size: 18px;
}

.rte-content {
  min-height: 200px;
  max-height: 500px;
  overflow-y: auto;
}

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

.rte-content :deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: rgba(0, 0, 0, 0.35);
  pointer-events: none;
  height: 0;
}
</style>

<style>
.body--dark .rich-text-editor {
  background: #2d2d2d;
  border-color: rgba(255, 255, 255, 0.22);
}

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

.body--dark .rte-color-popup,
.body--dark .rte-input-popup {
  background: #2d2d2d;
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

.body--dark .rte-color-popup .q-field--filled .q-field__control,
.body--dark .rte-input-popup .q-field--filled .q-field__control {
  background: #383838 !important;
}

.body--dark .rte-color-popup .q-field__native,
.body--dark .rte-color-popup .q-field__label,
.body--dark .rte-input-popup .q-field__native,
.body--dark .rte-input-popup .q-field__label {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .rte-clear-color-btn {
  color: rgba(255, 255, 255, 0.6);
}

.body--dark .rte-clear-color-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.body--dark .rte-table-toolbar {
  background: #252525;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.body--dark .rte-table-toolbar .q-btn {
  color: rgba(255, 255, 255, 0.55) !important;
}

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
