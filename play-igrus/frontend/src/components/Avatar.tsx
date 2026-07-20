import { flex } from "styled-system/patterns";

/** 프로필 사진 없을 때 공통으로 쓰는 회색 사람 실루엣 (아바타 자리표시자) */
export function PersonIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" width="60%" height="60%" aria-hidden>
      <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4.42 0-8 2.24-8 5v3h16v-3c0-2.76-3.58-5-8-5Z" />
    </svg>
  );
}

interface Props {
  /** 한 변 크기 (panda 사이즈 토큰) */
  size: string;
  /** 모서리 — 원형은 "full", 사각형은 "xl" 등 (기본 원형) */
  rounded?: string;
}

/** 회색 배경 + 사람 실루엣. 사진 미등록 프로필에 어디서나 동일하게 쓴다. */
export default function AvatarPlaceholder({ size, rounded = "full" }: Props) {
  return (
    <div
      className={flex({
        w: size,
        h: size,
        flexShrink: 0,
        align: "center",
        justify: "center",
        rounded,
        bg: "gray.200",
        color: "gray.500",
      })}
    >
      <PersonIcon />
    </div>
  );
}
