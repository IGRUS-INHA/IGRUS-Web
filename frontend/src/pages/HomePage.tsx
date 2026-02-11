import { Link } from "react-router-dom";
import { ArrowRight, UserPlus, MessageCircle, Megaphone, Eye, Heart } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useUIStore } from "@/stores/uiStore";
import { useAuth } from "@/hooks";
import { cn } from "@/lib/utils";
import { useGetPinnedPostList } from "@/api/model/pinned-post/pinned-post";
import type { PinnedPostListResponse } from "@/api/model/models";
import { formatRelativeTime } from "@/utils";
import MarkdownPreview from "@uiw/react-markdown-preview";

export default function HomePage() {
  const theme = useUIStore((state) => state.theme);
  const isDark = theme === "dark";
  const { isAuthenticated } = useAuth();
  const { data: pinnedResponse, isLoading: isPinnedLoading } =
    useGetPinnedPostList();
  const pinnedPosts = (pinnedResponse?.data ?? []) as PinnedPostListResponse[];

  return (
    <div className="animate-in fade-in duration-500">
      {/* Hero Section */}
      <section
        className={cn(
          "relative w-full min-h-[480px] rounded-r4 overflow-hidden transition-all duration-500 hero-accent-line",
          isDark ? "hero-clean-dark" : "hero-clean-light",
        )}
      >
        {/* Dot Grid Pattern */}
        <div className="hero-dot-grid" />

        {/* Decorative Elements */}
        <div
          className="hero-deco-ring"
          style={{ top: "15%", right: "12%", width: 160, height: 160 }}
        />
        <div
          className="hero-deco-ring-2"
          style={{ top: "65%", right: "8%", width: 72, height: 72 }}
        />
        <div
          className="hero-glow-orb"
          style={{ top: "10%", right: "5%", width: 200, height: 200 }}
        />
        <img
          src="/igruslogo.png"
          alt=""
          className="hero-logo-deco"
          style={{
            bottom: -48,
            left: -100,
            top: "auto",
            right: "auto",
            width: 320,
            height: 320,
            opacity: 0.12,
          }}
        />

        {/* Content */}
        <div className="relative z-10 flex flex-col justify-center min-h-[480px] px-s5 md:px-s8 py-s6 md:py-s8">
          <div className="space-y-6 max-w-2xl">
            {/* Badge */}
            <div
              className={cn(
                "inline-flex items-center px-s3 py-s1 rounded-full text-[11px] font-mono tracking-wider transition-colors hero-badge-glow",
                isDark
                  ? "bg-[#03A69E]/10 border border-[#03A69E]/20 text-[#66CBC5]"
                  : "bg-[#03A69E]/5 border border-[#03A69E]/12 text-[#03A69E]",
              )}
            >
              &lt;IGRUS /&gt;
            </div>

            {/* Heading */}
            <h2 className="hero-heading hero-text-glow">
              <span
                className={cn(
                  "block transition-colors",
                  isDark ? "text-white" : "text-gray-8",
                )}
              >
                성장과 낭만의 동아리,
              </span>
              <span className="block font-black text-transparent bg-clip-text bg-gradient-to-r from-[#03A69E] via-[#0891b2] to-[#03A69E] hero-gradient-text">
                IGRUS.
              </span>
            </h2>

            {/* Description */}
            <p
              className={cn(
                "typo-b2 md:typo-b1 max-w-xl leading-relaxed transition-colors",
                isDark ? "text-[#9CA3AF]" : "text-gray-500",
              )}
            >
              2000년부터 이어진 정보통신처 직속
              <br className="sm:hidden" />
              컴퓨터 학술 자치회 IGRUS.
              <br />
              26년간 수많은 성과와 인재를 배출하며
              <br className="sm:hidden" />{" "}
              실력으로 증명해온 동아리입니다.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-wrap items-center gap-s3 pt-s6">
              {isAuthenticated ? (
                <Button
                  asChild
                  className="flex items-center gap-s2 group/btn px-s6 py-s3 rounded-full font-semibold text-sm transition-all bg-[#03A69E] text-white hover:bg-[#029890] hero-btn-glow"
                >
                  <Link
                    to={
                      __FEATURE_COMMUNITY__
                        ? "/board/general"
                        : "/board/notices"
                    }
                  >
                    {__FEATURE_COMMUNITY__ ? (
                      <MessageCircle size={16} />
                    ) : (
                      <Megaphone size={16} />
                    )}
                    {__FEATURE_COMMUNITY__
                      ? "커뮤니티 둘러보기"
                      : "공지사항 보기"}
                    <ArrowRight
                      size={16}
                      className="group-hover/btn:translate-x-1 transition-transform"
                    />
                  </Link>
                </Button>
              ) : (
                <Button
                  asChild
                  className="flex items-center gap-s2 group/btn px-s6 py-s3 rounded-full font-semibold text-sm transition-all bg-[#03A69E] text-white hover:bg-[#029890] hero-btn-glow"
                >
                  <Link to="/signup">
                    <UserPlus size={16} />
                    가입하기
                    <ArrowRight
                      size={16}
                      className="group-hover/btn:translate-x-1 transition-transform"
                    />
                  </Link>
                </Button>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Featured Section */}
      {!isPinnedLoading && pinnedPosts.length > 0 && (
        <section className="mt-s6">
          <div className="flex justify-between items-center mb-s6">
            <div>
              <h3
                className={cn(
                  "text-2xl font-bold transition-colors",
                  isDark ? "text-white" : "text-black",
                )}
              >
                주요 게시글
              </h3>
              <p className="text-gray-500 text-sm">
                엄선된 이야기와 소식을 확인하세요.
              </p>
            </div>
            <Link
              to="/board/general"
              className="text-sm text-gray-400 hover:text-[#03A69E] transition"
            >
              전체 게시글 보기
            </Link>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-s7 items-stretch">
            {pinnedPosts.map((pinned) => (
              <PinnedPostCard key={pinned.id} pinned={pinned} theme={theme} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

// PinnedPostCard Component
interface PinnedPostCardProps {
  pinned: PinnedPostListResponse;
  theme: "light" | "dark";
}

function PinnedPostCard({ pinned, theme }: PinnedPostCardProps) {
  const isDark = theme === "dark";
  const post = pinned.post;

  return (
    <Link
      to={`/board/${post?.boardCode}/${post?.id}`}
      className="h-full"
    >
      <Card
        className={cn(
          "h-full flex flex-col overflow-hidden cursor-pointer transition-all duration-300 hover:scale-[1.02]",
          isDark
            ? "bg-[#1A1A1A] border-white/5 hover:border-[#03A69E]/30"
            : "bg-white border-gray-100 hover:border-[#03A69E]/30 hover:shadow-lg",
        )}
      >
        {/* Image */}
        <div className={cn(
          "h-48 relative",
          isDark ? "bg-white/5" : "bg-muted/30",
        )}>
          <img
            src="/igruslogo2.png"
            alt=""
            className="absolute inset-0 m-auto h-40 w-40 object-contain"
          />
        </div>

        <CardContent className="p-s4 flex-1 flex flex-col">
          <div className="space-y-s4 flex-1 flex flex-col">
            <div className="flex items-center gap-s2">
              <span
                className={cn(
                  "text-xs font-bold uppercase tracking-widest",
                  isDark ? "text-gray-400" : "text-gray-500",
                )}
              >
                {post?.boardName}
              </span>
              {post?.isVisibleToAssociate && (
                <span
                  className={cn(
                    "px-s3 py-s1 rounded-full typo-c2 font-bold tracking-widest",
                    isDark ? "bg-white/5 text-muted-foreground" : "bg-muted text-muted-foreground",
                  )}
                >
                  준회원 공개
                </span>
              )}
            </div>
            <h3
              className={cn(
                "text-2xl font-bold line-clamp-2 transition-colors",
                isDark ? "text-white" : "text-black",
              )}
            >
              {post?.title}
            </h3>
            <div
              className={cn(
                "text-sm line-clamp-2 flex-1 transition-colors overflow-hidden",
                isDark ? "text-gray-400" : "text-gray-600",
              )}
              data-color-mode={isDark ? "dark" : "light"}
            >
              <MarkdownPreview source={post?.contentPreview?.replace(/\n/g, "  \n") ?? ""} className="!text-sm !bg-transparent" />
            </div>
            <div className="flex items-center justify-between pt-s2 text-xs text-gray-500">
              <span>{post?.author?.name ?? "익명"} · {post?.createdAt ? formatRelativeTime(post.createdAt) : ""}</span>
            </div>
            <div className="flex items-center gap-s4 text-xs text-gray-500">
              <span className="flex items-center gap-1"><Eye size={14} /> {post?.viewCount ?? 0}</span>
              <span className="flex items-center gap-1"><Heart size={14} /> {post?.likeCount ?? 0}</span>
              <span className="flex items-center gap-1"><MessageCircle size={14} /> {post?.commentCount ?? 0}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
