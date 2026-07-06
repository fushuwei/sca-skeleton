import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";
import ConfirmDialog, { type ConfirmDialogType } from "../components/ConfirmDialog.vue";

export type { ConfirmDialogType };

export interface ConfirmDialogOptions {
  /** 对话框标题，默认取 i18n common.systemPrompt */
  title?: string;
  /** 正文消息 */
  message: string;
  /** 对话框类型，决定左侧图标，默认 confirm */
  type?: ConfirmDialogType;
  /** 确认按钮文字，默认取 i18n common.confirm */
  confirmText?: string;
  /** 取消按钮文字，默认取 i18n common.cancel */
  cancelText?: string;
}

/**
 * 确认对话框 composable。
 *
 * 返回一个 `confirmDialog` 函数，调用方式与原局部函数完全一致：
 * ```ts
 * const { confirmDialog } = useConfirmDialog();
 * await confirmDialog("确定删除？");          // 点击确认 resolve，点击取消 reject
 * await confirmDialog({ message: "...", type: "warning" }); // 扩展用法
 * ```
 */
export function useConfirmDialog() {
  const { t } = useI18n({ useScope: "global" });
  const $q = useQuasar();

  function confirmDialog(message: string): Promise<void>;
  function confirmDialog(options: ConfirmDialogOptions): Promise<void>;
  function confirmDialog(messageOrOptions: string | ConfirmDialogOptions): Promise<void> {
    const options: ConfirmDialogOptions =
      typeof messageOrOptions === "string" ? { message: messageOrOptions } : messageOrOptions;

    return new Promise<void>((resolve, reject) => {
      $q.dialog({
        component: ConfirmDialog,
        componentProps: {
          title: options.title ?? t("common.systemPrompt"),
          message: options.message,
          type: options.type ?? "confirm",
          confirmText: options.confirmText ?? t("common.confirm"),
          cancelText: options.cancelText ?? t("common.cancel"),
        },
      }).onOk(resolve).onCancel(reject);
    });
  }

  return { confirmDialog };
}
