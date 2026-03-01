import { useQuery } from "@tanstack/react-query";
import { MOCK_POSTS } from "@/mocks/posts";
import type { BoardType } from "@/types/common";
import type {
  PostListPageResponse,
  PostListResponse,
} from "@/api/model/models";

/**
 * Mock 게시글 목록 조회 훅
 */
export const useMockPostList = (boardType: BoardType) => {
  return useQuery({
    queryKey: ["mockPosts", boardType],
    queryFn: (): { data: PostListPageResponse } => {
      // 해당 게시판의 게시글 필터링
      const posts = MOCK_POSTS.filter((post) => post.board === boardType);

      // API 응답 형식에 맞춰 변환 (PostListResponse 타입 준수)
      const postListResponses: PostListResponse[] = posts.map((post) => ({
        postId: Number(post.id),
        title: post.title,
        authorName:
          typeof post.author === "string" ? post.author : post.author.name,
        isAnonymous: post.isAnonymous,
        isQuestion: post.isQuestion,
        viewCount: 0,
        likeCount: post.likes,
        commentCount: post.comments,
        createdAt: post.date,
      }));

      return {
        data: {
          posts: postListResponses,
          totalPages: 1,
          totalElements: posts.length,
          currentPage: 0,
          hasNext: false,
        },
      };
    },
    staleTime: Infinity, // Mock 데이터는 변하지 않으므로
  });
};

/**
 * Mock 게시글 상세 조회 훅
 */
export const useMockPostDetail = (boardType: string, postId: number) => {
  return useQuery({
    queryKey: ["mockPost", boardType, postId],
    queryFn: () => {
      // ID로 게시글 찾기
      const post = MOCK_POSTS.find((p) => p.id === String(postId));

      if (!post) {
        throw new Error("게시글을 찾을 수 없습니다.");
      }

      // API 응답 형식에 맞춰 변환
      return {
        data: {
          postId: Number(post.id),
          boardCode: post.board,
          title: post.title,
          content: post.content,
          authorId: 1,
          authorName:
            typeof post.author === "string" ? post.author : post.author.name,
          isAnonymous: post.isAnonymous,
          isQuestion: post.isQuestion,
          viewCount: 100,
          likeCount: post.likes,
          commentCount: post.comments,
          imageUrls: post.image ? [post.image] : undefined,
          createdAt: post.date,
          updatedAt: post.date,
          isAuthor: false,
          liked: false,
          bookmarked: false,
        },
      };
    },
    staleTime: Infinity,
  });
};
