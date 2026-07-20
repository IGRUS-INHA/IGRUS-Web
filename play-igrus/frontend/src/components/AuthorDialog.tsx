import { useEffect, useRef, useState } from "react";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { fetchAuthor, type AuthorProfile } from "../api/client";

interface Props {
  /** 열려 있는 동안의 대상 프로젝트 id — undefined 면 닫힘 */
  projectId?: number;
  onClose: () => void;
}

/** 작성자 프로필 다이얼로그 — 닉네임(또는 이름), 자기소개, 링크 */
export default function AuthorDialog({ projectId, onClose }: Props) {
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
        rounded: "2xl",
        w: "min(400px, 92vw)",
        maxH: "80dvh",
        overflow: "hidden",
        _backdrop: { bg: "black/60" },
      })}
    >
      <div className={css({ bg: "white", p: "6", overflowY: "auto", maxH: "inherit" })}>
        {author === null ? (
          <p className={css({ textAlign: "center", color: "gray.400", py: "8", fontSize: "sm" })}>
            불러오는 중…
          </p>
        ) : (
          <>
            <div className={flex({ align: "center", gap: "3" })}>
              {/* 아바타 — 표시 이름 첫 글자 */}
              <div
                className={flex({
                  w: "12",
                  h: "12",
                  flexShrink: 0,
                  align: "center",
                  justify: "center",
                  rounded: "full",
                  bg: "indigo.100",
                  color: "indigo.600",
                  fontSize: "xl",
                  fontWeight: "800",
                })}
              >
                {author.displayName.slice(0, 1)}
              </div>
              <h2 className={css({ fontSize: "lg", fontWeight: "800", minW: 0, truncate: true })}>
                {author.displayName}
              </h2>
            </div>

            {author.introduction && (
              <p
                className={css({
                  mt: "4",
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
              <ul className={flex({ direction: "column", gap: "2", mt: "4" })}>
                {author.links.map((link) => (
                  <li key={`${link.label}-${link.url}`}>
                    <a
                      href={link.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={flex({
                        align: "center",
                        gap: "2",
                        fontSize: "sm",
                        px: "3",
                        py: "2",
                        rounded: "lg",
                        bg: "gray.50",
                        border: "1px solid",
                        borderColor: "gray.200",
                        _hover: { bg: "gray.100" },
                      })}
                    >
                      <span className={css({ fontWeight: "700", flexShrink: 0 })}>{link.label}</span>
                      <span className={css({ color: "gray.500", truncate: true })}>{link.url}</span>
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}

        <button
          type="button"
          onClick={onClose}
          className={css({
            mt: "6",
            w: "full",
            py: "2.5",
            rounded: "xl",
            bg: "gray.100",
            color: "gray.700",
            fontSize: "sm",
            fontWeight: "700",
            cursor: "pointer",
          })}
        >
          닫기
        </button>
      </div>
    </dialog>
  );
}
