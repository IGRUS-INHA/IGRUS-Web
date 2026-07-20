/** 분류별 팔레트 색 — 플레이스홀더/뱃지에 쓴다 */
const PALETTE = ["#6366f1", "#ec4899", "#f59e0b", "#10b981", "#06b6d4", "#8b5cf6"];

export const categoryColor = (category: string) =>
  PALETTE[
    [...category].reduce((acc, ch) => acc + (ch.codePointAt(0) ?? 0), 0) % PALETTE.length
  ];

/** 서버 고정 분류 — backend/projects.go 의 allowedCategories 와 동기화 */
export const CATEGORIES = ["게임", "앱"];
