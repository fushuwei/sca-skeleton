import type { Directive } from "vue";

/**
 * v-mask-close：为抽屉蒙层绑定「点击蒙层关闭」逻辑。
 *
 * 把「点击外部关闭抽屉」的判定集中在一处，供所有本地右侧抽屉复用，
 * 避免在每个 ListView 里重复接线 mousedown/mouseup。
 *
 * 关键点（修复 Chrome 拖拽选中文字误关闭的问题）：
 * - Chrome 下即使发生拖拽/选中，`click` 事件仍会在 mousedown 与 mouseup
 *   目标的最近公共祖先上派发，导致 `@click.self` 误触发关闭；Firefox 则抑制该 click。
 * - 因此这里不使用 `click`，而是自行判定「一次真实的点击」：
 *   1. mousedown 与 mouseup 都直接落在蒙层上（target === currentTarget）；
 *   2. 指针位移小于阈值 CLICK_SLOP（排除拖选/拖拽）。
 * - 两者同时满足才调用 closeFn。
 *
 * 用法：
 *   <div class="xxx-local-drawer-mask" v-mask-close="closeXxxDrawer">...</div>
 */
type CloseFn = () => void;

/** 位移阈值(px)：从 mousedown 到 mouseup 的移动距离小于该值视为「点击」，否则视为拖拽/选中，不关闭 */
const CLICK_SLOP = 8;

interface DownState {
  /** 按下时是否直接落在蒙层上 */
  downOnMask: boolean;
  /** 按下位置（用于判断是否是拖拽） */
  startX: number;
  startY: number;
}

const downStateMap = new WeakMap<HTMLElement, DownState>();
const closeFnMap = new WeakMap<HTMLElement, CloseFn>();

function onMouseDown(e: MouseEvent) {
  const el = e.currentTarget as HTMLElement;
  downStateMap.set(el, {
    downOnMask: e.target === el,
    startX: e.clientX,
    startY: e.clientY
  });
}

function onMouseUp(e: MouseEvent) {
  const el = e.currentTarget as HTMLElement;
  const state = downStateMap.get(el);
  if (!state) {
    return;
  }
  downStateMap.delete(el);
  const moved = Math.hypot(e.clientX - state.startX, e.clientY - state.startY);
  if (state.downOnMask && e.target === el && moved < CLICK_SLOP) {
    closeFnMap.get(el)?.();
  }
}

export const vMaskClose: Directive<HTMLElement, CloseFn> = {
  mounted(el, binding) {
    closeFnMap.set(el, binding.value);
    el.addEventListener("mousedown", onMouseDown);
    el.addEventListener("mouseup", onMouseUp);
  },
  updated(el, binding) {
    // 回调引用变化时同步（通常 closeXxxDrawer 为稳定函数，不会变化）
    closeFnMap.set(el, binding.value);
  },
  unmounted(el) {
    closeFnMap.delete(el);
    downStateMap.delete(el);
    el.removeEventListener("mousedown", onMouseDown);
    el.removeEventListener("mouseup", onMouseUp);
  }
};
