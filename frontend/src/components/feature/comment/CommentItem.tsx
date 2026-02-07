import { useState } from 'react';
import { User } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useUIStore } from '@/stores';
import { useCreateReplyMutation } from '@/hooks/queries/useComments';
import { CommentActions } from './CommentActions';
import { CommentInput } from './CommentInput';
import type { CommentWithRepliesResponse } from '@/api/model/models';

interface CommentItemProps {
  comment: CommentWithRepliesResponse;
  postId: number;
  level?: 0 | 1;
}

/**
 * 개별 댓글/대댓글 컴포넌트
 * - 삭제된 댓글, 익명 댓글 처리
 * - 대댓글 작성 및 표시
 * - 최대 1단계 대댓글만 허용
 */
export function CommentItem({ comment, postId, level = 0 }: CommentItemProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const [showReplyInput, setShowReplyInput] = useState(false);
  const [replyContent, setReplyContent] = useState('');

  const createReply = useCreateReplyMutation();

  // 삭제된 댓글이면서 대댓글이 없는 경우, 렌더링하지 않음
  if (comment.deleted && (!comment.replies || comment.replies.length === 0)) {
    return null;
  }

  const handleReplySubmit = (content: string, anonymous: boolean) => {
    if (!comment.id) return;

    createReply.mutate(
      {
        postId,
        commentId: comment.id,
        data: {
          content,
          anonymous,
        },
      },
      {
        onSuccess: () => {
          setReplyContent('');
          setShowReplyInput(false);
        },
      }
    );
  };

  const handleReplyClick = () => {
    setShowReplyInput(!showReplyInput);
  };

  // 작성 시간 포맷팅 (간단한 버전)
  const formatTime = (createdAt?: string) => {
    if (!createdAt) return '';

    const date = new Date(createdAt);
    const now = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000 / 60);

    if (diff < 1) return '방금 전';
    if (diff < 60) return `${diff}분 전`;
    if (diff < 1440) return `${Math.floor(diff / 60)}시간 전`;
    return `${Math.floor(diff / 1440)}일 전`;
  };

  return (
    <div className={cn('flex gap-s4', level === 1 && 'ml-12')}>
      {/* 아바타 */}
      <div
        className={cn(
          'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm shrink-0',
          isDark ? 'bg-white/5 text-muted-foreground' : 'bg-muted text-muted-foreground'
        )}
      >
        {comment.deleted ? (
          <User size={20} />
        ) : comment.anonymous ? (
          '익'
        ) : (
          (comment.authorName?.[0] ?? 'U')
        )}
      </div>

      <div className="flex-1">
        {/* 댓글 헤더 */}
        <div className="flex items-center gap-s2 mb-1">
          <span className="font-bold text-sm">
            {comment.deleted
              ? '알 수 없음'
              : comment.anonymous
              ? '익명'
              : comment.authorName}
          </span>
          <span className="text-xs text-muted-foreground">
            {formatTime(comment.createdAt)}
          </span>
        </div>

        {/* 댓글 내용 */}
        {comment.deleted ? (
          <p className="text-sm text-muted-foreground italic">삭제된 댓글입니다</p>
        ) : (
          <>
            <p className="text-sm mb-s2">{comment.content}</p>

            {/* 액션 버튼들 */}
            <CommentActions
              comment={comment}
              postId={postId}
              onReplyClick={level === 0 ? handleReplyClick : undefined}
              level={level}
            />
          </>
        )}

        {/* 답글 입력 (level 0에서만 표시) */}
        {showReplyInput && level === 0 && (
          <div className="mt-s4">
            <CommentInput
              value={replyContent}
              onChange={setReplyContent}
              onSubmit={handleReplySubmit}
              placeholder="답글을 입력하세요..."
              isSubmitting={createReply.isPending}
              autoFocus
            />
          </div>
        )}

        {/* 대댓글 렌더링 (재귀, 1단계만) */}
        {level === 0 && comment.replies && comment.replies.length > 0 && (
          <div className="mt-s4 space-y-s4">
            {comment.replies.map((reply) => (
              <CommentItem
                key={reply.id}
                comment={reply}
                postId={postId}
                level={1}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
