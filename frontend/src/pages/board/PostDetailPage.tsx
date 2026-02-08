import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Heart,
  MessageCircle,
  MoreHorizontal,
  AlertTriangle,
  Bookmark,
  Edit,
  Trash2,
} from 'lucide-react';
import { useGetPostDetail, useDeletePost } from '@/api/model/post/post';
import { useToggleLike } from '@/api/model/post-like/post-like';
import { useToggleBookmark, useGetBookmarkStatus } from '@/api/model/bookmark/bookmark';
import { useUIStore } from '@/stores';
import { useQueryClient } from '@tanstack/react-query';
import { Card } from '@/components/ui/card';
import { CommentSection } from '@/components/feature/comment';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';
import { useMockData } from '@/hooks/useMockData';
import { useMockPostDetail } from '@/hooks/queries/useMockPosts';
import { usePermission } from '@/hooks/usePermission';

export default function PostDetailPage() {
  const { boardType, postId } = useParams<{ boardType: BoardType; postId: string }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const isMockMode = useMockData();
  const { isAuthenticated } = usePermission();

  // Fetch post data (Mock 또는 실제 API)
  const realQuery = useGetPostDetail(
    boardType as string,
    Number(postId),
    {
      query: { enabled: !isMockMode },
    }
  );
  const mockQuery = useMockPostDetail(boardType as string, Number(postId));

  const { data: response, isLoading } = isMockMode ? mockQuery : realQuery;
  const post = response?.data;

  // Local state
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);

  // Refs
  const moreMenuRef = useRef<HTMLDivElement>(null);

  // Mutations
  const toggleLike = useToggleLike();
  const toggleBookmark = useToggleBookmark();
  const queryClient = useQueryClient();
  const deletePost = useDeletePost();

  // Bookmark status query (로그인한 경우에만 조회)
  const { data: bookmarkStatusResponse } = useGetBookmarkStatus(Number(postId), {
    query: { enabled: !isMockMode && !!postId && isAuthenticated },
  });
  const isBookmarked = bookmarkStatusResponse?.data?.bookmarked ?? false;

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

    // 로그인하지 않은 경우 로그인 페이지로 이동
    if (!isAuthenticated) {
      alert('로그인이 필요한 기능입니다.');
      navigate('/login');
      return;
    }

    toggleLike.mutate(
      { postId: post.postId },
      {
        onSuccess: () => {
          // 게시글 데이터 새로고침 (좋아요 상태 및 카운트 업데이트)
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/boards/${boardType}/posts/${post.postId}`],
          });
        },
      }
    );
  };

  const handleScrap = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!post?.postId) return;

    // 로그인하지 않은 경우 로그인 페이지로 이동
    if (!isAuthenticated) {
      alert('로그인이 필요한 기능입니다.');
      navigate('/login');
      return;
    }

    toggleBookmark.mutate(
      { postId: post.postId },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/posts/${post.postId}/bookmarks/status`],
          });
        },
      }
    );
  };

  const handleReport = () => {
    alert('이 게시글을 신고했습니다.');
    setIsMoreMenuOpen(false);
    // TODO: Implement report API call
  };

  const handleEdit = () => {
    navigate(`/board/${boardType}/${postId}/edit`);
    setIsMoreMenuOpen(false);
  };

  const handleDelete = () => {
    if (!window.confirm('이 게시글을 삭제하시겠습니까?\n삭제된 게시글은 복구할 수 없습니다.')) {
      return;
    }

    deletePost.mutate(
      {
        boardCode: boardType as string,
        postId: Number(postId),
      },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/boards/${boardType}/posts`],
          });
          navigate(`/board/${boardType}`);
        },
        onError: (error: any) => {
          let errorMessage = '게시글 삭제에 실패했습니다.';
          if (error.message?.includes('403')) errorMessage = '삭제 권한이 없습니다.';
          else if (error.message?.includes('404')) errorMessage = '게시글을 찾을 수 없습니다.';
          alert(errorMessage);
        },
      }
    );
    setIsMoreMenuOpen(false);
  };

  const handleBack = () => {
    navigate(`/board/${boardType}`);
  };

  const handleCommentClick = () => {
    const input = document.getElementById('comment-input') as HTMLInputElement;
    if (input) {
      input.scrollIntoView({ behavior: 'smooth', block: 'center' });
      setTimeout(() => {
        input.focus();
      }, 300);
    }
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
                    'bg-popover border-border'
                  )}
                >
                  {post.isAuthor ? (
                    // 작성자: 수정/삭제
                    <>
                      <button
                        onClick={handleEdit}
                        type="button"
                        className={cn(
                          'w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer',
                          isDark ? 'text-foreground hover:bg-white/5' : 'text-foreground hover:bg-muted'
                        )}
                      >
                        <Edit size={16} /> 수정하기
                      </button>
                      <button
                        onClick={handleDelete}
                        type="button"
                        className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer"
                      >
                        <Trash2 size={16} /> 삭제하기
                      </button>
                    </>
                  ) : (
                    // 비작성자: 신고만
                    <button
                      onClick={handleReport}
                      type="button"
                      className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer"
                    >
                      <AlertTriangle size={16} /> 신고하기
                    </button>
                  )}
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
            onClick={handleCommentClick}
            type="button"
            className={cn(
              'flex items-center gap-s2 px-s6 py-s3 rounded-r3 font-bold transition-all cursor-pointer',
              isDark ? 'bg-white/5 text-muted-foreground hover:bg-white/10' : 'bg-muted text-muted-foreground hover:bg-muted/80'
            )}
          >
            <MessageCircle size={20} />
            {(post && 'commentCount' in post ? post.commentCount : 0) ?? 0}
          </button>

          <button
            onClick={handleScrap}
            type="button"
            className={cn(
              'flex items-center gap-s2 px-s6 py-s3 rounded-r3 font-bold transition-all cursor-pointer',
              isBookmarked
                ? 'bg-primary/10 text-primary'
                : isDark
                  ? 'bg-white/5 text-muted-foreground hover:bg-white/10'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
            )}
          >
            <Bookmark size={20} className={isBookmarked ? 'fill-current' : ''} />
            <span className="hidden sm:inline">스크랩</span>
          </button>
        </div>
      </article>

      {/* Comments Section */}
      <Card
        className={cn(
          'p-s8 rounded-[2.5rem] border',
          isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'
        )}
      >
        <CommentSection postId={Number(postId)} />
      </Card>
    </div>
  );
}
