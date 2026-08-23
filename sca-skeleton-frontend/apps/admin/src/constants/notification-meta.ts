/**
 * 通知公告元信息映射（类型图标 / 类型底色 / 级别徽章色）。
 *
 * 由 NotificationPanel（铃铛弹窗列表）与 NoticeDetailView（新 tab 详情页）共享，
 * 保证两处对同一条通知的视觉呈现一致。
 */

/** 类型 → 图标名 */
export const noticeTypeIconMap: Record<string, string> = {
  notice: "sym_r_campaign",
  announcement: "sym_r_newspaper",
  system: "sym_r_settings",
  other: "sym_r_info"
};

/** 类型 → 图标圆片背景色 */
export const noticeTypeColorMap: Record<string, string> = {
  notice: "#1976d2",
  announcement: "#7e57c2",
  system: "#607d8b",
  other: "#78909c"
};

/** 级别 → q-badge 颜色 */
export const noticeLevelColorMap: Record<string, string> = {
  normal: "grey-6",
  important: "orange-7",
  urgent: "red-7"
};
