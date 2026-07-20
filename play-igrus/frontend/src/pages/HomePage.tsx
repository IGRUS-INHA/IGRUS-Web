import { useEffect, useMemo, useState } from "react";
import { css } from "styled-system/css";
import { flex, grid } from "styled-system/patterns";
import { fetchProject, fetchProjects, imageSrc, type Project } from "../api/client";
import ProjectDialog from "../components/ProjectDialog";
import { categoryColor } from "../components/category";

export default function HomePage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [category, setCategory] = useState("");
  const [selected, setSelected] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProjects()
      .then(setProjects)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(
    () => [...new Set(projects.map((p) => p.category))],
    [projects],
  );
  const visible = category ? projects.filter((p) => p.category === category) : projects;

  const open = (p: Project) => {
    setSelected(p); // 목록 데이터로 즉시 열고
    fetchProject(p.id)
      .then(setSelected) // 본문/배너는 도착하면 채운다
      .catch(() => {});
  };

  return (
    <>
      {/* 분류 필터 칩 */}
      {categories.length > 1 && (
        <div className={flex({ gap: "2", overflowX: "auto", pb: "3", scrollbar: "hidden" })}>
          {["", ...categories].map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => setCategory(c)}
              className={css({
                flexShrink: 0,
                px: "3.5",
                py: "1.5",
                rounded: "full",
                fontSize: "sm",
                fontWeight: "600",
                cursor: "pointer",
                bg: category === c ? "gray.900" : "white",
                color: category === c ? "white" : "gray.600",
                border: "1px solid",
                borderColor: category === c ? "gray.900" : "gray.200",
              })}
            >
              {c || "전체"}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <p className={emptyStyle}>불러오는 중…</p>
      ) : visible.length === 0 ? (
        <p className={emptyStyle}>아직 등록된 작품이 없어요</p>
      ) : (
        <div className={grid({ columns: { base: 2, sm: 3, md: 4 }, gap: { base: "3", sm: "4" } })}>
          {visible.map((p) => (
            <button
              key={p.id}
              type="button"
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
                className={css({ aspectRatio: "1", bg: "gray.100" })}
                style={
                  p.thumbnailUrl
                    ? undefined
                    : { background: `linear-gradient(135deg, ${categoryColor(p.category)}, #1e1b4b)` }
                }
              >
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
              </div>
            </button>
          ))}
        </div>
      )}

      <ProjectDialog project={selected} onClose={() => setSelected(null)} />
    </>
  );
}

const emptyStyle = css({ textAlign: "center", color: "gray.400", py: "20", fontSize: "sm" });
