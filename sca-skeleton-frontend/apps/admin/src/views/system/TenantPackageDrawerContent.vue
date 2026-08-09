<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenantPackage, PermissionAssignOption, PermissionTreeNode } from "../../types/auth";
import { createTenantPackageApi, updateTenantPackageApi } from "../../apis/tenant-package";
import { getTenantPackagePermissionIdsApi } from "../../apis/tenant-package";
import { getTenantPackageAssignOptionsApi } from "../../apis/tenant-package";

const { t, locale } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  pkg?: SysTenantPackage;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  name: "",
  code: "",
  status: "enabled",
  userLimit: null as number | null,
  apiLimit: null as number | null,
  storageLimit: null as number | null,
  expireDays: null as number | null,
  sort: 100,
  remark: "",
  permissionIds: [] as string[]
});

// ── 限额数字输入过滤（与数据源页面连接池配置输入一致）──
function onNumericKeydown(e: KeyboardEvent) {
  if (e.isComposing || e.keyCode === 229 || e.key === "Process") {
    e.preventDefault();
    return;
  }
  const controlKeys = ["Backspace", "Delete", "Tab", "Escape", "Enter", "Home", "End", "ArrowLeft", "ArrowRight"];
  if (controlKeys.includes(e.key)) return;
  if ((e.ctrlKey || e.metaKey) && /^[acvxzy]$/i.test(e.key)) return;
  if (!/^\d$/.test(e.key)) {
    e.preventDefault();
  }
}

/** 限额字段输入过滤：仅保留数字；清空则为 null（表示无限制） */
function onLimitInput(field: "userLimit" | "apiLimit" | "storageLimit" | "expireDays", v: string | number | null) {
  const digits = String(v ?? "").replace(/\D/g, "");
  form[field] = digits ? Number(digits) : null;
}

/** 限额校验：留空表示无限制（通过）；填写时必须为大于 0 的整数 */
function validateLimit(val: number | null): true | string {
  if (val === null || val === undefined) return true;
  if (!Number.isInteger(val) || val <= 0) {
    return t("tenantPackageMgmt.limitPositiveInteger");
  }
  return true;
}

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("tenantPackageMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("tenantPackageMgmt.codeRequired")],
  status: [(v: string) => !!v || t("tenantPackageMgmt.statusRequired")],
  userLimit: [(v: number | null) => validateLimit(v)],
  apiLimit: [(v: number | null) => validateLimit(v)],
  storageLimit: [(v: number | null) => validateLimit(v)],
  expireDays: [(v: number | null) => validateLimit(v)]
}));

/** 套餐编码输入处理：自动转小写 */
function onCodeInput(val: string | number | null) {
  form.code = String(val || "").toLowerCase().trim();
}

const statusOptions = computed(() => [
  { label: t("tenantPackageMgmt.statusEnabled"), value: "enabled" },
  { label: t("tenantPackageMgmt.statusDisabled"), value: "disabled" }
]);

// ── 权限树 ──
const permTreeLoading = ref(false);
const allPermissions = ref<PermissionAssignOption[]>([]);

/** 分类节点 id 前缀（避免与真实权限 ID 冲突） */
const REALM_GROUP_PREFIX = "realm:";
/** 节点叶子总数的扩展字段 key（buildPermTree 时预计算，用于分支统计徽章） */
const LEAF_COUNT_KEY = "__leafCount";
/** 权限域分组定义（顺序即树展示顺序） */
const REALM_GROUPS: { realm: string; labelKey: string }[] = [
  { realm: "admin", labelKey: "permissionMgmt.adminGroup" },
  { realm: "portal", labelKey: "permissionMgmt.portalGroup" }
];
const realmGroupId = (realm: string) => REALM_GROUP_PREFIX + realm;

const permTreeNodes = computed(() => buildPermTree(allPermissions.value));
const permTreeExpanded = ref<string[]>([]);
const permTreeTicked = ref<string[]>([]);
const permSearchKey = ref("");

/** 权限树未加载时的待处理操作 */
const pendingPermIds = ref<string[] | null>(null);

/**
 * 将扁平权限列表转成树结构，并按权限域（admin/portal）分组成两棵子树。
 *
 * 后端已做过滤（仅返回启用且可见的权限，且非超管仅返回自身拥有的权限），
 * 前端直接信任后端数据，不再做任何过滤，避免「掩耳盗铃」式掩盖后端问题。
 */
function buildPermTree(perms: PermissionAssignOption[]): PermissionTreeNode[] {
  if (!perms.length) return [];

  const sorted = [...perms].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  // 先按 realm 分组，再各自建树
  const isEn = locale.value.startsWith("en");
  const groups: PermissionTreeNode[] = [];

  for (const group of REALM_GROUPS) {
    const groupPerms = sorted.filter((p) => p.realm === group.realm);
    if (!groupPerms.length) continue;

    const map = new Map<string, PermissionTreeNode>();
    for (const p of groupPerms) {
      map.set(p.id, {
        id: p.id,
        label: (isEn && p.nameEn) ? p.nameEn : p.name,
        parentId: p.parentId,
        type: p.type,
        icon: p.icon || "",
        realm: p.realm,
        children: []
      });
    }

    const roots: PermissionTreeNode[] = [];
    for (const p of groupPerms) {
      const node = map.get(p.id)!;
      if (!p.parentId || p.parentId === "0") {
        roots.push(node);
      } else {
        const parent = map.get(p.parentId);
        if (parent) {
          parent.children = parent.children ?? [];
          parent.children.push(node);
        }
        // 父节点不在返回列表中（非超管场景下当前用户未持有该父权限），丢弃该节点
      }
    }

    const cleanEmpty = (nodes: PermissionTreeNode[]) => {
      for (const n of nodes) {
        if (n.children?.length) {
          cleanEmpty(n.children);
        } else {
          delete n.children;
        }
      }
    };
    cleanEmpty(roots);

    // 预计算各节点叶子总数，挂在扩展字段上，供分支统计徽章直接使用
    const attachLeafCount = (nodes: PermissionTreeNode[]) => {
      for (const n of nodes) {
        (n as unknown as Record<string, number>)[LEAF_COUNT_KEY] = countLeaves(n);
        if (n.children?.length) attachLeafCount(n.children);
      }
    };
    attachLeafCount(roots);

    if (roots.length) {
      const groupNode: PermissionTreeNode = {
        id: realmGroupId(group.realm),
        label: t(group.labelKey),
        parentId: "",
        type: "realm-group",
        icon: "",
        realm: group.realm,
        children: roots
      };
      (groupNode as unknown as Record<string, number>)[LEAF_COUNT_KEY] = countLeaves(groupNode);
      groups.push(groupNode);
    }
  }

  return groups;
}

/** 收集权限树中所有叶子节点的 ID（leaf-filtered 模式要求 ticked 数组仅含叶子节点） */
function collectLeafIds(nodes: PermissionTreeNode[]): Set<string> {
  const leafIds = new Set<string>();
  const collect = (list: PermissionTreeNode[]) => {
    for (const n of list) {
      if (n.children?.length) {
        collect(n.children);
      } else {
        leafIds.add(n.id);
      }
    }
  };
  collect(nodes);
  return leafIds;
}

/** 统计节点子树内叶子节点总数（含自身为叶子时计 1），用于分支「已选/总数」统计 */
function countLeaves(node: PermissionTreeNode): number {
  if (!node.children?.length) return 1;
  return node.children.reduce((sum, c) => sum + countLeaves(c), 0);
}

/** 读取 buildPermTree 预计算的叶子总数（PermissionTreeNode 未声明该扩展字段，此处收敛类型断言） */
function nodeLeafCount(node: PermissionTreeNode): number {
  return (node as unknown as Record<string, number>)[LEAF_COUNT_KEY] ?? countLeaves(node);
}

/** 分支已勾选叶子数：取子树叶子集合与已勾选集合的交集 */
function nodeTickedCount(node: PermissionTreeNode): number {
  if (!permTreeTicked.value.length) return 0;
  const leafIds = collectLeafIds([node]);
  return permTreeTicked.value.filter((id) => leafIds.has(id)).length;
}

/** 收集被勾选节点的所有祖先 ID（含自身），用于保存完整权限链（module/folder/menu + button） */
function collectWithAncestors(tickedIds: string[]): string[] {
  if (!tickedIds.length) return [];
  const result = new Set<string>(tickedIds);
  // 构建 id -> parentId 映射（基于全量权限列表，确保能追溯到被过滤的祖先）
  const parentMap = new Map<string, string>();
  for (const p of allPermissions.value) {
    parentMap.set(p.id, p.parentId);
  }
  // 对每个 ticked ID 沿 parentId 向上追溯，补全所有祖先
  for (const id of tickedIds) {
    let current = parentMap.get(id);
    while (current && current !== "0" && !result.has(current)) {
      result.add(current);
      current = parentMap.get(current);
    }
  }
  return [...result];
}

/** 树节点行图标：realm 分组节点不展示图标（以徽章表达权限域），模块/目录用 folder，菜单用叶子 */
function permNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "realm-group") return "";
  if (node.type === "module" || node.type === "folder") {
    return permTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
  }
  if (node.type === "menu") return "sym_r_nest_eco_leaf";
  return "";
}

/** 过滤树节点（按关键字，保留匹配的父节点） */
function filterPermTree(nodes: PermissionTreeNode[], keyword: string): PermissionTreeNode[] {
  if (!keyword.trim()) return nodes;
  const lower = keyword.toLowerCase();
  const result: PermissionTreeNode[] = [];
  for (const n of nodes) {
    const childResult = n.children?.length ? filterPermTree(n.children, keyword) : [];
    if (n.label.toLowerCase().includes(lower) || childResult.length) {
      result.push({ ...n, children: childResult.length ? childResult : n.children?.length ? [] : undefined });
    }
  }
  return result;
}

const filteredPermTreeNodes = computed(() => filterPermTree(permTreeNodes.value, permSearchKey.value));

/** 搜索时自动展开所有节点 */
watch(permSearchKey, (val) => {
  if (val.trim()) {
    const allKeys: string[] = [];
    const collectKeys = (nodes: PermissionTreeNode[]) => {
      for (const n of nodes) {
        allKeys.push(n.id);
        if (n.children?.length) collectKeys(n.children);
      }
    };
    collectKeys(filteredPermTreeNodes.value);
    permTreeExpanded.value = allKeys;
  }
});

/** 当前过滤结果的全部节点 key（展开/收起与全选均作用于可见范围） */
const filteredTreeKeys = computed(() => {
  const keys: string[] = [];
  const collectKeys = (nodes: PermissionTreeNode[]) => {
    for (const n of nodes) {
      keys.push(n.id);
      if (n.children?.length) collectKeys(n.children);
    }
  };
  collectKeys(filteredPermTreeNodes.value);
  return keys;
});

/** 可见节点是否已全部展开（决定工具栏按钮显示「收起」还是「展开」） */
const allVisibleExpanded = computed(
  () => filteredTreeKeys.value.length > 0 && filteredTreeKeys.value.every((k) => permTreeExpanded.value.includes(k))
);

/** 展开/收起全部（针对当前过滤后的可见树） */
function toggleExpandAll() {
  permTreeExpanded.value = allVisibleExpanded.value ? [] : [...filteredTreeKeys.value];
}

/** 全选：勾选可见树的全部叶子节点（与已有勾选取并集，避免搜索过滤时丢失不可见区域的勾选） */
function selectAllVisible() {
  const visibleLeaves = collectLeafIds(filteredPermTreeNodes.value);
  permTreeTicked.value = [...new Set([...permTreeTicked.value, ...visibleLeaves])];
}

/** 清空全部勾选 */
function clearAllTicks() {
  permTreeTicked.value = [];
}

async function loadPermTree() {
  permTreeLoading.value = true;
  try {
    const result = await getTenantPackageAssignOptionsApi();
    if (result.code === 10_000 && result.data) {
      allPermissions.value = result.data;
      permTreeExpanded.value = permTreeNodes.value.map((n) => n.id);
      if (pendingPermIds.value) {
        // 过滤为仅叶子节点 ID，匹配 q-tree leaf-filtered 策略
        const leafIds = collectLeafIds(permTreeNodes.value);
        const filtered = pendingPermIds.value.filter((id) => leafIds.has(id));
        permTreeTicked.value = filtered;
        form.permissionIds = [...filtered];
        pendingPermIds.value = null;
      }
    }
  } catch {
    // 静默失败
  } finally {
    permTreeLoading.value = false;
  }
}

/** 加载套餐已分配的权限 ID 列表 */
async function loadPackagePermissions(packageId: string) {
  try {
    const result = await getTenantPackagePermissionIdsApi(packageId);
    if (result.code === 10_000 && result.data) {
      if (allPermissions.value.length === 0) {
        pendingPermIds.value = result.data;
      } else {
        // 过滤为仅叶子节点 ID，匹配 q-tree leaf-filtered 策略
        const leafIds = collectLeafIds(permTreeNodes.value);
        const filtered = result.data.filter((id) => leafIds.has(id));
        permTreeTicked.value = filtered;
        form.permissionIds = [...filtered];
      }
    }
  } catch {
    // 静默失败
  }
}

function resetForm() {
  form.id = "";
  form.name = "";
  form.code = "";
  form.status = "enabled";
  form.userLimit = null;
  form.apiLimit = null;
  form.storageLimit = null;
  form.expireDays = null;
  form.sort = 100;
  form.remark = "";
  form.permissionIds = [];
  permTreeTicked.value = [];
}

function initForm() {
  resetForm();
  if (props.pkg) {
    form.id = props.pkg.id;
    form.name = props.pkg.name;
    form.code = props.pkg.code ?? "";
    form.status = props.pkg.status || "enabled";
    // -1 表示无限制，前端展示为空（留空即无限制）
    form.userLimit = props.pkg.userLimit === -1 ? null : (props.pkg.userLimit ?? null);
    form.apiLimit = props.pkg.apiLimit === -1 ? null : (props.pkg.apiLimit ?? null);
    form.storageLimit = props.pkg.storageLimit === -1 ? null : (props.pkg.storageLimit ?? null);
    form.expireDays = props.pkg.expireDays === -1 ? null : (props.pkg.expireDays ?? null);
    form.sort = props.pkg.sort ?? 100;
    form.remark = props.pkg.remark || "";

    if (props.pkg.id) {
      loadPackagePermissions(props.pkg.id);
    }
  }
}

watch(() => props.pkg, initForm, { immediate: true });

onMounted(() => {
  loadPermTree();
});

// 同步 q-tree ticked 到 form.permissionIds
watch(permTreeTicked, (val) => {
  form.permissionIds = [...val];
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  // 收集被勾选叶子节点的所有祖先 ID，确保保存完整权限链（module/folder/menu + button），
  // 避免 leaf-filtered 策略导致只保存 button 而菜单树断裂
  const fullPermissionIds = collectWithAncestors(form.permissionIds);

  const data: Record<string, unknown> = {
    name: form.name,
    code: form.code,
    status: form.status,
    // 留空（null）提交为 -1，后端以 -1 表示无限制
    userLimit: form.userLimit ?? -1,
    apiLimit: form.apiLimit ?? -1,
    storageLimit: form.storageLimit ?? -1,
    expireDays: form.expireDays ?? -1,
    sort: form.sort,
    remark: form.remark || undefined,
    permissionIds: fullPermissionIds.length ? fullPermissionIds : undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createTenantPackageApi(data);
    } else {
      data.id = form.id;
      data.version = props.pkg?.version;
      result = await updateTenantPackageApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("tenantPackageMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("tenantPackageMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("tenantPackageMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="pkg-drawer-content">
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="pkg-drawer-form" class="pkg-drawer-form pkg-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 套餐名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('tenantPackageMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 套餐编码 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.code"
            @update:model-value="onCodeInput"
            :label="t('tenantPackageMgmt.code')"
            filled
            square
            :rules="formRules.code"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 套餐状态 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.status"
            :label="t('tenantPackageMgmt.status')"
            filled
            square
            :options="statusOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.status"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('tenantPackageMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 用户数限制 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.userLimit"
            @update:model-value="(v) => onLimitInput('userLimit', v)"
            @keydown="onNumericKeydown"
            type="text"
            inputmode="numeric"
            :label="t('tenantPackageMgmt.userLimit')"
            :placeholder="t('tenantPackageMgmt.limitPlaceholder')"
            filled
            square
            :rules="formRules.userLimit"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- API调用限制 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.apiLimit"
            @update:model-value="(v) => onLimitInput('apiLimit', v)"
            @keydown="onNumericKeydown"
            type="text"
            inputmode="numeric"
            :label="t('tenantPackageMgmt.apiLimit')"
            :placeholder="t('tenantPackageMgmt.limitPlaceholder')"
            filled
            square
            :rules="formRules.apiLimit"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 存储限制 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.storageLimit"
            @update:model-value="(v) => onLimitInput('storageLimit', v)"
            @keydown="onNumericKeydown"
            type="text"
            inputmode="numeric"
            :label="t('tenantPackageMgmt.storageLimit')"
            :placeholder="t('tenantPackageMgmt.limitPlaceholder')"
            filled
            square
            :rules="formRules.storageLimit"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 有效期天数 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.expireDays"
            @update:model-value="(v) => onLimitInput('expireDays', v)"
            @keydown="onNumericKeydown"
            type="text"
            inputmode="numeric"
            :label="t('tenantPackageMgmt.expireDays')"
            :placeholder="t('tenantPackageMgmt.limitPlaceholder')"
            filled
            square
            :rules="formRules.expireDays"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
      </div>

      <!-- ── 权限分配：一体化面板（面板头 + 工具行 + 树内容区），与驱动上传面板同一设计语言 ── -->
      <div class="perm-panel q-mt-md" :class="{ 'perm-panel--readonly': drawerReadonly }">
        <!-- 面板头：左侧标题，右侧已选统计胶囊与快捷操作 -->
        <div class="perm-panel__head">
          <q-icon name="sym_r_security" size="18px" class="perm-panel__head-icon" />
          <span class="perm-panel__title">{{ t('tenantPackageMgmt.permissionAssign') }}</span>
          <q-space />
          <q-badge v-if="permTreeTicked.length" color="primary" rounded outline class="perm-panel__count">
            {{ t('tenantPackageMgmt.selectedPermissions', { count: permTreeTicked.length }) }}
          </q-badge>
          <template v-if="!drawerReadonly">
            <q-btn flat dense no-caps size="12px" color="grey-8" class="perm-panel__action" @click="selectAllVisible">
              {{ t('tenantPackageMgmt.selectAll') }}
            </q-btn>
            <q-btn
              flat dense no-caps size="12px" color="grey-8"
              class="perm-panel__action"
              :disable="!permTreeTicked.length"
              @click="clearAllTicks"
            >
              {{ t('tenantPackageMgmt.clearAll') }}
            </q-btn>
          </template>
          <q-btn flat dense no-caps size="12px" color="grey-8" class="perm-panel__action" @click="toggleExpandAll">
            {{ allVisibleExpanded ? t('tenantPackageMgmt.collapseAll') : t('tenantPackageMgmt.expandAll') }}
          </q-btn>
        </div>

        <!-- 工具行：面板内无边框搜索框，focus 时底部主色描边 -->
        <div class="perm-panel__toolbar">
          <q-input
            v-model="permSearchKey"
            dense
            borderless
            square
            :placeholder="t('tenantPackageMgmt.searchPermission')"
            clearable
            class="perm-panel__search full-width"
            :disable="drawerReadonly"
          >
            <template #prepend>
              <q-icon name="sym_r_search" size="18px" />
            </template>
          </q-input>
        </div>

        <!-- 树内容区：独立滚动 -->
        <div class="perm-panel__body">
          <q-scroll-area style="height: 320px">
            <template v-if="permTreeLoading">
              <div v-for="i in 6" :key="i" class="perm-skeleton-row">
                <q-skeleton type="rect" width="60%" height="16px" class="q-ml-md q-my-sm" />
              </div>
            </template>
            <div v-else-if="!filteredPermTreeNodes.length && permSearchKey.trim()" class="perm-panel__empty">
              {{ t('tenantPackageMgmt.noSearchResult') }}
            </div>
            <q-tree
              v-else
              :nodes="filteredPermTreeNodes"
              node-key="id"
              label-key="label"
              children-key="children"
              v-model:expanded="permTreeExpanded"
              v-model:ticked="permTreeTicked"
              tick-strategy="leaf-filtered"
              no-connectors
              dense
              :no-tick-children="drawerReadonly"
              :class="['perm-tree', { 'perm-tree--readonly': drawerReadonly }]"
              no-nodes-label=" "
            >
              <template #default-header="scope">
                <div
                  class="perm-tree-node row items-center no-wrap full-width"
                  :class="{ 'perm-tree-node--realm': scope.node.type === 'realm-group' }"
                >
                  <!-- 查看模式：以勾选图标展示已选状态，替代禁用的复选框 -->
                  <q-icon
                    v-if="drawerReadonly && permTreeTicked.includes(scope.node.id)"
                    name="sym_r_check_small"
                    size="16px"
                    color="primary"
                    class="q-mr-xs"
                  />
                  <q-icon
                    v-if="permNodeIcon(scope.node)"
                    :name="permNodeIcon(scope.node)"
                    size="18px"
                    class="q-mr-sm"
                    :class="{
                      'perm-tree-icon--folder': scope.node.type === 'module' || scope.node.type === 'folder',
                      'perm-tree-icon--menu': scope.node.type === 'menu'
                    }"
                  />
                  <!-- realm 分组节点：权限域色点标识（admin=purple / portal=teal，与用户域徽章一致） -->
                  <span
                    v-if="scope.node.type === 'realm-group'"
                    class="perm-tree-node__realm-dot"
                    :class="scope.node.realm === 'admin' ? 'perm-tree-node__realm-dot--admin' : 'perm-tree-node__realm-dot--portal'"
                  />
                  <span
                    class="ellipsis"
                    :class="{ 'perm-tree-node__realm-label': scope.node.type === 'realm-group' }"
                  >{{ scope.node.label }}</span>
                  <q-space />
                  <!-- 分支统计徽章：已选叶子/叶子总数，选满时转主色 -->
                  <span
                    v-if="scope.node.children?.length && nodeLeafCount(scope.node) > 0"
                    class="perm-tree-node__stats"
                    :class="{ 'perm-tree-node__stats--full': nodeTickedCount(scope.node) === nodeLeafCount(scope.node) }"
                  >
                    {{ nodeTickedCount(scope.node) }}/{{ nodeLeafCount(scope.node) }}
                  </span>
                </div>
              </template>
            </q-tree>
          </q-scroll-area>
        </div>
      </div>

      <!-- 备注 -->
      <div class="q-mt-md">
        <q-input
          v-model="form.remark"
          :label="t('tenantPackageMgmt.remark')"
          filled
          square
          type="textarea"
          rows="3"
          :disable="drawerReadonly"
          :readonly="drawerReadonly"
          hide-bottom-space
        />
      </div>
    </q-form>

    <!-- 底部操作栏：固定第三段，不随内容滚动；按钮直接挂在页脚容器上，间距由 CSS gap 控制 -->
    <div v-if="!drawerReadonly" class="pkg-drawer-footer row items-center justify-end no-wrap">
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
        form="pkg-drawer-form"
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
/* 三段式抽屉布局：外层 .pkg-drawer-body 不再滚动，
   中间内容区（q-form.pkg-drawer-main）独立滚动，底部操作栏固定为第三段 */
.pkg-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中间内容区：唯一的滚动区域 */
.pkg-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

/* 底部操作栏：固定第三段，不参与内容滚动（按钮直接子元素，gap 控制间距） */
.pkg-drawer-footer {
  flex-shrink: 0;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

/* ── 权限分配一体化面板：面板头 + 工具行 + 树内容区共享同一边框与背景（同驱动上传面板语言） ── */
.perm-panel {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fafbfc;
  overflow: hidden;
}

/* 只读态：白底信息展示卡片，弱化操作感（同上传面板只读态） */
.perm-panel--readonly {
  background: #fff;
}

/* 面板头：左侧标题，右侧统计胶囊与快捷操作 */
.perm-panel__head {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.perm-panel__head-icon {
  color: #757575;
  margin-right: 4px;
  flex-shrink: 0;
}

.perm-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  flex-shrink: 0;
}

.perm-panel__count {
  font-weight: 400;
  padding: 3px 9px;
  margin-right: 4px;
  flex-shrink: 0;
}

.perm-panel__action {
  padding: 0 6px;
  flex-shrink: 0;
}

/* 工具行：搜索框与面板同宽，无独立边框 */
.perm-panel__toolbar {
  padding: 2px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.perm-panel__search :deep(.q-field__control) {
  height: 36px;
}

.perm-panel__search :deep(.q-field__prepend) {
  color: #9aa3af;
}

/* focus 反馈：底部主色描边（borderless 模式默认无任何聚焦指示） */
.perm-panel__search :deep(.q-field--focused .q-field__control) {
  box-shadow: inset 0 -2px 0 0 var(--q-primary);
}

/* 树内容区 */
.perm-panel__body {
  padding: 6px 8px;
}

/* 搜索无匹配的空态 */
.perm-panel__empty {
  padding: 40px 16px;
  text-align: center;
  font-size: 13px;
  color: #9aa3af;
}

.perm-skeleton-row {
  padding: 4px 0;
}

/* 树节点行头占满整行，使行悬停背景与分支统计徽章对齐至行尾 */
.perm-tree :deep(.q-tree__node-child) {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
}

.perm-tree :deep(.q-tree__node-header) {
  flex: 1 1 auto;
  min-width: 0;
  margin: 1px 0;
  padding: 0;
  min-height: 0;
  border-radius: 6px;
  box-sizing: border-box;
  transition: background 0.15s ease;
}

.perm-tree :deep(.q-tree__node-header:hover) {
  background: rgba(25, 118, 210, 0.04);
}

.perm-tree-node {
  min-width: 0;
  padding: 4px 8px;
  min-height: 32px;
}

/* realm 分组节点：分组标签式展示，行距略增以区隔分组 */
.perm-tree-node--realm {
  padding: 6px 8px;
  margin-top: 6px;
}

.perm-tree-node__realm-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #757575;
}

/* realm 分组节点：权限域色点（purple-7 / teal-7） */
.perm-tree-node__realm-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  flex-shrink: 0;
}

.perm-tree-node__realm-dot--admin {
  background: #7b1fa2;
}

.perm-tree-node__realm-dot--portal {
  background: #00796b;
}

/* 节点图标语义化配色：目录琥珀 / 菜单绿叶 */
.perm-tree-icon--folder {
  color: #b98a2f;
}

.perm-tree-icon--menu {
  color: #4caf50;
}

/* 分支统计徽章：已选/总数，选满时转主色 */
.perm-tree-node__stats {
  margin-left: 8px;
  font-size: 11px;
  color: #9aa3af;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.perm-tree-node__stats--full {
  color: var(--q-primary);
  font-weight: 600;
}

/* 查看模式：隐藏复选框列，已选状态改用行内勾选图标展示 */
.perm-tree--readonly :deep(.q-tree__tickbox) {
  display: none;
}
</style>

<style>
.body--dark .pkg-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .pkg-drawer-form .q-field__native,
.body--dark .pkg-drawer-form .q-field__prefix,
.body--dark .pkg-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .pkg-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .pkg-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .pkg-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .pkg-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .pkg-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 权限分配面板暗色模式 */
.body--dark .perm-panel {
  border-color: rgba(255, 255, 255, 0.08);
  background: #252525;
}

.body--dark .perm-panel--readonly {
  background: #222;
}

.body--dark .perm-panel__head,
.body--dark .perm-panel__toolbar {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.body--dark .perm-panel__head-icon {
  color: rgba(255, 255, 255, 0.72);
}

.body--dark .perm-panel__title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .perm-panel__search :deep(.q-field__native) {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .perm-tree :deep(.q-tree__node-header:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.body--dark .perm-tree-node__realm-label {
  color: rgba(255, 255, 255, 0.6);
}

.body--dark .perm-tree-node__stats {
  color: rgba(255, 255, 255, 0.45);
}

.body--dark .perm-tree-icon--folder {
  color: #d8ae5f;
}

/* 权限树骨架屏 */
.body--dark .perm-skeleton-row .q-skeleton {
  background: rgba(255, 255, 255, 0.08);
}
</style>
