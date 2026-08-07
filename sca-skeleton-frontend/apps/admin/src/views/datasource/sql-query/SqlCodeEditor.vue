<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { Compartment, EditorState } from "@codemirror/state";
import { EditorView, keymap, placeholder as cmPlaceholder } from "@codemirror/view";
import { basicSetup } from "codemirror";
import { MSSQL, MySQL, PLSQL, PostgreSQL, sql, type SQLDialect } from "@codemirror/lang-sql";
import { oneDark } from "@codemirror/theme-one-dark";

interface Props {
  modelValue: string;
  /** 数据库类型（决定 SQL 方言高亮与补全） */
  dbType?: string;
  /** 深色模式 */
  dark?: boolean;
  placeholderText?: string;
  /** 自动补全 Schema：表名 → 字段列表 */
  schema?: Record<string, readonly string[]>;
}

const props = withDefaults(defineProps<Props>(), {
  dbType: "",
  dark: false,
  placeholderText: "",
  schema: () => ({})
});

const emit = defineEmits<{
  "update:modelValue": [value: string];
  execute: [];
}>();

const editorEl = ref<HTMLElement>();
let view: EditorView | null = null;

const langComp = new Compartment();
const themeComp = new Compartment();

/** dbType → CodeMirror SQL 方言（未识别的类型退化为标准 SQL） */
function dialectOf(dbType: string): SQLDialect | undefined {
  switch (dbType) {
    case "MYSQL":
    case "CLICKHOUSE":
      return MySQL;
    case "POSTGRESQL":
    case "KINGBASE":
      return PostgreSQL;
    case "ORACLE":
    case "DAMENG":
      return PLSQL;
    case "SQLSERVER":
      return MSSQL;
    default:
      return undefined;
  }
}

function langExtension() {
  const dialect = dialectOf(props.dbType);
  const config = { schema: props.schema, upperCaseKeywords: true };
  return dialect ? sql({ ...config, dialect }) : sql(config);
}

onMounted(() => {
  if (!editorEl.value) return;
  view = new EditorView({
    parent: editorEl.value,
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        basicSetup,
        langComp.of(langExtension()),
        themeComp.of(props.dark ? oneDark : []),
        props.placeholderText ? cmPlaceholder(props.placeholderText) : [],
        keymap.of([
          {
            key: "Mod-Enter",
            run: () => {
              emit("execute");
              return true;
            }
          }
        ]),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            emit("update:modelValue", update.state.doc.toString());
          }
        })
      ]
    })
  });
});

onBeforeUnmount(() => {
  view?.destroy();
  view = null;
});

// 外部修改 modelValue（如树节点生成 SQL）时同步到编辑器
watch(
  () => props.modelValue,
  (val) => {
    if (!view) return;
    if (view.state.doc.toString() !== val) {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: val }
      });
    }
  }
);

// 方言 / 补全 Schema 变化时重建语言扩展
watch([() => props.dbType, () => props.schema], () => {
  view?.dispatch({ effects: langComp.reconfigure(langExtension()) });
});

// 主题切换
watch(
  () => props.dark,
  (dark) => {
    view?.dispatch({ effects: themeComp.reconfigure(dark ? oneDark : []) });
  }
);

/** 聚焦编辑器 */
function focus() {
  view?.focus();
}

/** 在光标处插入文本 */
function insertAtCursor(text: string) {
  if (!view) return;
  const { from, to } = view.state.selection.main;
  view.dispatch({
    changes: { from, to, insert: text },
    selection: { anchor: from + text.length }
  });
  view.focus();
}

defineExpose({ focus, insertAtCursor });
</script>

<template>
  <div ref="editorEl" class="sql-cm-host" />
</template>

<style scoped>
.sql-cm-host {
  height: 100%;
  min-height: 0;
}

.sql-cm-host :deep(.cm-editor) {
  height: 100%;
  font-size: 13px;
}

.sql-cm-host :deep(.cm-editor.cm-focused) {
  outline: none;
}

.sql-cm-host :deep(.cm-scroller) {
  font-family: "SF Mono", "Monaco", "Menlo", "Consolas", "Liberation Mono", monospace;
  line-height: 1.65;
}

.sql-cm-host :deep(.cm-gutters) {
  border-right: none;
}
</style>
