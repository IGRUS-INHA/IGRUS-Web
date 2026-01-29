import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { postsApi } from '@/api/posts';

// 쿼리 키 상수
export const postKeys = {
  all: ['posts'],
  lists: () => [...postKeys.all, 'list'],
  list: (board, filters) => [...postKeys.lists(), board, filters],
  details: () => [...postKeys.all, 'detail'],
  detail: (board, id) => [...postKeys.details(), board, id],
};

// 게시글 목록 조회
export function usePosts(board, params = {}) {
  return useQuery({
    queryKey: postKeys.list(board, params),
    queryFn: async () => {
      const response = await postsApi.getList(board, params);
      return response.data;
    },
    enabled: !!board,
  });
}

// 게시글 상세 조회
export function usePost(board, postId) {
  return useQuery({
    queryKey: postKeys.detail(board, postId),
    queryFn: async () => {
      const response = await postsApi.getDetail(board, postId);
      return response.data;
    },
    enabled: !!board && !!postId,
  });
}

// 게시글 작성
export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, data }) => {
      const response = await postsApi.create(board, data);
      return response.data;
    },
    onSuccess: (_, { board }) => {
      queryClient.invalidateQueries({ queryKey: postKeys.list(board, {}) });
    },
  });
}

// 게시글 수정
export function useUpdatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, postId, data }) => {
      const response = await postsApi.update(board, postId, data);
      return response.data;
    },
    onSuccess: (_, { board, postId }) => {
      queryClient.invalidateQueries({ queryKey: postKeys.detail(board, postId) });
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

// 게시글 삭제
export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, postId }) => {
      const response = await postsApi.delete(board, postId);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

// 좋아요 토글 (낙관적 업데이트 예시)
export function useToggleLike() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, postId, isLiked }) => {
      if (isLiked) {
        return postsApi.unlike(board, postId);
      }
      return postsApi.like(board, postId);
    },
    // 낙관적 업데이트
    onMutate: async ({ board, postId, isLiked }) => {
      const queryKey = postKeys.detail(board, postId);

      await queryClient.cancelQueries({ queryKey });
      const previousPost = queryClient.getQueryData(queryKey);

      if (previousPost) {
        queryClient.setQueryData(queryKey, {
          ...previousPost,
          isLiked: !isLiked,
          likes: isLiked ? previousPost.likes - 1 : previousPost.likes + 1,
        });
      }

      return { previousPost, queryKey };
    },
    onError: (err, _, context) => {
      if (context?.previousPost) {
        queryClient.setQueryData(context.queryKey, context.previousPost);
      }
    },
    onSettled: (_, __, { board, postId }) => {
      queryClient.invalidateQueries({ queryKey: postKeys.detail(board, postId) });
    },
  });
}
