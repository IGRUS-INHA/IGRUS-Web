import { useEffect, useRef } from "react";
import ReactMarkdown from "react-markdown";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { clickProject, imageSrc, type Project } from "../api/client";
import { categoryColor } from "./category";

interface Props {
  project: Project | null;
  onClose: () => void;
}

/** 작품 상세 — 모바일은 풀스크린 시트, 데스크톱은 다이얼로그 (native <dialog>) */
export default function ProjectDialog({ project, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (project && !dialog.open) dialog.showModal();
    if (!project && dialog.open) dialog.close();
  }, [project]);

  if (!project) return null;

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
        m: "auto",
        p: 0,
        border: "none",
        rounded: { base: "none", sm: "2xl" },
        w: { base: "100vw", sm: "min(640px, 92vw)" },
        h: { base: "100dvh", sm: "auto" },
        maxW: { base: "100vw", sm: "min(640px, 92vw)" },
        maxH: { base: "100dvh", sm: "85dvh" },
        overflow: "hidden",
        _backdrop: { bg: "black/60" },
      })}
    >
      <div className={flex({ direction: "column", h: "full", maxH: "inherit", bg: "white" })}>
        {/* 배너 (없으면 분류 색 그라데이션) */}
        <div
          className={css({ pos: "relative", flexShrink: 0, h: { base: "40", sm: "48" } })}
          style={
            banner
              ? undefined
              : { background: `linear-gradient(135deg, ${categoryColor(project.category)}, #1e1b4b)` }
          }
        >
          {banner && (
            <img
              src={banner}
              alt=""
              className={css({ w: "full", h: "full", objectFit: "cover" })}
            />
          )}
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className={css({
              pos: "absolute",
              top: "3",
              right: "3",
              w: "9",
              h: "9",
              rounded: "full",
              bg: "black/50",
              color: "white",
              fontSize: "lg",
              cursor: "pointer",
            })}
          >
            ✕
          </button>
        </div>

        {/* 제목/작성자 */}
        <div className={css({ px: "5", pt: "4", pb: "3", borderBottom: "1px solid", borderColor: "gray.100" })}>
          <div className={flex({ align: "center", gap: "2" })}>
            <h2 className={css({ fontSize: "xl", fontWeight: "800", flex: 1, minW: 0 })}>
              {project.title}
            </h2>
            <span
              className={css({ fontSize: "xs", fontWeight: "700", color: "white", px: "2", py: "0.5", rounded: "full" })}
              style={{ background: categoryColor(project.category) }}
            >
              {project.category}
            </span>
          </div>
          <p className={css({ fontSize: "sm", color: "gray.500", mt: "1" })}>{project.author}</p>
        </div>

        {/* 마크다운 본문 (스크롤) */}
        <div className={css({ flex: 1, overflowY: "auto", px: "5", py: "4" })}>
          <div className={markdownStyle}>
            <ReactMarkdown>{project.body ?? ""}</ReactMarkdown>
          </div>
        </div>

        {/* 이동하기 */}
        <div className={css({ flexShrink: 0, p: "4", borderTop: "1px solid", borderColor: "gray.100" })}>
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
