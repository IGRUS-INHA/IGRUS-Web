import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Heart,
  MessageCircle,
  MoreHorizontal,
  Send,
  User as UserIcon,
  AlertTriangle,
  UserX,
  Bookmark,
} from 'lucide-react';
import { useGetPostDetail } from '@/api/model/post/post';
import { useToggleLike } from '@/api/model/post-like/post-like';
import { useUIStore } from '@/stores';
import { Card } from '@/components/ui/card';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';

export default function PostDetailPage() {
  const { boardType, postId } = useParams<{ boardType: BoardType; postId: string }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  // Fetch post data
  const { data: response, isLoading } = useGetPostDetail(
    boardType as string,
    Number(postId)
  );
  const post = response?.data;

  // Local state
  const [isScrapped, setIsScrapped] = useState(false);
  const [comment, setComment] = useState('');
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);

  // Refs
  const moreMenuRef = useRef<HTMLDivElement>(null);

  // Mutations
  const toggleLike = useToggleLike();

  // Click outside handler
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (moreMenuRef.current && !moreMenuRef.current.contains(event.target as Node)) {
        setIsMoreMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Handlers
  const handleLike = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!post?.postId) return;

    toggleLike.mutate({
      postId: post.postId,
    });
  };

  const handleScrap = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsScrapped(!isScrapped);
    // TODO: Implement bookmark API call
  };

  const handleReport = () => {
    alert('이 게시글을 신고했습니다.');
    setIsMoreMenuOpen(false);
    // TODO: Implement report API call
  };

  const handleBlock = () => {
    if (!post?.authorName) return;
    alert(`${post.authorName}님을 차단했습니다.`);
    setIsMoreMenuOpen(false);
    // TODO: Implement block API call
  };

  const handleBack = () => {
    navigate(`/board/${boardType}`);
  };

  const handleCommentSubmit = () => {
    if (!comment.trim()) return;
    console.log('댓글 작성:', comment);
    setComment('');
    // TODO: Implement comment creation API call
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-muted-foreground">로딩 중...</p>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-s4">
        <p className="text-muted-foreground">게시글을 찾을 수 없습니다.</p>
        <button
          type="button"
          onClick={handleBack}
          className="text-primary hover:underline cursor-pointer"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  const authorName = post.authorName ?? 'Unknown';
  const authorInitial = authorName[0];

  return (
    <div className="animate-in slide-in-from-right-8 duration-300 pb-12">
      {/* Navigation */}
      <button
        onClick={handleBack}
        type="button"
        className={cn(
          'mb-s6 flex items-center gap-s2 text-sm font-bold transition-colors cursor-pointer',
          isDark ? 'text-muted-foreground hover:text-foreground' : 'text-muted-foreground hover:text-foreground'
        )}
      >
        <ArrowLeft size={18} /> 목록으로
      </button>

      {/* Main Post Card */}
      <article
        className={cn(
          'p-s8 md:p-12 rounded-[2.5rem] border mb-s8 relative',
          isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'
        )}
      >
        {/* Header */}
        <div className="flex flex-col gap-s6 mb-s8 border-b border-border pb-s8">
          <div className="flex justify-between items-start">
            <span
              className={cn(
                'px-s4 py-1.5 rounded-full text-xs font-bold uppercase tracking-widest',
                isDark ? 'bg-white/5 text-muted-foreground' : 'bg-muted text-muted-foreground'
              )}
            >
              {post.boardCode}
            </span>

            {/* More Options Menu */}
            <div className="relative" ref={moreMenuRef}>
              <button
                onClick={() => setIsMoreMenuOpen(!isMoreMenuOpen)}
                type="button"
                className={cn(
                  'p-s2 rounded-full transition cursor-pointer',
                  isDark
                    ? 'text-muted-foreground hover:bg-white/5 hover:text-foreground'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                )}
              >
                <MoreHorizontal size={20} />
              </button>

              {isMoreMenuOpen && (
                <div
                  className={cn(
                    'absolute right-0 top-full mt-2 w-48 rounded-r3 shadow-2xl border overflow-hidden z-20 animate-in fade-in zoom-in-95 duration-200',
                    isDark ? 'bg-[#252525] border-white/10' : 'bg-background border-border'
                  )}
                >
                  <button
                    onClick={handleReport}
                    type="button"
                    className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer"
                  >
                    <AlertTriangle size={16} /> 신고하기
                  </button>
                  <button
                    onClick={handleBlock}
                    type="button"
                    className={cn(
                      'w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer',
                      isDark ? 'text-foreground hover:bg-white/5' : 'text-muted-foreground hover:bg-muted'
                    )}
                  >
                    <UserX size={16} /> 작성자 차단
                  </button>
                </div>
              )}
            </div>
          </div>

          <h1 className="text-3xl md:text-4xl font-bold leading-tight">{post.title}</h1>

          <div className="flex items-center gap-s3">
            <div
              className={cn(
                'w-10 h-10 rounded-full flex items-center justify-center font-bold text-lg',
                isDark ? 'bg-white/10' : 'bg-muted'
              )}
            >
              {authorInitial}
            </div>
            <div>
              <p className="text-sm font-bold">{authorName}</p>
              <p className="text-xs text-muted-foreground">{post.createdAt} · 4분 읽기</p>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className={cn('prose max-w-none mb-10', isDark ? 'prose-invert text-muted-foreground' : 'text-muted-foreground')}>
          {post.imageUrls?.[0] && (
            <div className="my-8 rounded-r4 overflow-hidden aspect-video">
              <img src={post.imageUrls[0]} alt={post.title} className="w-full h-full object-cover" />
            </div>
          )}
          <div className="text-lg leading-relaxed whitespace-pre-wrap">{post.content}</div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-s4 border-t border-border pt-s8">
          <button
            onClick={handleLike}
            type="button"
            className={cn(
              'flex items-center gap-s2 px-s6 py-s3 rounded-r3 font-bold transition-all cursor-pointer',
              post.liked
                ? 'bg-red-500/10 text-red-500'
                : isDark
                  ? 'bg-white/5 text-muted-foreground hover:bg-white/10'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
            )}
          >
            <Heart size={20} className={post.liked ? 'fill-current' : ''} />
            {post.likeCount ?? 0}
          </button>

          <button
            type="button"
            className={cn(
              'flex items-center gap-s2 px-s6 py-s3 rounded-r3 font-bold transition-all cursor-pointer',
              isDark ? 'bg-white/5 text-muted-foreground hover:bg-white/10' : 'bg-muted text-muted-foreground hover:bg-muted/80'
            )}
          >
            <MessageCircle size={20} />
            {post.commentCount ?? 0}
          </button>

          <button
            onClick={handleScrap}
            type="button"
            className={cn(
              'flex items-center gap-s2 px-s6 py-s3 rounded-r3 font-bold transition-all cursor-pointer',
              isScrapped
                ? 'bg-primary/10 text-primary'
                : isDark
                  ? 'bg-white/5 text-muted-foreground hover:bg-white/10'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
            )}
          >
            <Bookmark size={20} className={isScrapped ? 'fill-current' : ''} />
            <span className="hidden sm:inline">스크랩</span>
          </button>
        </div>
      </article>

      {/* Comments Section */}
      <Card className={cn('p-s8 rounded-[2.5rem] border', isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm')}>
        <h3 className="text-xl font-bold mb-s6">댓글 ({post.commentCount ?? 0})</h3>

        {/* Comment Input */}
        <div className="flex gap-s4 mb-s8">
          <div
            className={cn(
              'w-10 h-10 rounded-full flex items-center justify-center shrink-0',
              isDark ? 'bg-white/10' : 'bg-muted'
            )}
          >
            <UserIcon size={20} className="text-muted-foreground" />
          </div>
          <div className="flex-1 relative">
            <input
              type="text"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleCommentSubmit();
                }
              }}
              placeholder="댓글을 입력하세요..."
              className={cn(
                'w-full rounded-r4 px-5 py-s3 pr-12 border focus:outline-none focus:border-primary transition-all',
                isDark ? 'bg-white/5 border-border' : 'bg-muted/50 border-border'
              )}
            />
            <button
              onClick={handleCommentSubmit}
              type="button"
              className="absolute right-2 top-1/2 -translate-y-1/2 p-s2 text-primary hover:bg-primary/10 rounded-lg transition cursor-pointer"
            >
              <Send size={18} />
            </button>
          </div>
        </div>

        {/* Mock Comments List */}
        <div className="space-y-s6">
          {[1, 2].map((_, i) => (
            <div key={i} className="flex gap-s4">
              <div
                className={cn(
                  'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm shrink-0',
                  isDark ? 'bg-white/5 text-muted-foreground' : 'bg-muted text-muted-foreground'
                )}
              >
                {i === 0 ? 'JD' : 'AL'}
              </div>
              <div>
                <div className="flex items-center gap-s2 mb-1">
                  <span className="font-bold text-sm">{i === 0 ? 'John Doe' : 'Alice Lee'}</span>
                  <span className="text-xs text-muted-foreground">2시간 전</span>
                </div>
                <p className="text-sm text-muted-foreground">
                  {i === 0 ? '정말 유익한 정보네요! 공유 감사합니다.' : '세 번째 내용에 대해 좀 더 자세히 설명해주실 수 있나요?'}
                </p>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
