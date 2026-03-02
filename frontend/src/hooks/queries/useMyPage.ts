import {
  useGetMyProfile,
  useGetMyPosts,
  useGetMyLikes1,
  useGetMyBookmarks1,
  useGetMyRegistrations,
} from "@/api/model/my-page/my-page";
import type {
  GetMyPostsParams,
  GetMyLikes1Params,
  GetMyBookmarks1Params,
  MyPostPageResponse,
  LikedPostPageResponse,
  BookmarkedPostPageResponse,
  MyRegistrationResponse,
} from "@/api/model/models";

// 쿼리 키 - invalidation을 위해 정의
export const myPageKeys = {
  all: ["/api/v1/mypage"] as const,
  profile: () => ["/api/v1/mypage/profile"] as const,
  posts: (params?: GetMyPostsParams) =>
    ["/api/v1/mypage/posts", ...(params ? [params] : [])] as const,
  likes: (params?: GetMyLikes1Params) =>
    ["/api/v1/mypage/likes", ...(params ? [params] : [])] as const,
  bookmarks: (params?: GetMyBookmarks1Params) =>
    ["/api/v1/mypage/bookmarks", ...(params ? [params] : [])] as const,
  registrations: () => ["/api/v1/mypage/registrations"] as const,
};

// 내 프로필 조회
export function useMyProfile() {
  return useGetMyProfile();
}

// 내 게시글 목록 조회 (Blob → MyPostPageResponse 캐스팅)
export function useMyPosts(params?: GetMyPostsParams) {
  const query = useGetMyPosts(params);
  const response = query.data;

  // IO boundary: Orval이 Blob으로 생성했지만 실제로는 JSON 응답
  const pageData =
    response?.status === 200
      ? (response.data as unknown as MyPostPageResponse)
      : undefined;

  return {
    ...query,
    posts: pageData?.posts ?? [],
    totalElements: pageData?.totalElements ?? 0,
    totalPages: pageData?.totalPages ?? 0,
    currentPage: pageData?.currentPage ?? 0,
    hasNext: pageData?.hasNext ?? false,
  };
}

// 좋아요한 게시글 목록 조회 (Blob → LikedPostPageResponse 캐스팅)
export function useMyLikes(params?: GetMyLikes1Params) {
  const query = useGetMyLikes1(params);
  const response = query.data;

  const pageData =
    response?.status === 200
      ? (response.data as unknown as LikedPostPageResponse)
      : undefined;

  return {
    ...query,
    posts: pageData?.posts ?? [],
    totalElements: pageData?.totalElements ?? 0,
    totalPages: pageData?.totalPages ?? 0,
    currentPage: pageData?.currentPage ?? 0,
    hasNext: pageData?.hasNext ?? false,
  };
}

// 북마크한 게시글 목록 조회 (Blob → BookmarkedPostPageResponse 캐스팅)
export function useMyBookmarks(params?: GetMyBookmarks1Params) {
  const query = useGetMyBookmarks1(params);
  const response = query.data;

  const pageData =
    response?.status === 200
      ? (response.data as unknown as BookmarkedPostPageResponse)
      : undefined;

  return {
    ...query,
    posts: pageData?.posts ?? [],
    totalElements: pageData?.totalElements ?? 0,
    totalPages: pageData?.totalPages ?? 0,
    currentPage: pageData?.currentPage ?? 0,
    hasNext: pageData?.hasNext ?? false,
  };
}

// 내 행사 신청 목록 조회 (Blob → MyRegistrationResponse[] 캐스팅)
export function useMyRegistrations() {
  const query = useGetMyRegistrations();
  const response = query.data;

  const registrations =
    response?.status === 200
      ? (response.data as unknown as MyRegistrationResponse[])
      : [];

  return {
    ...query,
    registrations,
  };
}
