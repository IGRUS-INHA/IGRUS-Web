import { useEffect, useState } from "react";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import {
  ApiError,
  fetchMyProfile,
  updateMyProfile,
  type ProfileLink,
} from "../api/client";
import RequireLogin from "../components/RequireLogin";
import { field, input, label } from "../components/formStyles";

export default function ProfilePage() {
  return (
    <RequireLogin>
      <div className={css({ maxW: "lg", mx: "auto" })}>
        <ProfileEditor />
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
      })}
    >
      <h1 className={css({ fontSize: "2xl", fontWeight: "800", mb: "1" })}>내 정보</h1>
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
