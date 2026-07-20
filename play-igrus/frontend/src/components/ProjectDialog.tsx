import { useEffect, useRef } from "react";
import ReactMarkdown from "react-markdown";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { clickProject, imageSrc, type Project } from "../api/client";
import { useScrollLock } from "../hooks/useScrollLock";
import CloseButton from "./CloseButton";
import { categoryColor } from "./category";

interface Props {
  project: Project | null;
  onClose: () => void;
  /** 작성자 탭 → 작성자 프로필 다이얼로그 열기 */
  onAuthor?: () => void;
}

/** 작품 상세 — 모바일은 하단 바텀시트, 데스크톱은 다이얼로그 (개발자 시트와 같은 감성) */
export default function ProjectDialog({ project, onClose, onAuthor }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (project && !dialog.open) dialog.showModal();
    if (!project && dialog.open) dialog.close();
  }, [project]);

  useScrollLock(project != null); // 시트 열린 동안 배경 스크롤 잠금

  if (!project) return null;

  // 배너 없으면 흰색.
  const banner = imageSrc(project.bannerUrl);

  const go = () => {
    if (!project.redirectUrl) return;
    void clickProject(project.id).catch(() => {}); // 집계 실패해도 이동은 막지 않는다
    window.open(project.redirectUrl, "_blank", "noopener,noreferrer");
  };

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose(); // 바깥(backdrop) 탭으로 닫기
      }}
      className={css({
        p: 0,
        border: "none",
        // 모바일: 하단에 붙는 바텀시트 / 데스크톱: 중앙 다이얼로그
        mx: "auto",
        mt: "auto",
        mb: { base: 0, sm: "auto" },
        rounded: { base: "20px 20px 0 0", sm: "3xl" },
        w: { base: "100vw", sm: "min(640px, 92vw)" },
        h: "auto",
        maxW: { base: "100vw", sm: "min(640px, 92vw)" },
        maxH: { base: "85dvh", sm: "85dvh" },
        overflow: "hidden",
        animation: { base: "sheet-up 0.28s cubic-bezier(0.32, 0.72, 0, 1)", sm: "none" },
        _backdrop: { bg: "black/60" },
      })}
    >
      <div className={flex({ pos: "relative", direction: "column", h: "full", maxH: "inherit", bg: "white" })}>
        {/* X — 개발자 시트와 같은 시트 최상단 코너 (배너 위) */}
        <CloseButton onClick={onClose} />

        {/* 배너 — 이전처럼 full-bleed (패딩·라운드 없음) */}
        {banner && (
          <img
            src={banner}
            alt=""
            className={css({ flexShrink: 0, display: "block", w: "full", h: { base: "40", sm: "48" }, objectFit: "cover" })}
          />
        )}

        {/* 제목 헤더 */}
        <div
          className={css({
            flexShrink: 0,
            px: "6",
            pt: banner ? "5" : "7",
            pb: "5",
          })}
        >
          <h2 className={css({ fontSize: "2xl", fontWeight: "800", letterSpacing: "tight", pr: "10" })}>
            {project.title}
          </h2>

          {/* 분류·작성자 칩 (개발자 시트 링크 칩과 동일 감성) */}
          <div className={flex({ align: "center", gap: "2", mt: "3", wrap: "wrap" })}>
            <span
              className={css({
                flexShrink: 0,
                fontSize: "xs",
                fontWeight: "700",
                color: "white",
                px: "2.5",
                py: "1",
                rounded: "full",
              })}
              style={{ background: categoryColor(project.category) }}
            >
              {project.category}
            </span>
            <button
              type="button"
              onClick={onAuthor}
              className={css({
                maxW: "full",
                fontSize: "xs",
                fontWeight: "700",
                px: "3",
                py: "1",
                rounded: "full",
                bg: "white",
                color: "gray.700",
                border: "1px solid",
                borderColor: "gray.200",
                truncate: true,
                cursor: "pointer",
                _hover: { borderColor: "indigo.400", color: "indigo.600" },
              })}
            >
              {project.author}
            </button>
          </div>
        </div>

        {/* 소개 (마크다운, 스크롤) */}
        <div className={css({ flex: 1, minH: 0, overflowY: "auto", px: "6", pb: "6" })}>
          <h3
            className={css({
              fontSize: "xs",
              fontWeight: "800",
              color: "gray.400",
              letterSpacing: "wider",
              pt: "2",
              pb: "3",
              pos: "sticky",
              top: 0,
              bg: "white",
            })}
          >
            소개
          </h3>
          <div className={markdownStyle}>
            <ReactMarkdown>{project.body ?? ""}</ReactMarkdown>
          </div>
        </div>

        {/* 이동하기 */}
        <div className={css({ flexShrink: 0, p: "4" })}>
          <button
            type="button"
            onClick={go}
            className={css({
              w: "full",
              bg: "indigo.600",
              color: "white",
              py: "3.5",
              rounded: "xl",
              fontSize: "md",
              fontWeight: "700",
              cursor: "pointer",
              _hover: { bg: "indigo.700" },
              _active: { transform: "scale(0.98)" },
            })}
          >
            이동하기
          </button>
        </div>
      </div>
    </dialog>
  );
}

const markdownStyle = css({
  fontSize: "sm",
  lineHeight: "relaxed",
  color: "gray.800",
  "& h1": { fontSize: "xl", fontWeight: "800", mt: "5", mb: "2" },
  "& h2": { fontSize: "lg", fontWeight: "700", mt: "4", mb: "2" },
  "& h3": { fontSize: "md", fontWeight: "700", mt: "3", mb: "1.5" },
  "& p": { my: "2" },
  "& ul, & ol": { pl: "5", my: "2" },
  "& ul": { listStyleType: "disc" },
  "& ol": { listStyleType: "decimal" },
  "& a": { color: "indigo.600", textDecoration: "underline" },
  "& code": { bg: "gray.100", px: "1", rounded: "sm", fontSize: "xs" },
  "& pre": { bg: "gray.900", color: "gray.100", p: "3", rounded: "lg", overflowX: "auto", my: "3" },
  "& pre code": { bg: "transparent", p: 0 },
  "& img": { maxW: "full", rounded: "lg", my: "3" },
  "& blockquote": { borderLeft: "3px solid", borderColor: "gray.300", pl: "3", color: "gray.500", my: "3" },
});
