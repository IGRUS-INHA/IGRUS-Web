import { Heart, MessageCircle, Trash2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { usePermission } from '@/hooks/usePermission';
import { useDeleteCommentMutation, useToggleCommentLike } from '@/hooks/queries/useComments';
import type { CommentWithRepliesResponse } from '@/api/model/models';

interface CommentActionsProps {
  comment: CommentWithRepliesResponse;
  postId: number;
  onReplyClick?: (() => void) | undefined;
  level?: number;
}

/**
 * 댓글 액션 버튼들 (좋아요, 답글, 삭제)
 */
export function CommentActions({
  comment,
  postId,
  onReplyClick,
  level = 0,
}: CommentActionsProps) {
  const { userId, isAdmin, isOperator } = usePermission();
  const deleteComment = useDeleteCommentMutation();
  const { toggle: toggleLike, isLoading: isLikeLoading } = useToggleCommentLike();

  // 삭제 권한 체크: 본인 또는 관리자/운영자
  const canDelete =
    String(comment.authorId) === String(userId) || isAdmin() || isOperator();

  const handleLike = () => {
    if (isLikeLoading) return;
    toggleLike(comment.id ?? 0, postId, comment.likedByMe ?? false);
  };

  const handleDelete = () => {
    // 반드시 confirm으로 사용자 확인
    if (!window.confirm('정말 삭제하시겠습니까?')) {
      return;
    }

    deleteComment.mutate({
      postId,
      commentId: comment.id ?? 0,
    });
  };

  return (
    <div className="flex items-center gap-s4 text-muted-foreground">
      {/* 좋아요 버튼 */}
      <button
        type="button"
        onClick={handleLike}
        disabled={isLikeLoading}
        className={cn(
          'flex items-center gap-s2 transition-colors cursor-pointer',
          comment.likedByMe
            ? 'text-primary fill-current'
            : 'hover:text-primary',
          isLikeLoading && 'opacity-50 cursor-not-allowed'
        )}
      >
        <Heart size={16} className={cn(comment.likedByMe && 'fill-current')} />
        <span className="text-c1">{comment.likeCount ?? 0}</span>
      </button>

      {/* 답글 버튼 (1단계 대댓글에는 표시 안 함) */}
      {level === 0 && onReplyClick && (
        <button
          type="button"
          onClick={onReplyClick}
          className="flex items-center gap-s2 hover:text-primary transition-colors cursor-pointer"
        >
          <MessageCircle size={16} />
          <span className="text-c1">{comment.replies?.length ?? 0}</span>
        </button>
      )}

      {/* 삭제 버튼 (권한 있을 때만 표시) */}
      {canDelete && !comment.deleted && (
        <button
          type="button"
          onClick={handleDelete}
          disabled={deleteComment.isPending}
          className={cn(
            'flex items-center gap-s2 hover:text-red-500 transition-colors cursor-pointer',
            deleteComment.isPending && 'opacity-50 cursor-not-allowed'
          )}
        >
          <Trash2 size={16} />
          <span className="text-c1">삭제</span>
        </button>
      )}
    </div>
  );
}
