/** 분류는 자유 문자열 — 해시로 팔레트 색을 골라 플레이스홀더/뱃지에 쓴다 */
const PALETTE = ["#6366f1", "#ec4899", "#f59e0b", "#10b981", "#06b6d4", "#8b5cf6"];

export const categoryColor = (category: string) =>
  PALETTE[
    [...category].reduce((acc, ch) => acc + (ch.codePointAt(0) ?? 0), 0) % PALETTE.length
  ];

/** 제출 폼의 제안 목록 — 서버는 자유 문자열을 받는다 (스펙: enum ㄴㄴ) */
export const SUGGESTED_CATEGORIES = ["게임", "앱", "웹", "기타"];
