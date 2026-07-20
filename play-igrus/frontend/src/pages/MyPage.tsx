import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { fetchMine, imageSrc, type Project } from "../api/client";
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
                <Link
                  to={`/edit/${p.id}`}
                  className={css({
                    alignSelf: "center",
                    flexShrink: 0,
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
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

const emptyStyle = css({ textAlign: "center", color: "gray.400", py: "20", fontSize: "sm" });
