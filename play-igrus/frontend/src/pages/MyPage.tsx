import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { fetchMine, imageSrc, setProjectVisibility, type Project } from "../api/client";
import RequireLogin from "../components/RequireLogin";
import { categoryColor } from "../components/category";

export default function MyPage() {
  return (
    <RequireLogin>
      <MyList />
    </RequireLogin>
  );
}

const STATUS_LABEL = {
  pending: { text: "심사중", bg: "amber.100", color: "amber.700" },
  approved: { text: "승인됨", bg: "green.100", color: "green.700" },
  rejected: { text: "반려됨", bg: "red.100", color: "red.700" },
} as const;

function MyList() {
  const [items, setItems] = useState<Project[] | null>(null);
  const [confirming, setConfirming] = useState<Project | null>(null);

  useEffect(() => {
    fetchMine()
      .then(setItems)
      .catch(() => setItems([]));
  }, []);

  return (
    <div className={css({ maxW: "lg", mx: "auto" })}>
      <h1 className={css({ fontSize: "2xl", fontWeight: "800", mb: "5" })}>내 작품</h1>

      {items === null ? (
        <p className={emptyStyle}>불러오는 중…</p>
      ) : items.length === 0 ? (
        <p className={emptyStyle}>아직 출시한 작품이 없어요</p>
      ) : (
        <ul className={flex({ direction: "column", gap: "3" })}>
          {items.map((p) => {
            const s = STATUS_LABEL[p.status ?? "pending"];
            return (
              <li
                key={p.id}
                className={flex({
                  gap: "3",
                  bg: "white",
                  border: "1px solid",
                  borderColor: "gray.200",
                  rounded: "xl",
                  p: "3",
                })}
              >
                <div
                  className={css({ w: "16", h: "16", flexShrink: 0, rounded: "lg", overflow: "hidden" })}
                  style={
                    p.thumbnailUrl
                      ? undefined
                      : { background: `linear-gradient(135deg, ${categoryColor(p.category)}, #1e1b4b)` }
                  }
                >
                  {p.thumbnailUrl && (
                    <img
                      src={imageSrc(p.thumbnailUrl)}
                      alt=""
                      className={css({ w: "full", h: "full", objectFit: "cover" })}
                    />
                  )}
                </div>
                <div className={css({ flex: 1, minW: 0 })}>
                  <div className={flex({ align: "center", gap: "2" })}>
                    <p className={css({ fontWeight: "700", truncate: true })}>{p.title}</p>
                    {/* 버전 표시 — 승인작은 라이브 버전, 미승인작은 제출 버전 */}
                    <span
                      className={css({
                        flexShrink: 0,
                        fontSize: "xs",
                        fontWeight: "700",
                        px: "1.5",
                        py: "0.5",
                        rounded: "md",
                        bg: "gray.100",
                        color: "gray.600",
                      })}
                    >
                      v{p.version || (p.update?.version ?? 1)}
                    </span>
                    <span
                      className={css({
                        flexShrink: 0,
                        fontSize: "xs",
                        fontWeight: "700",
                        px: "2",
                        py: "0.5",
                        rounded: "full",
                        bg: s.bg,
                        color: s.color,
                      })}
                    >
                      {s.text}
                    </span>
                    {p.status === "approved" && p.hidden && (
                      <span
                        className={css({
                          flexShrink: 0,
                          fontSize: "xs",
                          fontWeight: "700",
                          px: "2",
                          py: "0.5",
                          rounded: "full",
                          bg: "gray.200",
                          color: "gray.600",
                        })}
                      >
                        비공개
                      </span>
                    )}
                  </div>
                  <p className={css({ fontSize: "sm", color: "gray.500", truncate: true })}>
                    {p.description}
                  </p>
                  {p.status === "rejected" && p.rejectReason && (
                    <p className={css({ fontSize: "xs", color: "red.500", mt: "0.5" })}>
                      사유: {p.rejectReason}
                    </p>
                  )}
                  {p.status === "approved" && p.update?.status === "pending" && (
                    <p className={css({ fontSize: "xs", color: "amber.600", mt: "0.5" })}>
                      v{p.update.version} 수정 심사중 — 승인 전까지 v{p.version}이 공개돼요
                    </p>
                  )}
                  {p.status === "approved" && p.update?.status === "rejected" && (
                    <p className={css({ fontSize: "xs", color: "red.500", mt: "0.5" })}>
                      v{p.update.version} 수정 반려
                      {p.update.rejectReason ? `: ${p.update.rejectReason}` : ""}
                    </p>
                  )}
                </div>
                <div className={flex({ direction: "column", gap: "1.5", alignSelf: "center", flexShrink: 0 })}>
                  <Link
                    to={`/edit/${p.id}`}
                    className={css({
                      textAlign: "center",
                      fontSize: "sm",
                      fontWeight: "600",
                      color: "indigo.600",
                      px: "2.5",
                      py: "1.5",
                      rounded: "lg",
                      border: "1px solid",
                      borderColor: "indigo.200",
                    })}
                  >
                    수정
                  </Link>
                  {/* 공개 토글은 승인작에만 — 심사중/반려작은 애초에 비공개 */}
                  {p.status === "approved" && (
                    <button
                      type="button"
                      onClick={() => setConfirming(p)}
                      className={css({
                        fontSize: "sm",
                        fontWeight: "600",
                        color: "gray.600",
                        px: "2.5",
                        py: "1.5",
                        rounded: "lg",
                        border: "1px solid",
                        borderColor: "gray.300",
                        cursor: "pointer",
                      })}
                    >
                      {p.hidden ? "공개하기" : "숨기기"}
                    </button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <VisibilityDialog
        project={confirming}
        onClose={() => setConfirming(null)}
        onDone={(updated) => {
          setItems((prev) => prev?.map((it) => (it.id === updated.id ? updated : it)) ?? prev);
          setConfirming(null);
        }}
      />
    </div>
  );
}

/** 공개/비공개 확인 다이얼로그 (native <dialog> — ProjectDialog 와 같은 패턴) */
function VisibilityDialog({
  project,
  onClose,
  onDone,
}: {
  project: Project | null;
  onClose: () => void;
  onDone: (updated: Project) => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (project && !dialog.open) dialog.showModal();
    if (!project && dialog.open) dialog.close();
    setError("");
  }, [project]);

  if (!project) return null;
  const toHidden = !project.hidden;

  const submit = () => {
    setBusy(true);
    setProjectVisibility(project.id, toHidden)
      .then(() => onDone({ ...project, hidden: toHidden }))
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "처리에 실패했습니다"))
      .finally(() => setBusy(false));
  };

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
      className={css({
        m: "auto",
        p: "6",
        border: "none",
        rounded: "2xl",
        w: "min(360px, 92vw)",
        _backdrop: { bg: "black/60" },
      })}
    >
      <p className={css({ fontWeight: "700", mb: "2" })}>
        {toHidden ? "작품을 숨길까요?" : "작품을 다시 공개할까요?"}
      </p>
      <p className={css({ fontSize: "sm", color: "gray.500", mb: "4" })}>
        {toHidden
          ? `‘${project.title}’이(가) 메인 목록에서 보이지 않게 돼요. 언제든 다시 공개할 수 있어요.`
          : `‘${project.title}’이(가) 심사 없이 바로 다시 공개돼요.`}
      </p>
      {error && <p className={css({ fontSize: "sm", color: "red.500", mb: "3" })}>{error}</p>}
      <div className={flex({ gap: "2", justify: "flex-end" })}>
        <button
          type="button"
          onClick={onClose}
          className={css({
            fontSize: "sm",
            fontWeight: "600",
            px: "3",
            py: "1.5",
            rounded: "lg",
            border: "1px solid",
            borderColor: "gray.300",
            color: "gray.600",
            cursor: "pointer",
          })}
        >
          취소
        </button>
        <button
          type="button"
          onClick={submit}
          disabled={busy}
          className={css({
            fontSize: "sm",
            fontWeight: "600",
            px: "3",
            py: "1.5",
            rounded: "lg",
            bg: toHidden ? "gray.700" : "indigo.600",
            color: "white",
            cursor: "pointer",
            _disabled: { opacity: 0.5, cursor: "default" },
          })}
        >
          {busy ? "처리 중…" : toHidden ? "숨기기" : "공개하기"}
        </button>
      </div>
    </dialog>
  );
}

const emptyStyle = css({ textAlign: "center", color: "gray.400", py: "20", fontSize: "sm" });
