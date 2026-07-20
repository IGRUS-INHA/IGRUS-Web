import { css } from "styled-system/css";

/** 모바일 바텀시트 상단 드래그 손잡이 — 끌어내려 닫을 수 있다는 어포던스 (시각용, 터치는 시트 전체가 처리) */
export default function SheetHandle() {
  return (
    <div
      className={css({
        display: { base: "block", sm: "none" },
        pos: "absolute",
        top: "3",
        left: "50%",
        transform: "translateX(-50%)",
        zIndex: 1,
        w: "16",
        h: "4px",
        rounded: "full",
        bg: "black/80",
        pointerEvents: "none",
      })}
    />
  );
}
