import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import {
  ApiError,
  fetchMine,
  fetchMyProfile,
  imageSrc,
  updateMyProfile,
  type ProfileLink,
  type Project,
} from "../api/client";
import RequireLogin from "../components/RequireLogin";
import { categoryColor } from "../components/category";
import { field, input, label } from "../components/formStyles";

export default function MyPage() {
  return (
    <RequireLogin>
      <div className={css({ maxW: "lg", mx: "auto" })}>
        <ProfileEditor />
        <MyList />
      </div>
    </RequireLogin>
  );
}

/** 공개 프로필 편집 — 닉네임/자기소개/링크. 기존 users 테이블(igrus API)에 저장된다. */
function ProfileEditor() {
  const [loaded, setLoaded] = useState(false);
  const [nickname, setNickname] = useState("");
  const [introduction, setIntroduction] = useState("");
  const [links, setLinks] = useState<ProfileLink[]>([]);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ text: string; ok: boolean }>();

  useEffect(() => {
    fetchMyProfile()
      .then((p) => {
        setNickname(p.nickname ?? "");
        setIntroduction(p.introduction ?? "");
        setLinks(p.links ?? []);
      })
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, []);

  const setLink = (index: number, patch: Partial<ProfileLink>) =>
    setLinks((prev) => prev.map((l, i) => (i === index ? { ...l, ...patch } : l)));

  const save = async () => {
    const cleaned = links.filter((l) => l.label.trim() || l.url.trim()); // 빈 행은 버린다
    if (cleaned.some((l) => !l.label.trim() || !/^https?:\/\//.test(l.url.trim()))) {
      setMessage({ text: "링크는 라벨과 http(s):// 주소를 모두 입력해야 해요", ok: false });
      return;
    }
    setSaving(true);
    setMessage(undefined);
    try {
      await updateMyProfile({
        nickname: nickname.trim() || undefined,
        introduction: introduction.trim() || undefined,
        links: cleaned.map((l) => ({ label: l.label.trim(), url: l.url.trim() })),
      });
      setLinks(cleaned);
      setMessage({ text: "저장됐어요", ok: true });
    } catch (e) {
      setMessage({
        text: e instanceof ApiError ? e.message : "저장에 실패했어요",
        ok: false,
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <section
      className={css({
        bg: "white",
        border: "1px solid",
        borderColor: "gray.200",
        rounded: "xl",
        p: "4",
        mb: "8",
      })}
    >
      <h1 className={css({ fontSize: "2xl", fontWeight: "800", mb: "1" })}>내 프로필</h1>
      <p className={css({ fontSize: "sm", color: "gray.500", mb: "4" })}>
        작품에 표시되는 공개 프로필이에요. 닉네임을 정하면 이름 대신 닉네임만 공개돼요.
      </p>

      {!loaded ? (
        <p className={css({ color: "gray.400", fontSize: "sm", py: "4" })}>불러오는 중…</p>
      ) : (
        <div className={flex({ direction: "column", gap: "4" })}>
          <div className={field}>
            <label htmlFor="nickname" className={label}>
              닉네임
            </label>
            <input
              id="nickname"
              className={input}
              maxLength={50}
              placeholder="비우면 이름으로 표시돼요"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
            />
          </div>

          <div className={field}>
            <label htmlFor="introduction" className={label}>
              자기소개
            </label>
            <textarea
              id="introduction"
              rows={3}
              maxLength={1000}
              placeholder="나를 소개해 보세요"
              className={`${input} ${css({ resize: "vertical" })}`}
              value={introduction}
              onChange={(e) => setIntroduction(e.target.value)}
            />
          </div>

          <div className={field}>
            <span className={label}>링크</span>
            <div className={flex({ direction: "column", gap: "2" })}>
              {links.map((link, i) => (
                // 저장 전 임시 행이라 안정적인 id 가 없다 — index key 로 충분
                <div key={i} className={flex({ gap: "2", align: "center" })}>
                  <input
                    aria-label="링크 라벨"
                    className={`${input} ${css({ w: "28", flexShrink: 0 })}`}
                    maxLength={30}
                    placeholder="github"
                    value={link.label}
                    onChange={(e) => setLink(i, { label: e.target.value })}
                  />
                  <input
                    aria-label="링크 주소"
                    className={input}
                    placeholder="https://github.com/username"
                    value={link.url}
                    onChange={(e) => setLink(i, { url: e.target.value })}
                  />
                  <button
                    type="button"
                    aria-label="링크 삭제"
                    onClick={() => setLinks((prev) => prev.filter((_, j) => j !== i))}
                    className={css({
                      flexShrink: 0,
                      px: "2",
                      py: "2",
                      color: "gray.400",
                      cursor: "pointer",
                      _hover: { color: "red.500" },
                    })}
                  >
                    🗑
                  </button>
                </div>
              ))}
              {links.length < 10 && (
                <button
                  type="button"
                  onClick={() => setLinks((prev) => [...prev, { label: "", url: "" }])}
                  className={css({
                    alignSelf: "flex-start",
                    fontSize: "sm",
                    fontWeight: "600",
                    color: "indigo.600",
                    cursor: "pointer",
                    _hover: { textDecoration: "underline" },
                  })}
                >
                  + 링크 추가
                </button>
              )}
            </div>
          </div>

          {message && (
            <p
              className={css({
                fontSize: "xs",
                color: message.ok ? "green.600" : "red.500",
              })}
            >
              {message.text}
            </p>
          )}

          <button
            type="button"
            onClick={save}
            disabled={saving}
            className={css({
              alignSelf: "flex-end",
              bg: "indigo.600",
              color: "white",
              px: "5",
              py: "2",
              rounded: "lg",
              fontSize: "sm",
              fontWeight: "700",
              cursor: "pointer",
              _hover: { bg: "indigo.700" },
              _disabled: { opacity: 0.6, cursor: "default" },
            })}
          >
            {saving ? "저장 중…" : "저장"}
          </button>
        </div>
      )}
    </section>
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
    <div>
      <h2 className={css({ fontSize: "2xl", fontWeight: "800", mb: "5" })}>내 작품</h2>

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
