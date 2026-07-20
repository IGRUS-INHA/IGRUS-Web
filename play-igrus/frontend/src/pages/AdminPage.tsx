import { useCallback, useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import {
  approveProject,
  fetchAdminProjects,
  imageSrc,
  rejectProject,
  type Project,
} from "../api/client";
import RequireLogin from "../components/RequireLogin";

export default function AdminPage() {
  return (
    <RequireLogin staff>
      <ReviewQueue />
    </RequireLogin>
  );
}

const TABS = [
  { key: "pending", label: "대기" },
  { key: "approved", label: "승인" },
  { key: "rejected", label: "반려" },
] as const;

function ReviewQueue() {
  const [tab, setTab] = useState<string>("pending");
  const [items, setItems] = useState<Project[] | null>(null);

  const reload = useCallback(() => {
    setItems(null);
    fetchAdminProjects(tab)
      .then(setItems)
      .catch(() => setItems([]));
  }, [tab]);

  useEffect(reload, [reload]);

  return (
    <div className={css({ maxW: "lg", mx: "auto" })}>
      <h1 className={css({ fontSize: "2xl", fontWeight: "800", mb: "4" })}>검수</h1>

      <div className={flex({ gap: "2", mb: "4" })}>
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setTab(t.key)}
            className={css({
              px: "3.5",
              py: "1.5",
              rounded: "full",
              fontSize: "sm",
              fontWeight: "600",
              cursor: "pointer",
              bg: tab === t.key ? "gray.900" : "white",
              color: tab === t.key ? "white" : "gray.600",
              border: "1px solid",
              borderColor: tab === t.key ? "gray.900" : "gray.200",
            })}
          >
            {t.label}
          </button>
        ))}
      </div>

      {items === null ? (
        <p className={emptyStyle}>불러오는 중…</p>
      ) : items.length === 0 ? (
        <p className={emptyStyle}>비어 있어요</p>
      ) : (
        <ul className={flex({ direction: "column", gap: "4" })}>
          {items.map((p) => (
            <ReviewCard
              key={`${p.id}-${p.version}`}
              project={p}
              showActions={tab === "pending"}
              onDone={reload}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

function ReviewCard({
  project: p,
  showActions,
  onDone,
}: {
  project: Project;
  showActions: boolean;
  onDone: () => void;
}) {
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);

  const act = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await fn();
      onDone();
    } finally {
      setBusy(false);
    }
  };

  return (
    <li
      className={css({
        bg: "white",
        border: "1px solid",
        borderColor: "gray.200",
        rounded: "xl",
        p: "4",
      })}
    >
      <div className={flex({ align: "center", gap: "2", mb: "1" })}>
        <p className={css({ fontWeight: "800", flex: 1, minW: 0, truncate: true })}>{p.title}</p>
        {/* 버전 이력 — v2 이상 대기 건은 승인작의 수정 요청 */}
        <span
          className={css({
            flexShrink: 0,
            fontSize: "xs",
            fontWeight: "700",
            px: "1.5",
            py: "0.5",
            rounded: "md",
            bg: "indigo.100",
            color: "indigo.700",
          })}
        >
          v{p.version}
        </span>
        <span className={css({ fontSize: "xs", color: "gray.500", flexShrink: 0 })}>
          {p.category}
        </span>
      </div>
      <p className={css({ fontSize: "sm", color: "gray.500" })}>
        {p.author} · {new Date(p.createdAt).toLocaleDateString("ko-KR")}
      </p>
      <p className={css({ fontSize: "sm", mt: "1" })}>{p.description}</p>
      <a
        href={p.redirectUrl}
        target="_blank"
        rel="noopener noreferrer"
        className={css({ fontSize: "sm", color: "indigo.600", wordBreak: "break-all" })}
      >
        {p.redirectUrl}
      </a>

      {(p.thumbnailUrl || p.bannerUrl) && (
        <div className={flex({ gap: "2", mt: "2" })}>
          {p.thumbnailUrl && (
            <img
              src={imageSrc(p.thumbnailUrl)}
              alt="썸네일"
              className={css({ w: "20", h: "20", objectFit: "cover", rounded: "lg" })}
            />
          )}
          {p.bannerUrl && (
            <img
              src={imageSrc(p.bannerUrl)}
              alt="배너"
              className={css({ h: "20", maxW: "48", objectFit: "cover", rounded: "lg" })}
            />
          )}
        </div>
      )}

      {p.body && (
        <details className={css({ mt: "2", fontSize: "sm" })}>
          <summary className={css({ cursor: "pointer", color: "gray.500" })}>본문 보기</summary>
          <div className={css({ mt: "2", p: "3", bg: "gray.50", rounded: "lg" })}>
            <ReactMarkdown>{p.body}</ReactMarkdown>
          </div>
        </details>
      )}

      {p.status === "rejected" && p.rejectReason && (
        <p className={css({ fontSize: "sm", color: "red.500", mt: "2" })}>
          반려 사유: {p.rejectReason}
        </p>
      )}
      {p.reviewerName && (
        <p className={css({ fontSize: "xs", color: "gray.400", mt: "1" })}>
          처리: {p.reviewerName}
        </p>
      )}

      {showActions && (
        <div className={css({ mt: "3" })}>
          {rejecting ? (
            <div className={flex({ gap: "2" })}>
              <input
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="반려 사유 (선택)"
                className={css({
                  flex: 1,
                  border: "1px solid",
                  borderColor: "gray.300",
                  rounded: "lg",
                  px: "3",
                  py: "2",
                  fontSize: "sm",
                })}
              />
              <button
                type="button"
                disabled={busy}
                onClick={() => void act(() => rejectProject(p.id, reason))}
                className={dangerBtn}
              >
                반려 확정
              </button>
            </div>
          ) : (
            <div className={flex({ gap: "2" })}>
              <button
                type="button"
                disabled={busy}
                onClick={() => void act(() => approveProject(p.id))}
                className={css({
                  flex: 1,
                  bg: "indigo.600",
                  color: "white",
                  py: "2.5",
                  rounded: "lg",
                  fontWeight: "700",
                  cursor: "pointer",
                  _disabled: { opacity: 0.6 },
                })}
              >
                승인
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={() => setRejecting(true)}
                className={dangerBtn}
              >
                반려
              </button>
            </div>
          )}
        </div>
      )}
    </li>
  );
}

const dangerBtn = css({
  px: "4",
  py: "2.5",
  rounded: "lg",
  fontWeight: "700",
  cursor: "pointer",
  bg: "red.50",
  color: "red.600",
  border: "1px solid",
  borderColor: "red.200",
  _disabled: { opacity: 0.6 },
});

const emptyStyle = css({ textAlign: "center", color: "gray.400", py: "20", fontSize: "sm" });
