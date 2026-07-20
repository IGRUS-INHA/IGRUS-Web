import { css } from "styled-system/css";

export const field = css({ display: "flex", flexDirection: "column" });

export const label = css({ fontSize: "sm", fontWeight: "600", mb: "1.5" });

/** 필수 항목 표시 — 라벨 옆 붉은 뱃지 */
export const requiredMark = css({
  display: "inline-block",
  fontSize: "2xs",
  fontWeight: "700",
  color: "red.500",
  bg: "red.50",
  px: "1.5",
  py: "0.5",
  rounded: "sm",
  ml: "1.5",
  verticalAlign: "middle",
});

export const input = css({
  border: "1px solid",
  borderColor: "gray.200",
  rounded: "xl",
  px: "3.5",
  py: "2.5",
  fontSize: "md", // 16px 미만이면 iOS 사파리가 포커스 시 화면을 확대한다
  bg: "gray.50",
  transition: "border-color 0.15s, background 0.15s",
  _hover: { borderColor: "gray.300" },
  _focus: { outline: "2px solid", outlineColor: "indigo.500", borderColor: "transparent", bg: "white" },
  _placeholder: { color: "gray.400" },
});

export const primaryBtn = css({
  bg: "indigo.600",
  color: "white",
  py: "3",
  rounded: "xl",
  fontSize: "md",
  fontWeight: "700",
  cursor: "pointer",
  _hover: { bg: "indigo.700" },
  _disabled: { opacity: 0.6, cursor: "not-allowed" },
});
