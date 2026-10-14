import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { useConfirmDialog } from "@repo/ui";
import { showToast, isNotificationHandled } from "@repo/shared";
import { getSsoEnabledStatusApi, setSsoEnabledApi } from "../apis/sso-config";
import type { SsoProvider } from "../types/sso";

/**
 * 统一身份认证「启用」开关联动逻辑。
 *
 * 规则：CAS 与 OAuth2/OIDC 互斥，生产环境至多启用其一。开启当前方前检测另一方状态，
 * 若另一方已启用则弹出确认，用户确认后由服务端在事务内「启用当前 + 停用另一」。
 *
 * @param provider 当前页对应的提供方
 * @param otherProviderLabel 另一方名称（用于提示文案，需为已翻译文本）
 * @returns 当前配置是否处于启用态（响应式）与切换处理函数
 */
export function useSsoEnable(
  provider: SsoProvider,
  getOtherName: () => string
) {
  const { t } = useI18n({ useScope: "global" });
  const { confirmDialog } = useConfirmDialog();

  const enabled = ref(false);
  const toggling = ref(false);

  /** 回滚开关到给定值（API 失败或用户取消时使用） */
  function rollback(to: boolean) {
    enabled.value = to;
  }

  /**
   * 处理启用开关切换。
   * @param next 期望切换到的目标状态
   */
  async function handleToggle(next: boolean): Promise<void> {
    if (toggling.value) return;
    toggling.value = true;
    try {
    let otherEnabled = false;
    if (next) {
      // 开启前先探测另一方是否已启用
      otherEnabled = await checkOtherEnabled();
        if (otherEnabled) {
          try {
            await confirmDialog({
              type: "warning",
              title: t("common.systemPrompt"),
              message: t("ssoConfig.enableConflictConfirm", { other: getOtherName() })
            });
          } catch {
            rollback(false);
            return;
          }
        }
      }
      const result = await setSsoEnabledApi(provider, next, otherEnabled);
      if (result.code === 10_000) {
        enabled.value = next;
        showToast(next ? t("ssoConfig.enableSuccess") : t("ssoConfig.disableSuccess"), "positive");
      } else {
        rollback(!next);
        showToast(result.message || t("ssoConfig.enableFail"), "negative");
      }
    } catch (error) {
      rollback(!next);
      if (!isNotificationHandled(error)) {
        showToast(t("ssoConfig.enableFail"), "negative");
      }
    } finally {
      toggling.value = false;
    }
  }

  /** 查询另一方是否处于启用态 */
  async function checkOtherEnabled(): Promise<boolean> {
    try {
      const { data } = await getSsoEnabledStatusApi();
      if (!data) return false;
      return provider === "CAS" ? data.oauth2Enabled : data.casEnabled;
    } catch {
      // 探测失败不阻断，交由服务端最终保证互斥
      return false;
    }
  }

  return { enabled, toggling, handleToggle };
}
