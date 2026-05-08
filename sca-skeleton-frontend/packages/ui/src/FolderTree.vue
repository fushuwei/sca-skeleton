<script setup>
/**
 * 通用「文件夹式」分支展开的 Quasar QTree 封装：
 * 分支为文件夹收起/展开；叶子在同一列展示叶子图标（与分支列宽对齐）；不限于导航菜单。
 */
import { QIcon, QTree } from "quasar";
import { useAttrs } from "vue";

defineOptions({
  name: "FolderTree",
  inheritAttrs: false
});

const props = defineProps({
  /** 收起态文件夹图标（Material Symbols Rounded 等 Quasar 支持的名称） */
  folderClosedIcon: {
    type: String,
    default: "sym_r_folder"
  },
  /** 展开态文件夹图标 */
  folderOpenIcon: {
    type: String,
    default: "sym_r_folder_open"
  },
  /** 无子节点时的前置图标；空字符串则不占图标（仍保留列宽占位） */
  leafIcon: {
    type: String,
    /** Material Symbols Rounded：`nest_eco_leaf` */
    default: "sym_r_nest_eco_leaf"
  },
  /** 与 QTree `children-key` 一致，用于判断是否存在子节点 */
  childrenKey: {
    type: String,
    default: "children"
  },
  /** 与 QTree `label-key` 一致 */
  labelKey: {
    type: String,
    default: "label"
  }
});

const attrs = useAttrs();

function isBranchNode(node) {
  if (!node || typeof node !== "object") {
    return false;
  }
  const lazy = node.lazy;
  if (lazy === true || lazy === "pending" || lazy === "loading") {
    return true;
  }
  const ch = node[props.childrenKey];
  return Array.isArray(ch) && ch.length > 0;
}

function toggleBranch(scope, event) {
  event?.stopPropagation?.();
  scope.expanded = !scope.expanded;
}
</script>

<template>
  <QTree class="folder-tree" v-bind="attrs" :label-key="labelKey" :children-key="childrenKey">
    <template #default-header="scope">
      <div class="folder-tree__header row items-center no-wrap col">
        <button
          v-if="isBranchNode(scope.node)"
          type="button"
          class="folder-tree__folder-hit"
          :aria-expanded="scope.expanded === true ? 'true' : 'false'"
          @click="toggleBranch(scope, $event)"
        >
          <QIcon
            :name="scope.expanded ? folderOpenIcon : folderClosedIcon"
            class="folder-tree__folder-icon material-symbols-rounded"
          />
        </button>
        <span v-else class="folder-tree__leaf-cell" aria-hidden="true">
          <QIcon
            v-if="leafIcon"
            :name="leafIcon"
            class="folder-tree__leaf-icon material-symbols-rounded"
          />
        </span>

        <QIcon
          v-if="scope.node.icon"
          :name="scope.node.icon"
          class="folder-tree__node-icon material-symbols-rounded q-mr-sm"
        />

        <div class="folder-tree__label col ellipsis">{{ scope.node[labelKey] }}</div>
      </div>
    </template>
  </QTree>
</template>

<style scoped>
/* 自定义文件夹展开控件，隐藏 Quasar 默认三角箭头 */
.folder-tree :deep(.q-tree__arrow) {
  display: none !important;
}

/* 根层：Quasar 仅对首层 .q-tree__node--child 的 header 设 padding-left:24px；须 .folder-tree.q-tree 复合（与 .q-tree 同节点） */
.folder-tree.q-tree > :deep(.q-tree__node > .q-tree__node-header) {
  padding-left: 12px !important;
}

.folder-tree__folder-hit,
.folder-tree__leaf-cell {
  flex: 0 0 24px;
  width: 24px;
  min-width: 24px;
  height: 24px;
  margin-right: 8px;
  box-sizing: border-box;
}

.folder-tree__leaf-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.folder-tree__leaf-icon {
  font-size: 20px !important;
  opacity: 0.72;
}

.folder-tree__folder-hit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.folder-tree__folder-hit:focus-visible {
  outline: 2px solid rgba(0, 150, 136, 0.45);
  outline-offset: 1px;
}

.folder-tree__folder-icon {
  font-size: 20px !important;
}

.folder-tree__node-icon {
  font-size: 20px !important;
}

.folder-tree__label {
  min-width: 0;
}
</style>
