import { Link } from "react-router-dom";
import { useUIStore } from "@/stores";
import { MessageCircle, Heart, Bookmark } from "lucide-react";
import { Card } from "@/components/ui/card";
import type { PostListResponse } from "@/api/model/models";
import type { BoardType } from "@/types/common";

interface PostListItemProps {
  post: PostListResponse;
  boardType: BoardType;
  linkTo?: string;
}

export default function PostListItem({ post, linkTo }: PostListItemProps) {
  const { theme } = useUIStore();
  const isDark = theme === "dark";

  const authorName = post.authorName ?? "익명";
  const authorInitial = authorName[0] ?? "?";

  // 날짜 포맷팅 (ISO → 간단한 형식)
  const formatDate = (isoDate?: string) => {
    if (!isoDate) return "";
    const date = new Date(isoDate);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));

    if (hours < 24) {
      return `${hours}시간 전`;
    }
    const days = Math.floor(hours / 24);
    if (days < 7) {
      return `${days}일 전`;
    }
    return date.toLocaleDateString("ko-KR", { month: "short", day: "numeric" });
  };

  const content = (
    <Card
      className={`px-s4 py-s3 md:px-s6 md:py-s5 rounded-r4 border transition-all hover:border-primary/50 group cursor-pointer ${
        isDark ? "bg-card border-border" : "bg-card border-border shadow-sm"
      }`}
    >
      <div className="flex justify-between items-start gap-s2">
        <h3 className="text-base md:text-[22px] font-bold group-hover:text-primary transition-colors flex-1 flex items-center gap-s2 flex-wrap">
          {post.title}
          {/* 데스크톱: 배지를 제목 옆에 표시 */}
          {post.isQuestion && (
            <span
              className={`hidden md:inline px-s3 py-s1 rounded-full typo-c2 font-bold uppercase tracking-widest ${
                isDark
                  ? "bg-white/5 text-muted-foreground"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              Q&A
            </span>
          )}
          {post.isVisibleToAssociate && (
            <span
              className={`hidden md:inline px-s3 py-s1 rounded-full typo-c2 font-bold tracking-widest ${
                isDark
                  ? "bg-white/5 text-muted-foreground"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              준회원 공개
            </span>
          )}
        </h3>
        {/* 오른쪽 메타 영역 */}
        <div className="flex flex-col items-end gap-s1 shrink-0">
          {/* 모바일: 배지를 오른쪽 열에 표시 */}
          {post.isQuestion && (
            <span
              className={`md:hidden px-s3 py-s1 rounded-full typo-c2 font-bold uppercase tracking-widest ${
                isDark
                  ? "bg-white/5 text-muted-foreground"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              Q&A
            </span>
          )}
          {post.isVisibleToAssociate && (
            <span
              className={`md:hidden px-s3 py-s1 rounded-full typo-c2 font-bold tracking-widest ${
                isDark
                  ? "bg-white/5 text-muted-foreground"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              준회원 공개
            </span>
          )}
          {/* 데스크톱: 한 줄로 표시 */}
          <p className="hidden md:block typo-c1 text-muted-foreground whitespace-nowrap">
            조회 {post.viewCount ?? 0} · {formatDate(post.createdAt)}
          </p>
          {/* 모바일: 조회수와 날짜를 세로로 분리 */}
          <p className="md:hidden typo-c1 text-muted-foreground whitespace-nowrap">
            조회 {post.viewCount ?? 0}
          </p>
          <p className="md:hidden typo-c1 text-muted-foreground whitespace-nowrap">
            {formatDate(post.createdAt)}
          </p>
        </div>
      </div>

      <div className="flex justify-between items-center">
        <div className="flex items-center gap-s2">
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center typo-c2 font-bold ${
              isDark ? "bg-white/10" : "bg-muted"
            }`}
          >
            {authorInitial}
          </div>
          <span className="typo-c1 font-medium text-muted-foreground">
            {authorName}
          </span>
        </div>

        <div className="flex items-center gap-s4 text-muted-foreground">
          <div className="flex items-center gap-s2 hover:text-primary transition-colors">
            <Heart size={16} />
            <span className="typo-c1">{post.likeCount ?? 0}</span>
          </div>
          <div className="flex items-center gap-s2 hover:text-primary transition-colors">
            <MessageCircle size={16} />
            <span className="typo-c1">{post.commentCount ?? 0}</span>
          </div>
          <div className="flex items-center gap-s2 hover:text-primary transition-colors cursor-pointer">
            <Bookmark size={16} />
            <span className="typo-c1">{post.bookmarkCount ?? 0}</span>
          </div>
        </div>
      </div>
    </Card>
  );

  if (linkTo) {
    return <Link to={linkTo}>{content}</Link>;
  }

  return content;
}
