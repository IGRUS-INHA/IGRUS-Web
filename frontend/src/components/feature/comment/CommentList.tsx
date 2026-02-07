import { CommentItem } from './CommentItem';
import type { CommentWithRepliesResponse } from '@/api/model/models';

interface CommentListProps {
  comments: CommentWithRepliesResponse[];
  postId: number;
}

/**
 * 댓글 목록 컴포넌트
 * - Empty state 처리
 * - 댓글 배열을 CommentItem으로 렌더링
 */
export function CommentList({ comments, postId }: CommentListProps) {
  if (comments.length === 0) {
    return (
      <div className="text-center py-s8 text-muted-foreground">
        아직 댓글이 없습니다. 첫 댓글을 작성해보세요!
      </div>
    );
  }

  return (
    <div className="space-y-s6">
      {comments.map((comment) => (
        <CommentItem key={comment.id} comment={comment} postId={postId} />
      ))}
    </div>
  );
}
