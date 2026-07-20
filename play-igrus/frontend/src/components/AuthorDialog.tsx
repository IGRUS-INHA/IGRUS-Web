import { useEffect, useRef, useState } from "react";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { fetchAuthor, imageSrc, type AuthorProfile, type Project } from "../api/client";
import { categoryColor } from "./category";

interface Props {
  /** 열려 있는 동안의 대상 프로젝트 id — undefined 면 닫힘 */
  projectId?: number;
  onClose: () => void;
  /** 작품 목록에서 작품 탭 — 이 다이얼로그를 닫고 해당 작품을 연다 */
  onProject?: (p: Project) => void;
}

const releaseDate = (iso: string) =>
  new Date(iso).toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });

/** 개발자 프로필 — 커피챗·포트폴리오용. 닉네임/자기소개/링크 + 출시작 목록 */
export default function AuthorDialog({ projectId, onClose, onProject }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const [author, setAuthor] = useState<AuthorProfile | null>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (projectId !== undefined && !dialog.open) dialog.showModal();
    if (projectId === undefined && dialog.open) dialog.close();
  }, [projectId]);

  useEffect(() => {
    if (projectId === undefined) return;
    setAuthor(null);
    fetchAuthor(projectId)
      .then(setAuthor)
      .catch(() => {});
  }, [projectId]);

  if (projectId === undefined) return null;

  const projects = author?.projects ?? [];

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose(); // 바깥 탭으로 닫기
      }}
      className={css({
        m: "auto",
        p: 0,
        border: "none",
        rounded: { base: "none", sm: "3xl" },
        w: { base: "100vw", sm: "min(480px, 92vw)" },
        h: { base: "100dvh", sm: "auto" },
        maxW: { base: "100vw", sm: "min(480px, 92vw)" },
        maxH: { base: "100dvh", sm: "88dvh" },
        overflow: "hidden",
        _backdrop: { bg: "black/60" },
      })}
    >
      <div className={flex({ direction: "column", h: "full", maxH: "inherit", bg: "white" })}>
        {author === null ? (
          <p className={css({ textAlign: "center", color: "gray.400", py: "20", fontSize: "sm" })}>
            불러오는 중…
          </p>
        ) : (
          <>
            {/* 프로필 헤더 */}
            <div
              className={css({
                flexShrink: 0,
                px: "6",
                pt: "7",
                pb: "5",
                bgGradient: "to-b",
                gradientFrom: "indigo.50",
                gradientTo: "white",
              })}
            >
              <button
                type="button"
                onClick={onClose}
                aria-label="닫기"
                className={css({
                  pos: "absolute",
                  top: "4",
                  right: "4",
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

              {author.avatarUrl ? (
                <img
                  src={imageSrc(author.avatarUrl)}
                  alt=""
                  className={css({ w: "16", h: "16", rounded: "2xl", objectFit: "cover" })}
                />
              ) : (
                <div
                  className={flex({
                    w: "16",
                    h: "16",
                    align: "center",
                    justify: "center",
                    rounded: "2xl",
                    bg: "indigo.600",
                    color: "white",
                    fontSize: "2xl",
                    fontWeight: "800",
                  })}
                >
                  {author.displayName.slice(0, 1)}
                </div>
              )}

              <h2 className={css({ mt: "3", fontSize: "2xl", fontWeight: "800", letterSpacing: "tight" })}>
                {author.displayName}
              </h2>
              <p className={css({ fontSize: "xs", fontWeight: "700", color: "indigo.500", mt: "0.5" })}>
                개발자 · 출시작 {projects.length}
              </p>

              {author.introduction && (
                <p
                  className={css({
                    mt: "3",
                    fontSize: "sm",
                    color: "gray.700",
                    lineHeight: "relaxed",
                    whiteSpace: "pre-wrap",
                  })}
                >
                  {author.introduction}
                </p>
              )}

              {author.links.length > 0 && (
                <div className={flex({ wrap: "wrap", gap: "2", mt: "4" })}>
                  {author.links.map((link) => (
                    <a
                      key={`${link.label}-${link.url}`}
                      href={link.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={css({
                        fontSize: "xs",
                        fontWeight: "700",
                        px: "3",
                        py: "1.5",
                        rounded: "full",
                        bg: "white",
                        color: "gray.700",
                        border: "1px solid",
                        borderColor: "gray.200",
                        _hover: { borderColor: "indigo.400", color: "indigo.600" },
                      })}
                    >
                      {link.label} ↗
                    </a>
                  ))}
                </div>
              )}
            </div>

            {/* 출시작 */}
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
                출시한 작품
              </h3>

              {projects.length === 0 ? (
                <p className={css({ fontSize: "sm", color: "gray.400", py: "6", textAlign: "center" })}>
                  아직 공개된 작품이 없습니다
                </p>
              ) : (
                <ul className={flex({ direction: "column", gap: "2" })}>
                  {projects.map((p) => (
                    <li key={p.id}>
                      <button
                        type="button"
                        onClick={() => onProject?.(p)}
                        className={flex({
                          w: "full",
                          align: "center",
                          gap: "3",
                          p: "2",
                          rounded: "xl",
                          textAlign: "left",
                          cursor: "pointer",
                          _hover: { bg: "gray.50" },
                        })}
                      >
                        <div
                          className={css({
                            w: "14",
                            h: "14",
                            flexShrink: 0,
                            rounded: "xl",
                            overflow: "hidden",
                            bg: "gray.100",
                          })}
                          style={
                            p.thumbnailUrl
                              ? undefined
                              : { background: `linear-gradient(135deg, ${categoryColor(p.category)}, #1e1b4b)` }
                          }
                        >
                          {p.thumbnailUrl ? (
                            <img
                              src={imageSrc(p.thumbnailUrl)}
                              alt=""
                              loading="lazy"
                              className={css({ w: "full", h: "full", objectFit: "cover" })}
                            />
                          ) : (
                            <div
                              className={flex({
                                w: "full",
                                h: "full",
                                align: "center",
                                justify: "center",
                                color: "white",
                                fontWeight: "800",
                              })}
                            >
                              {p.title.slice(0, 1)}
                            </div>
                          )}
                        </div>

                        <div className={css({ flex: 1, minW: 0 })}>
                          <div className={flex({ align: "center", gap: "2" })}>
                            <span className={css({ fontSize: "sm", fontWeight: "700", truncate: true })}>
                              {p.title}
                            </span>
                            <span
                              className={css({
                                flexShrink: 0,
                                fontSize: "2xs",
                                fontWeight: "700",
                                color: "white",
                                px: "1.5",
                                py: "0.5",
                                rounded: "full",
                              })}
                              style={{ background: categoryColor(p.category) }}
                            >
                              {p.category}
                            </span>
                          </div>
                          <p className={css({ mt: "0.5", fontSize: "xs", color: "gray.400" })}>
                            {releaseDate(p.createdAt)} 출시
                          </p>
                        </div>

                        <span className={css({ flexShrink: 0, color: "gray.300", fontSize: "sm" })}>›</span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </>
        )}
      </div>
    </dialog>
  );
}
