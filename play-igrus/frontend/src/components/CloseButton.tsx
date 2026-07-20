import { css } from "styled-system/css";

/**
 * 시트·다이얼로그 공통 닫기 버튼 (개발자 시트에서 쓰던 고스트 스타일 그대로).
 * pos:absolute 이므로 밝은 헤더(그라디언트) 를 positioned 부모로 두고 그 우상단에 배치한다.
 */
export default function CloseButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="닫기"
      className={css({
        pos: "absolute",
        top: "4",
        right: "4",
        zIndex: 1,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        w: "8",
        h: "8",
        rounded: "full",
        bg: "black/5",
        color: "gray.600",
        fontSize: "sm",
        cursor: "pointer",
        _hover: { bg: "black/10" },
      })}
    >
      ✕
    </button>
  );
}
