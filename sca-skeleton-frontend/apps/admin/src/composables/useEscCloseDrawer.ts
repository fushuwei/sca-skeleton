import { watch, onBeforeUnmount, type Ref } from "vue";

/**
 * 为抽屉/弹窗绑定 ESC 键关闭功能。
 *
 * 当 isOpen 为 true 时监听 keydown，按下 ESC 自动置为 false；
 * 为 false 时自动移除监听，避免全局事件泄漏。
 *
 * @param isOpen 控制抽屉开关的 Ref
 */
export function useEscCloseDrawer(isOpen: Ref<boolean>) {
  function handleKeydown(e: KeyboardEvent) {
    if (e.key === "Escape" && isOpen.value) {
      isOpen.value = false;
    }
  }

  const stop = watch(
    isOpen,
    (open) => {
      if (open) {
        window.addEventListener("keydown", handleKeydown);
      } else {
        window.removeEventListener("keydown", handleKeydown);
      }
    },
    { immediate: true }
  );

  // 组件卸载时确保移除监听
  onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleKeydown);
    stop();
  });
}
