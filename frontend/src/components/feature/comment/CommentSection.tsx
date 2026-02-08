import { useState } from 'react';
import { User as UserIcon, Send } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Spinner } from '@/components/ui';
import { useUIStore } from '@/stores';
import { usePermission } from '@/hooks/usePermission';
import { useComments, useCreateCommentMutation } from '@/hooks/queries/useComments';
import { CommentList } from './CommentList';
import { isForbiddenError, hasErrorCode, getErrorMessage } from '@/utils/error';

interface CommentSectionProps {
  postId: number;
}

/**
 * 댓글 섹션 컨테이너 컴포넌트
 * - 댓글 목록 조회 및 표시
 * - 최상위 댓글 작성
 * - 로그인 상태 확인
 */
export function CommentSection({ postId }: CommentSectionProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const { isAuthenticated } = usePermission();

  const [comment, setComment] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);

  const { data: commentsResponse, isLoading } = useComments(postId);
  const createComment = useCreateCommentMutation();

  const responseData = commentsResponse?.data;
  const comments =
    responseData && 'comments' in responseData ? responseData.comments ?? [] : [];
  const totalCount =
    responseData && 'totalCount' in responseData ? responseData.totalCount ?? 0 : 0;

  const handleSubmit = () => {
    if (!comment.trim() || createComment.isPending) return;

    const payload = {
      postId,
      data: {
        content: comment.trim(),
        anonymous: isAnonymous,
      },
    };

    console.log('댓글 작성 요청:', payload);

    createComment.mutate(payload, {
      onSuccess: () => {
        setComment('');
        setIsAnonymous(false);
      },
      onError: (error: unknown) => {
        console.error('댓글 작성 실패:', error);
        const isForbidden =
          isForbiddenError(error) || hasErrorCode(error, 'COMMENT_CREATE_DENIED');
        const errorMessage = isForbidden
          ? '정회원 승인 후 댓글 이용이 가능합니다.'
          : getErrorMessage(error);
        alert(errorMessage);
      },
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <>
      {/* 댓글 섹션 헤더 */}
      <h3 className="text-xl font-bold mb-s5">댓글 ({totalCount})</h3>

      {/* 댓글 입력 */}
      <div className="flex gap-s4 mb-s3">
        <div
          className={cn(
            'w-10 h-10 rounded-full flex items-center justify-center shrink-0',
            isDark ? 'bg-white/10' : 'bg-muted'
          )}
        >
          <UserIcon size={20} className="text-muted-foreground" />
        </div>
        <div className="flex-1 space-y-s2">
          <div className="relative">
            <input
              id="comment-input"
              type="text"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="댓글을 입력하세요..."
              disabled={!isAuthenticated || createComment.isPending}
              className={cn(
                'w-full rounded-r4 px-s5 py-s3 pr-s7 border focus:outline-none focus:border-primary transition-all',
                isDark ? 'bg-white/5 border-border' : 'bg-muted/50 border-border',
                (!isAuthenticated || createComment.isPending) && 'opacity-50 cursor-not-allowed'
              )}
            />
            <button
              onClick={handleSubmit}
              type="button"
              disabled={!isAuthenticated || !comment.trim() || createComment.isPending}
              className={cn(
                'absolute right-s2 top-1/2 -translate-y-1/2 p-s2 text-primary hover:bg-primary/10 rounded-r2 transition cursor-pointer',
                (!isAuthenticated || !comment.trim() || createComment.isPending) &&
                  'opacity-50 cursor-not-allowed hover:bg-transparent'
              )}
            >
              <Send size={18} />
            </button>
          </div>

          {/* 익명 체크박스 */}
          {isAuthenticated && (
            <label className="flex items-center gap-s2 cursor-pointer px-s1">
              <input
                type="checkbox"
                checked={isAnonymous}
                onChange={(e) => setIsAnonymous(e.target.checked)}
                disabled={createComment.isPending}
                className="cursor-pointer"
              />
              <span className="text-c1 text-muted-foreground">익명</span>
            </label>
          )}
        </div>
      </div>

      {/* 댓글 목록 */}
      {isLoading ? (
        <div className="flex justify-center py-s8">
          <Spinner />
        </div>
      ) : (
        <CommentList comments={comments} postId={postId} />
      )}
    </>
  );
}
