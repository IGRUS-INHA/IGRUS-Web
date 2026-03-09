import { useRef, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import {
  ChevronRight,
  Users,
  Code2,
  BookOpen,
  Award,
  Monitor,
  MessageSquare,
  Instagram,
} from "lucide-react";
import { useAuth } from "@/hooks";
import { cn } from "@/lib/utils";
import { useGetPinnedPostList } from "@/api/model/pinned-post/pinned-post";
import { useGetEventList } from "@/api/model/event/event";
import type {
  PinnedPostListResponse,
  EventListResponse,
} from "@/api/model/models";

// ─── Constants ─────────────────────────────────────

const CATEGORIES = [
  { id: "algo", emoji: "\u{1F9E0}", label: "알고리즘" },
  { id: "web", emoji: "\u{1F310}", label: "웹 개발" },
  { id: "ai", emoji: "\u{1F916}", label: "AI / ML" },
  { id: "hackathon", emoji: "\u{1F3C6}", label: "해커톤" },
  { id: "study", emoji: "\u{1F4DA}", label: "스터디" },
  { id: "game", emoji: "\u{1F3AE}", label: "게임 개발" },
  { id: "security", emoji: "\u{1F512}", label: "보안" },
] as const;

const ORBIT_SPEED = 0.00006; // rad/ms (~105 seconds per revolution)
const SLOT_STEP = (2 * Math.PI) / CATEGORIES.length;

const IG_THUMBNAILS = [
  { className: "ig-thumb-1", Icon: Users, label: "OT 환영회" },
  { className: "ig-thumb-2", Icon: Code2, label: "해커톤" },
  { className: "ig-thumb-3", Icon: BookOpen, label: "스터디" },
  { className: "ig-thumb-4", Icon: Award, label: "수상 소식" },
  { className: "ig-thumb-5", Icon: Monitor, label: "세미나" },
  { className: "ig-thumb-6", Icon: MessageSquare, label: "네트워킹" },
] as const;

const IG_URL = "https://instagram.com/igrus_inha";

function formatDateDot(dateStr?: string) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}

function isWithin2Days(dateStr?: string) {
  if (!dateStr) return false;
  return Date.now() - new Date(dateStr).getTime() < 2 * 24 * 60 * 60 * 1000;
}

// ─── Main ──────────────────────────────────────────

export default function HomePage() {
  const { isAuthenticated } = useAuth();

  const { data: pinnedResponse, isLoading: isPinnedLoading } =
    useGetPinnedPostList();
  const pinnedPosts = (pinnedResponse?.data ?? []) as PinnedPostListResponse[];

  const { data: eventsResponse, isLoading: isEventsLoading } = useGetEventList(
    { eventStatus: "UPCOMING" },
    { query: { enabled: __FEATURE_EVENTS__ } },
  );
  const events = ((eventsResponse?.data ?? []) as EventListResponse[]).slice(
    0,
    3,
  );

  const showSplit = __FEATURE_EVENTS__ || __FEATURE_INSTAGRAM__;

  return (
    <div>
      <HeroSection isAuthenticated={isAuthenticated} />
      <NoticeSection pinnedPosts={pinnedPosts} isLoading={isPinnedLoading} />
      {showSplit && (
        <SplitSection events={events} isEventsLoading={isEventsLoading} />
      )}
    </div>
  );
}

// ─── Hero Section ──────────────────────────────────

function HeroSection({ isAuthenticated }: { isAuthenticated: boolean }) {
  return (
    <section className="hero-section">
      <div className="hero-ring hero-ring-1" />
      <div className="hero-ring hero-ring-2" />
      <div className="hero-ring hero-ring-3" />
      <div className="relative z-[1] max-w-[1280px] mx-auto px-s6 max-md:px-[20px]">
        <div className="grid grid-cols-2 gap-s7 items-center max-lg:grid-cols-1">
          <HeroText isAuthenticated={isAuthenticated} />
          <div className="hidden md:block">
            <BubbleOrbit />
          </div>
        </div>
      </div>
    </section>
  );
}

function HeroText({ isAuthenticated }: { isAuthenticated: boolean }) {
  const revealRefs = useRef<(HTMLElement | null)[]>([]);

  useEffect(() => {
    const prefersReduced = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    const els = revealRefs.current.filter(Boolean) as HTMLElement[];

    if (prefersReduced) {
      els.forEach((el) => {
        el.classList.add("visible");
        el.style.transition = "none";
      });
    } else {
      els.forEach((el, i) => {
        el.style.transitionDelay = `${i * 0.2}s`;
      });
      const raf = requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          els.forEach((el) => el.classList.add("visible"));
        });
      });
      return () => cancelAnimationFrame(raf);
    }
  }, []);

  const setRef = (i: number) => (el: HTMLElement | null) => {
    revealRefs.current[i] = el;
  };

  return (
    <div>
      <p
        ref={setRef(0)}
        className="hero-reveal typo-c1 font-semibold text-primary uppercase tracking-[0.06em] mb-[14px] md:mb-s5"
      >
        Inha Computer Club — Since 2000
      </p>
      <h1
        ref={setRef(1)}
        className="hero-reveal hero-heading text-foreground mb-[14px] md:mb-s5"
        style={{ wordBreak: "keep-all" }}
      >
        성장과 낭만의 동아리,
        <br />
        <span className="hero-title-brand">IGRUS.</span>
      </h1>
      <p
        ref={setRef(2)}
        className="hero-reveal text-xs md:text-base text-muted-foreground leading-[1.7] max-w-[520px] mb-[14px] md:mb-s6"
      >
        인하대학교 정보통신처 직속 컴퓨터 학술 자치회.
        <br />
        26년간 실력으로 증명해온 커뮤니티입니다.
      </p>
      <div
        ref={setRef(3)}
        className="hero-reveal flex gap-s2 md:gap-s3 items-center"
      >
        {isAuthenticated ? (
          <Link
            to={__FEATURE_COMMUNITY__ ? "/board/general" : "/board/notices"}
            className="btn-hero-primary"
          >
            {__FEATURE_COMMUNITY__ ? "커뮤니티 둘러보기" : "공지사항 보기"}
          </Link>
        ) : (
          <Link to="/signup" className="btn-hero-primary">
            가입 신청
          </Link>
        )}
        <a href="#" className="btn-hero-outline">
          동아리 소개
        </a>
      </div>
    </div>
  );
}

// ─── Bubble Orbit ──────────────────────────────────

interface BubbleState {
  slot: number;
  ready: boolean;
  isDragging: boolean;
  isSnapping: boolean;
}

function BubbleOrbit() {
  const containerRef = useRef<HTMLDivElement>(null);
  const bubbleRefs = useRef<(HTMLDivElement | null)[]>([]);
  const trackRef = useRef<HTMLDivElement>(null);
  const rafRef = useRef(0);
  const stateRef = useRef({
    gAngle: 0,
    lastT: 0,
    bubbles: CATEGORIES.map(
      (_, i): BubbleState => ({
        slot: i,
        ready: false,
        isDragging: false,
        isSnapping: false,
      }),
    ),
  });
  const dragRef = useRef<{
    idx: number;
    startX: number;
    startY: number;
    origLeft: number;
    origTop: number;
  } | null>(null);

  const N = CATEGORIES.length;

  const getDims = useCallback(() => {
    const el = containerRef.current;
    if (!el) return { cx: 0, cy: 0, rx: 160, ry: 120 };
    return {
      cx: el.offsetWidth / 2,
      cy: el.offsetHeight / 2,
      rx: Math.min(el.offsetWidth * 0.44, 190),
      ry: Math.min(el.offsetHeight * 0.42, 140),
    };
  }, []);

  const updateTrack = useCallback(() => {
    const { rx, ry } = getDims();
    if (trackRef.current) {
      trackRef.current.style.width = rx * 2 + "px";
      trackRef.current.style.height = ry * 2 + "px";
    }
  }, [getDims]);

  useEffect(() => {
    const prefersReduced = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    const state = stateRef.current;
    const bubbles = bubbleRefs.current;

    // Initial placement
    updateTrack();
    const { cx, cy, rx, ry } = getDims();
    state.bubbles.forEach((st, i) => {
      const el = bubbles[i];
      if (!el) return;
      const a = state.gAngle + st.slot * SLOT_STEP;
      el.style.left = cx + rx * Math.cos(a) + "px";
      el.style.top = cy + ry * Math.sin(a) + "px";
    });

    // Bounce-in animation
    const timeouts: number[] = [];
    if (prefersReduced) {
      bubbles.forEach((b, i) => {
        if (!b) return;
        b.style.opacity = "1";
        b.style.transform = "translate(-50%, -50%)";
        const bst = state.bubbles[i];
        if (bst) bst.ready = true;
      });
    } else {
      bubbles.forEach((b, i) => {
        if (!b) return;
        const tid = window.setTimeout(
          () => {
            b.classList.add("bounced");
            b.addEventListener(
              "animationend",
              () => {
                b.style.opacity = "1";
                b.style.transform = "translate(-50%, -50%)";
                b.classList.remove("bounced");
                const bst = state.bubbles[i];
                if (bst) bst.ready = true;
              },
              { once: true },
            );
          },
          400 + i * 120,
        );
        timeouts.push(tid);
      });
    }

    // RAF orbit rotation
    const tick = (ts: number) => {
      if (state.lastT === 0) state.lastT = ts;
      const dt = ts - state.lastT;
      state.lastT = ts;
      state.gAngle += ORBIT_SPEED * dt;

      const { cx, cy, rx, ry } = getDims();
      state.bubbles.forEach((st, i) => {
        if (!st.ready || st.isDragging || st.isSnapping) return;
        const el = bubbles[i];
        if (!el) return;
        const a = state.gAngle + st.slot * SLOT_STEP;
        el.style.left = cx + rx * Math.cos(a) + "px";
        el.style.top = cy + ry * Math.sin(a) + "px";
      });

      rafRef.current = requestAnimationFrame(tick);
    };
    rafRef.current = requestAnimationFrame(tick);

    const handleResize = () => updateTrack();
    window.addEventListener("resize", handleResize);

    return () => {
      cancelAnimationFrame(rafRef.current);
      window.removeEventListener("resize", handleResize);
      timeouts.forEach(clearTimeout);
    };
  }, [getDims, updateTrack]);

  const handlePointerDown = useCallback(
    (i: number, e: React.PointerEvent<HTMLDivElement>) => {
      const st = stateRef.current.bubbles[i];
      const el = bubbleRefs.current[i];
      if (!st || !st.ready || st.isSnapping || !el) return;

      st.isDragging = true;
      el.classList.add("dragging");
      el.classList.remove("snapping");
      el.setPointerCapture(e.pointerId);

      dragRef.current = {
        idx: i,
        startX: e.clientX,
        startY: e.clientY,
        origLeft: parseFloat(el.style.left),
        origTop: parseFloat(el.style.top),
      };
    },
    [],
  );

  const handlePointerMove = useCallback(
    (i: number, e: React.PointerEvent<HTMLDivElement>) => {
      const drag = dragRef.current;
      if (!drag || drag.idx !== i || !stateRef.current.bubbles[i]?.isDragging)
        return;
      const el = bubbleRefs.current[i];
      if (!el) return;
      el.style.left = drag.origLeft + (e.clientX - drag.startX) + "px";
      el.style.top = drag.origTop + (e.clientY - drag.startY) + "px";
    },
    [],
  );

  const handlePointerUp = useCallback(
    (i: number, e: React.PointerEvent<HTMLDivElement>) => {
      const st = stateRef.current.bubbles[i];
      const el = bubbleRefs.current[i];
      if (!st || !st.isDragging || !el) return;

      st.isDragging = false;
      el.classList.remove("dragging");
      el.releasePointerCapture(e.pointerId);
      dragRef.current = null;

      // Find nearest empty slot
      const { cx, cy, rx, ry } = getDims();
      const curAngle = Math.atan2(
        parseFloat(el.style.top) - cy,
        parseFloat(el.style.left) - cx,
      );

      const occupied = new Set(
        stateRef.current.bubbles.filter((_, j) => j !== i).map((b) => b.slot),
      );

      let bestSlot = st.slot;
      let bestDist = Infinity;

      for (let s = 0; s < N; s++) {
        if (occupied.has(s)) continue;
        const sa = stateRef.current.gAngle + s * SLOT_STEP;
        const normSa = ((sa % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        const normCur =
          ((curAngle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        let diff = Math.abs(normSa - normCur);
        if (diff > Math.PI) diff = 2 * Math.PI - diff;
        if (diff < bestDist) {
          bestDist = diff;
          bestSlot = s;
        }
      }

      st.slot = bestSlot;
      st.isSnapping = true;
      el.classList.add("snapping");

      const a = stateRef.current.gAngle + bestSlot * SLOT_STEP;
      el.style.left = cx + rx * Math.cos(a) + "px";
      el.style.top = cy + ry * Math.sin(a) + "px";

      const cleanup = () => {
        st.isSnapping = false;
        el.classList.remove("snapping");
      };
      el.addEventListener("transitionend", cleanup, { once: true });
      setTimeout(cleanup, 600); // fallback
    },
    [getDims, N],
  );

  return (
    <div ref={containerRef} className="hero-bubbles">
      <div ref={trackRef} className="orbit-track" />
      {CATEGORIES.map((cat, i) => (
        <div
          key={cat.id}
          ref={(el) => {
            bubbleRefs.current[i] = el;
          }}
          className={`bubble bubble-${cat.id}`}
          onPointerDown={(e) => handlePointerDown(i, e)}
          onPointerMove={(e) => handlePointerMove(i, e)}
          onPointerUp={(e) => handlePointerUp(i, e)}
        >
          <span className="bubble-icon">{cat.emoji}</span>
          {cat.label}
        </div>
      ))}
    </div>
  );
}

// ─── Notice Section ────────────────────────────────

function NoticeSection({
  pinnedPosts,
  isLoading,
}: {
  pinnedPosts: PinnedPostListResponse[];
  isLoading: boolean;
}) {
  return (
    <section className="pt-s6 pb-s8">
      <div className="max-w-[1280px] mx-auto px-s6 max-md:px-s4">
        <div className="flex items-baseline justify-between mb-s6 max-md:mb-s5">
          <div className="flex items-baseline gap-s3">
            <span className="typo-b2 font-semibold text-primary uppercase tracking-[0.08em]">
              Notice
            </span>
            <h2 className="typo-b2 font-semibold text-foreground">공지사항</h2>
          </div>
          <Link
            to="/board/notices"
            className="typo-c1 text-muted-foreground hover:text-foreground transition-colors"
          >
            전체 보기 →
          </Link>
        </div>

        <div className="min-h-[200px]">
          {isLoading ? (
            <div className="flex items-center justify-center h-[200px] text-muted-foreground typo-b2">
              로딩 중...
            </div>
          ) : pinnedPosts.length === 0 ? (
            <div className="flex items-center justify-center h-[200px] text-muted-foreground typo-b2">
              공지사항이 없습니다
            </div>
          ) : (
            pinnedPosts.map((pinned) => {
              const post = pinned.post;
              const postIsNew = isWithin2Days(post?.createdAt);
              return (
                <Link
                  key={pinned.id}
                  to={`/board/${post?.boardCode}/${post?.id}`}
                  className="notice-row"
                >
                  <span
                    className={cn(
                      "typo-c2 font-semibold rounded-r1 px-s2 py-[2px] shrink-0 tracking-[0.02em] min-w-[36px] text-center",
                      postIsNew
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground",
                    )}
                  >
                    {postIsNew ? "NEW" : "공지"}
                  </span>
                  <span className="flex-1 min-w-0 typo-b2 font-medium text-foreground truncate">
                    {post?.title}
                  </span>
                  <span className="typo-c1 text-muted-foreground shrink-0 tabular-nums">
                    {formatDateDot(post?.createdAt)}
                  </span>
                </Link>
              );
            })
          )}
        </div>
      </div>
    </section>
  );
}

// ─── Split Section (Events + Instagram) ────────────

function SplitSection({
  events,
  isEventsLoading,
}: {
  events: EventListResponse[];
  isEventsLoading: boolean;
}) {
  const bothEnabled = __FEATURE_EVENTS__ && __FEATURE_INSTAGRAM__;

  return (
    <section className="pt-s6 pb-s8 border-t border-border">
      <div className="max-w-[1280px] mx-auto px-s6 max-md:px-s4">
        <div
          className={
            bothEnabled
              ? "grid grid-cols-2 gap-s8 max-lg:grid-cols-1 max-lg:gap-0"
              : ""
          }
        >
          {__FEATURE_EVENTS__ && (
            <div className="min-w-0">
              <EventTimeline events={events} isLoading={isEventsLoading} />
            </div>
          )}
          {__FEATURE_INSTAGRAM__ && (
            <div
              className={cn(
                "min-w-0",
                bothEnabled &&
                  "max-lg:pt-s8 max-lg:border-t max-lg:border-border",
              )}
            >
              <InstagramFeed />
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

// ─── Event Timeline ────────────────────────────────

function EventTimeline({
  events,
  isLoading,
}: {
  events: EventListResponse[];
  isLoading: boolean;
}) {
  return (
    <>
      <div className="flex items-baseline justify-between mb-s6 max-md:mb-s5">
        <div className="flex items-baseline gap-s3">
          <span className="typo-b2 font-semibold text-primary uppercase tracking-[0.08em]">
            Events
          </span>
          <h2 className="typo-b2 font-semibold text-foreground">
            다가오는 행사
          </h2>
        </div>
        <Link
          to="/events"
          className="typo-c1 text-muted-foreground hover:text-foreground transition-colors"
        >
          전체 보기 →
        </Link>
      </div>

      <div className="min-h-[240px]">
        {isLoading ? (
          <div className="flex items-center justify-center h-[240px] text-muted-foreground typo-b2">
            로딩 중...
          </div>
        ) : events.length === 0 ? (
          <div className="flex items-center justify-center h-[240px] text-muted-foreground typo-b2">
            다가오는 행사가 없습니다
          </div>
        ) : (
          <div className="event-timeline pointer-events-none">
            {events.map((event) => {
              const isOpen = event.registrationStatus === "OPEN";
              return (
                <div key={event.id} className="timeline-item">
                  <div
                    className={cn(
                      "timeline-dot",
                      isOpen && "timeline-dot--open",
                    )}
                  />
                  <div
                    className={cn(
                      "typo-c1 font-semibold mb-s2 tabular-nums",
                      isOpen ? "text-primary" : "text-muted-foreground",
                    )}
                  >
                    {formatDateDot(event.eventStartAt)}
                  </div>
                  <h3 className="typo-b1 font-semibold text-foreground mb-s1 leading-snug">
                    {event.title}
                  </h3>
                  {event.location && (
                    <p className="typo-b2 text-muted-foreground leading-relaxed">
                      {event.location}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </>
  );
}

// ─── Instagram Feed ────────────────────────────────

function InstagramFeed() {
  return (
    <>
      <div className="flex items-baseline justify-between mb-s7 max-md:flex-col max-md:gap-s2 max-md:mb-s6">
        <div className="flex items-baseline gap-s3">
          <span className="typo-c1 font-semibold text-primary uppercase tracking-[0.08em]">
            Instagram
          </span>
          <h2 className="typo-h3 text-foreground">활동 갤러리</h2>
        </div>
      </div>

      {/* 인스타그램 계정 헤더 */}
      <div className="flex items-center gap-s3 mb-s5">
        <div className="ig-avatar">
          <div className="ig-avatar-inner">
            <Instagram size={20} className="text-foreground" />
          </div>
        </div>
        <div className="flex-1 min-w-0">
          <div className="typo-b2 font-semibold text-foreground">
            @igrus_inha
          </div>
          <div className="typo-c1 text-muted-foreground">
            인하대 컴퓨터 동아리 IGRUS
          </div>
        </div>
        <a
          href={IG_URL}
          target="_blank"
          rel="noopener noreferrer"
          className="px-s4 py-s1 bg-[#0095F6] text-white typo-c1 font-semibold rounded-r2 hover:opacity-85 transition-opacity shrink-0"
        >
          팔로우
        </a>
      </div>

      {/* 2×3 썸네일 그리드 */}
      <div className="grid grid-cols-3 gap-s1">
        {IG_THUMBNAILS.map((thumb) => (
          <div
            key={thumb.label}
            className={cn(
              "aspect-square rounded-r1 overflow-hidden cursor-pointer relative bg-muted hover:opacity-85 transition-opacity",
              thumb.className,
            )}
          >
            <div className="ig-thumb-placeholder">
              <thumb.Icon size={24} />
            </div>
            <span className="absolute bottom-s2 left-s2 typo-c2 font-semibold text-white bg-black/50 px-s2 py-[2px] rounded-r1 backdrop-blur-[4px]">
              {thumb.label}
            </span>
          </div>
        ))}
      </div>

      {/* 하단 링크 */}
      <div className="mt-s4 text-center">
        <a
          href={IG_URL}
          target="_blank"
          rel="noopener noreferrer"
          className="typo-label text-muted-foreground hover:text-foreground transition-colors inline-flex items-center gap-s1"
        >
          인스타그램에서 더 보기
          <ChevronRight size={14} />
        </a>
      </div>
    </>
  );
}
