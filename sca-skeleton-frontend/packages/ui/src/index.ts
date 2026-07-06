import { defineComponent, h } from "vue";

export { default as FolderTree } from "./FolderTree.vue";
export { default as ConfirmDialog } from "./components/ConfirmDialog/ConfirmDialog.vue";
export type { ConfirmDialogType } from "./components/ConfirmDialog/ConfirmDialog.vue";
export { useConfirmDialog, type ConfirmDialogOptions } from "./composables/useConfirmDialog";

export const SharedTag = defineComponent({
  name: "SharedTag",
  props: {
    text: {
      type: String,
      default: "Shared UI Component"
    }
  },
  setup(props) {
    return () => h("span", { style: "padding:4px 8px;background:#f0f5ff;border-radius:4px;" }, props.text);
  }
});
