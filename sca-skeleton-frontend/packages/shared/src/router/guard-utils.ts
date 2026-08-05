/**
 * 路由守卫共用的鉴权健壮性工具（admin / portal 共享）。
 *
 * 设计目标：区分"真正的认证失败"与"瞬时网络 / 浏览器导航中止"（如 Firefox 快速刷新），
 * 在瞬时错误时保留令牌并做有限次重试，避免把一次网络抖动误判为登录失效而误登出。
 */

/** 瞬时网络错误的静默重试次数上限。 */
export const MAX_TRANSIENT_RETRIES = 2;

/** 每次重试之间的等待间隔（毫秒）。 */
export const RETRY_DELAY_MS = 800;

/** 简单延迟。 */
export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 有界重试状态：跨同一导航周期内多次 beforeEach 调用共享，避免重触发导航导致无限循环。 */
export interface TransientRetryState {
  /** 当前导航周期内已重试的次数。 */
  count: number;
}

/** 判断是否仍允许继续重试（未超过上限）。 */
export function shouldRetryTransient(state: TransientRetryState): boolean {
  return state.count < MAX_TRANSIENT_RETRIES;
}

/** 记录一次重试。 */
export function markRetry(state: TransientRetryState): void {
  state.count += 1;
}

/** 重置重试计数（成功或最终失败时调用）。 */
export function resetRetry(state: TransientRetryState): void {
  state.count = 0;
}
