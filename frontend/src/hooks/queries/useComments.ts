import { useQueryClient } from "@tanstack/react-query";
import {
  useGetComments,
  useCreateComment,
  useCreateReply1,
  useDeleteComment,
  getGetCommentsQueryKey,
} from "@/api/model/comment/comment";
import {
  useLikeComment,
  useUnlikeComment,
} from "@/api/model/comment-like/comment-like";
import { useToast } from "@/hooks/useToast";

/**
 * 댓글 조회 훅
 */
export function useComments(postId: number) {
  return useGetComments(postId);
}

/**
 * 댓글 작성 뮤테이션 훅
 * - 성공 시 댓글 목록과 게시글 상세 캐시 무효화
 * - Toast 알림 표시
 */
export function useCreateCommentMutation() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useCreateComment({
    mutation: {
      onSuccess: (_data, variables) => {
        // 댓글 목록 갱신
        void queryClient.invalidateQueries({
          queryKey: getGetCommentsQueryKey(variables.postId),
        });
        // 게시글 상세 갱신 (댓글 수 업데이트)
        void queryClient.invalidateQueries({
          queryKey: ["/api/v1/boards", variables.postId],
        });
        // 게시글 목록 갱신 (댓글 수 업데이트)
        void queryClient.invalidateQueries({
          queryKey: ["/api/v1/boards"],
        });
        toast.success("댓글이 작성되었습니다");
      },
      onError: () => {
        toast.error("댓글 작성에 실패했습니다");
      },
    },
  });
}

/**
 * 대댓글 작성 뮤테이션 훅
 * - 성공 시 댓글 목록과 게시글 상세 캐시 무효화
 * - Toast 알림 표시
 */
export function useCreateReplyMutation() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useCreateReply1({
    mutation: {
      onSuccess: (_data, variables) => {
        // 댓글 목록 갱신
        void queryClient.invalidateQueries({
          queryKey: getGetCommentsQueryKey(variables.postId),
        });
        // 게시글 상세 갱신 (댓글 수 업데이트)
        void queryClient.invalidateQueries({
          queryKey: ["/api/v1/boards", variables.postId],
        });
        // 게시글 목록 갱신 (댓글 수 업데이트)
        void queryClient.invalidateQueries({
          queryKey: ["/api/v1/boards"],
        });
        toast.success("답글이 작성되었습니다");
      },
      onError: () => {
        toast.error("답글 작성에 실패했습니다");
      },
    },
  });
}

/**
 * 댓글 삭제 뮤테이션 훅
 * - 성공 시 댓글 목록과 게시글 상세 캐시 무효화
 * - Toast 알림 표시
 */
export function useDeleteCommentMutation() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useDeleteComment({
    mutation: {
      onSuccess: (_data, variables) => {
        // 댓글 목록 갱신
        void queryClient.invalidateQueries({
          queryKey: getGetCommentsQueryKey(variables.postId),
        });
        // 게시글 상세 갱신 (댓글 수 업데이트)
        void queryClient.invalidateQueries({
          queryKey: ["/api/v1/boards", variables.postId],
        });
        // 게시글 목록 갱신 (댓글 수 업데이트)
        void queryClient.invalidateQueries({
          queryKey: ["/api/v1/boards"],
        });
        toast.success("댓글이 삭제되었습니다");
      },
      onError: () => {
        toast.error("댓글 삭제에 실패했습니다");
      },
    },
  });
}

/**
 * 댓글 좋아요 토글 훅
 * - isLiked 상태에 따라 좋아요/좋아요 취소
 * - 성공 시 댓글 목록 캐시 무효화
 */
export function useToggleCommentLike() {
  const queryClient = useQueryClient();
  const likeMutation = useLikeComment();
  const unlikeMutation = useUnlikeComment();

  return {
    toggle: (commentId: number, postId: number, isLiked: boolean) => {
      const mutation = isLiked ? unlikeMutation : likeMutation;

      mutation.mutate(
        { commentId },
        {
          onSuccess: () => {
            // 댓글 목록 갱신
            void queryClient.invalidateQueries({
              queryKey: getGetCommentsQueryKey(postId),
            });
          },
          onError: () => {
            alert("본인이 작성한 댓글에는 좋아요를 할 수 없습니다.");
          },
        },
      );
    },
    isLoading: likeMutation.isPending || unlikeMutation.isPending,
  };
}
