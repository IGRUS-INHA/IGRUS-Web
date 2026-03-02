import type { PasswordSignupRequestInterestsItem } from "@/api/model/models/passwordSignupRequestInterestsItem";

export const INTEREST_TITLE = "관심 분야를 모두 체크해주세요.";

export const interestOptions = [
  "웹 (프론트엔드)",
  "웹 (백엔드)",
  "앱",
  "해킹/보안",
  "디자인 (UI/UX)",
  "디자인 (UI/UX 외)",
  "AI",
  "Cloud",
  "게임",
] as const;

export const interestToEnum: Record<
  string,
  PasswordSignupRequestInterestsItem
> = {
  "웹 (프론트엔드)": "WEB_FRONTEND",
  "웹 (백엔드)": "WEB_BACKEND",
  앱: "APP",
  "해킹/보안": "SECURITY",
  "디자인 (UI/UX)": "UI_UX_DESIGN",
  "디자인 (UI/UX 외)": "OTHER_DESIGN",
  AI: "AI",
  Cloud: "CLOUD",
  게임: "GAME",
  기타: "OTHER",
};
