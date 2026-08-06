/**
 * 修复 Chrome 下拖拽选中文字离开抽屉导致误关闭的问题。
 * 只有当 mousedown 和 mouseup 都在蒙层（mask）上触发时才执行关闭逻辑。
 * 
 * @param closeFn 关闭抽屉的回调函数
 */
export function useClickOutsideClose(closeFn: () => void) {
  let isMouseDownOnMask = false;

  function handleMaskMouseDown(e: MouseEvent) {
    // 只有直接点击在绑定事件的元素（蒙层）上才记录
    isMouseDownOnMask = e.target === e.currentTarget;
  }

  function handleMaskMouseUp(e: MouseEvent) {
    // 只有按下和抬起都在蒙层上，才执行关闭
    if (isMouseDownOnMask && e.target === e.currentTarget) {
      closeFn();
    }
    isMouseDownOnMask = false;
  }

  return {
    handleMaskMouseDown,
    handleMaskMouseUp
  };
}
