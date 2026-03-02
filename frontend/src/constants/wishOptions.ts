import type { PasswordSignupRequestWishesItem } from "@/api/model/models/passwordSignupRequestWishesItem";

export const WISH_TITLE = "IGRUS에 들어오신 목적/이유가 무엇인가요?";

export const wishOptions = [
  "네트워킹 및 친목 활동",
  "스터디 메이트와 함께하는 공부",
  "프로젝트 경험 및 팀원 모집",
  "취업 · 진로 준비",
  "프로그래밍 실력 향상",
] as const;

export const wishToEnum: Record<string, PasswordSignupRequestWishesItem> = {
  "네트워킹 및 친목 활동": "NETWORKING",
  "스터디 메이트와 함께하는 공부": "STUDY",
  "프로젝트 경험 및 팀원 모집": "PROJECT",
  "취업 · 진로 준비": "CAREER",
  "프로그래밍 실력 향상": "PROGRAMMING",
};
