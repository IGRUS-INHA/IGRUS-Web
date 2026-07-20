import { useEffect, useState } from "react";
import { css } from "styled-system/css";
import { flex, grid } from "styled-system/patterns";
import { fetchProject, fetchProjects, imageSrc, type Project, type SortKey } from "../api/client";
import AuthorDialog from "../components/AuthorDialog";
import ProjectDialog from "../components/ProjectDialog";
import { categoryColor } from "../components/category";

const SORTS: { key: SortKey; label: string }[] = [
  { key: "popular", label: "인기순" },
  { key: "recent", label: "최신순" },
];

export default function HomePage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [sort, setSort] = useState<SortKey>("popular");
  const [selected, setSelected] = useState<Project | null>(null);
  const [authorFor, setAuthorFor] = useState<number>(); // 작성자 다이얼로그 대상 프로젝트 id
  const [loading, setLoading] = useState(true);

  // 정렬은 서버가 한다 (클릭수/score 는 외부 비공개라 클라이언트 정렬 불가)
  useEffect(() => {
    setLoading(true);
    fetchProjects(sort)
      .then(setProjects)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [sort]);

  const open = (p: Project) => {
    setSelected(p); // 목록 데이터로 즉시 열고
    fetchProject(p.id)
      .then(setSelected) // 본문/배너는 도착하면 채운다
      .catch(() => {});
  };

  const chip = (active: boolean) =>
    css({
      flexShrink: 0,
      px: "3.5",
      py: "1.5",
      rounded: "full",
      fontSize: "sm",
      fontWeight: "600",
      cursor: "pointer",
      bg: active ? "gray.900" : "white",
      color: active ? "white" : "gray.600",
      border: "1px solid",
      borderColor: active ? "gray.900" : "gray.200",
    });

  return (
    <>
      {/* 정렬 칩 (분류 필터는 제거 — 스펙) */}
      <div className={flex({ gap: "2", align: "center", overflowX: "auto", pb: "3", scrollbar: "hidden" })}>
        {SORTS.map((s) => (
          <button key={s.key} type="button" onClick={() => setSort(s.key)} className={chip(sort === s.key)}>
            {s.label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className={emptyStyle}>불러오는 중…</p>
      ) : projects.length === 0 ? (
        <p className={emptyStyle}>아직 등록된 작품이 없어요</p>
      ) : (
        <div className={grid({ columns: { base: 2, sm: 3, md: 4 }, gap: { base: "3", sm: "4" } })}>
          {projects.map((p) => (
            /* 작성자 버튼이 안에 있어 button 중첩을 피하려고 div+onClick (HTML 유효성) */
            <div
              key={p.id}
              onClick={() => open(p)}
              className={css({
                textAlign: "left",
                cursor: "pointer",
                bg: "white",
                rounded: "xl",
                overflow: "hidden",
                border: "1px solid",
                borderColor: "gray.200",
                transition: "transform 0.1s",
                _active: { transform: "scale(0.97)" },
              })}
            >
              {/* 정방형 썸네일 (스펙) — 없으면 분류 색 플레이스홀더 */}
              <div
                className={css({ pos: "relative", aspectRatio: "1", bg: "gray.100" })}
                style={
                  p.thumbnailUrl
                    ? undefined
                    : { background: `linear-gradient(135deg, ${categoryColor(p.category)}, #1e1b4b)` }
                }
              >
                {/* 분류 뱃지 (앱/게임) */}
                <span
                  className={css({
                    pos: "absolute",
                    top: "2",
                    left: "2",
                    zIndex: 1,
                    fontSize: "xs",
                    fontWeight: "700",
                    color: "white",
                    px: "2",
                    py: "0.5",
                    rounded: "full",
                  })}
                  style={{ background: categoryColor(p.category) }}
                >
                  {p.category}
                </span>
                {p.thumbnailUrl ? (
                  <img
                    src={imageSrc(p.thumbnailUrl)}
                    alt={p.title}
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
                      fontSize: "2xl",
                      fontWeight: "800",
                    })}
                  >
                    {p.title.slice(0, 1)}
                  </div>
                )}
              </div>
              <div className={css({ p: "2.5" })}>
                <p className={css({ fontSize: "sm", fontWeight: "700", truncate: true })}>
                  {p.title}
                </p>
                <p
                  className={css({
                    fontSize: "xs",
                    color: "gray.500",
                    mt: "0.5",
                    lineClamp: 2,
                    minH: "2lh",
                  })}
                >
                  {p.description}
                </p>
                {/* 작성자 (닉네임 폴백은 서버가) — 탭하면 프로필 다이얼로그 */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation(); // 카드 열림 방지
                    setAuthorFor(p.id);
                  }}
                  className={css({
                    display: "block",
                    maxW: "full",
                    fontSize: "xs",
                    color: "gray.400",
                    mt: "1",
                    truncate: true,
                    cursor: "pointer",
                    _hover: { color: "gray.600", textDecoration: "underline" },
                  })}
                >
                  {p.author}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ProjectDialog
        project={selected}
        onClose={() => setSelected(null)}
        onAuthor={() => {
          if (!selected) return;
          setAuthorFor(selected.id); // 새 바텀시트 올리고
          setSelected(null); // 기존 작품 시트는 지운다
        }}
      />
      <AuthorDialog
        projectId={authorFor}
        onClose={() => setAuthorFor(undefined)}
        onProject={(p) => {
          setAuthorFor(undefined);
          open(p);
        }}
      />
    </>
  );
}

const emptyStyle = css({ textAlign: "center", color: "gray.400", py: "20", fontSize: "sm" });
