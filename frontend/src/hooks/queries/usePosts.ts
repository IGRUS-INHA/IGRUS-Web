import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryResult,
  type UseMutationResult,
} from '@tanstack/react-query';
import { postsApi } from '@/api/posts';
import type { Post, PostDetail } from '@/types/entities';
import type {
  PostListParams,
  CreatePostRequest,
  UpdatePostRequest,
  PaginatedResponse,
} from '@/types/api';
import type { BoardType } from '@/types/common';

// 쿼리 키 상수
export const postKeys = {
  all: ['posts'] as const,
  lists: () => [...postKeys.all, 'list'] as const,
  list: (board: BoardType, filters: PostListParams) =>
    [...postKeys.lists(), board, filters] as const,
  details: () => [...postKeys.all, 'detail'] as const,
  detail: (board: BoardType, id: string) =>
    [...postKeys.details(), board, id] as const,
};

// 게시글 목록 조회
export function usePosts(
  board: BoardType,
  params: PostListParams = {}
): UseQueryResult<PaginatedResponse<Post>> {
  return useQuery({
    queryKey: postKeys.list(board, params),
    queryFn: async (): Promise<PaginatedResponse<Post>> => {
      const response = await postsApi.getList(board, params);
      return response.data;
    },
    enabled: !!board,
  });
}

// 게시글 상세 조회
export function usePost(
  board: BoardType,
  postId: string
): UseQueryResult<PostDetail> {
  return useQuery({
    queryKey: postKeys.detail(board, postId),
    queryFn: async (): Promise<PostDetail> => {
      const response = await postsApi.getDetail(board, postId);
      return response.data;
    },
    enabled: !!board && !!postId,
  });
}

// 게시글 작성
interface CreatePostVariables {
  board: BoardType;
  data: CreatePostRequest;
}

export function useCreatePost(): UseMutationResult<
  Post,
  Error,
  CreatePostVariables
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, data }: CreatePostVariables): Promise<Post> => {
      const response = await postsApi.create(board, data);
      return response.data;
    },
    onSuccess: (_: Post, { board }: CreatePostVariables): void => {
      void queryClient.invalidateQueries({ queryKey: postKeys.list(board, {}) });
    },
  });
}

// 게시글 수정
interface UpdatePostVariables {
  board: BoardType;
  postId: string;
  data: UpdatePostRequest;
}

export function useUpdatePost(): UseMutationResult<
  Post,
  Error,
  UpdatePostVariables
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      board,
      postId,
      data,
    }: UpdatePostVariables): Promise<Post> => {
      const response = await postsApi.update(board, postId, data);
      return response.data;
    },
    onSuccess: (_: Post, { board, postId }: UpdatePostVariables): void => {
      void queryClient.invalidateQueries({
        queryKey: postKeys.detail(board, postId),
      });
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

// 게시글 삭제
interface DeletePostVariables {
  board: BoardType;
  postId: string;
}

export function useDeletePost(): UseMutationResult<
  void,
  Error,
  DeletePostVariables
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ board, postId }: DeletePostVariables): Promise<void> => {
      await postsApi.delete(board, postId);
    },
    onSuccess: (): void => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

// 좋아요 토글 (낙관적 업데이트)
interface ToggleLikeVariables {
  board: BoardType;
  postId: string;
  isLiked: boolean;
}

interface ToggleLikeContext {
  previousPost: PostDetail | undefined;
  queryKey: readonly unknown[];
}

export function useToggleLike(): UseMutationResult<
  void,
  Error,
  ToggleLikeVariables,
  ToggleLikeContext
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      board,
      postId,
      isLiked,
    }: ToggleLikeVariables): Promise<void> => {
      if (isLiked) {
        await postsApi.unlike(board, postId);
      } else {
        await postsApi.like(board, postId);
      }
    },
    // 낙관적 업데이트
    onMutate: async ({
      board,
      postId,
      isLiked,
    }: ToggleLikeVariables): Promise<ToggleLikeContext> => {
      const queryKey = postKeys.detail(board, postId);

      await queryClient.cancelQueries({ queryKey });
      const previousPost = queryClient.getQueryData<PostDetail>(queryKey);

      if (previousPost) {
        queryClient.setQueryData<PostDetail>(queryKey, {
          ...previousPost,
          isLiked: !isLiked,
          likes: isLiked ? previousPost.likes - 1 : previousPost.likes + 1,
        });
      }

      return { previousPost, queryKey };
    },
    onError: (
      _err: Error,
      _variables: ToggleLikeVariables,
      context: ToggleLikeContext | undefined
    ): void => {
      if (context?.previousPost) {
        queryClient.setQueryData(context.queryKey, context.previousPost);
      }
    },
    onSettled: (
      _data: void | undefined,
      _error: Error | null,
      { board, postId }: ToggleLikeVariables
    ): void => {
      void queryClient.invalidateQueries({
        queryKey: postKeys.detail(board, postId),
      });
    },
  });
}
