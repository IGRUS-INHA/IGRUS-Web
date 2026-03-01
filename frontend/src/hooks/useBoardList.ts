import { useState, useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { PAGINATION, SORT_TYPE, SEARCH_TYPE } from "@/constants/board";

interface BoardListParams {
  page: number;
  size: number;
  sort: string;
  searchType: string;
  keyword: string;
  category: string;
}

interface PaginationData {
  totalCount?: number;
  total?: number;
  totalPages?: number;
}

interface ApiParams {
  page: number;
  size: number;
  sort: string;
  searchType?: string;
  keyword?: string;
  category?: string;
}

interface UseBoardListReturn {
  // 현재 상태
  page: number;
  size: number;
  sort: string;
  searchType: string;
  keyword: string;
  category: string;
  totalCount: number;
  totalPages: number;

  // 액션
  setPage: (page: number) => void;
  setSort: (sort: string) => void;
  search: (keyword: string, searchType?: string) => void;
  clearSearch: () => void;
  setCategory: (category: string) => void;
  reset: () => void;
  updatePagination: (data: PaginationData) => void;

  // API 요청용
  apiParams: ApiParams;

  // 헬퍼
  hasNextPage: boolean;
  hasPrevPage: boolean;
  isSearching: boolean;
}

/**
 * 게시판 목록 조회 파라미터 관리 훅
 * 검색, 정렬, 페이지네이션을 URL 쿼리스트링으로 관리
 */
export function useBoardList(): UseBoardListReturn {
  const [searchParams, setSearchParams] = useSearchParams();
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // URL에서 파라미터 읽기
  const params = useMemo<BoardListParams>(
    () => ({
      page: Number(searchParams.get("page")) || PAGINATION.DEFAULT_PAGE,
      size: Number(searchParams.get("size")) || PAGINATION.DEFAULT_SIZE,
      sort: searchParams.get("sort") || SORT_TYPE.LATEST,
      searchType: searchParams.get("searchType") || SEARCH_TYPE.TITLE_CONTENT,
      keyword: searchParams.get("keyword") || "",
      category: searchParams.get("category") || "",
    }),
    [searchParams],
  );

  // 파라미터 업데이트 헬퍼
  const updateParams = useCallback(
    (updates: Partial<Record<string, string | number | undefined>>) => {
      setSearchParams((prev) => {
        Object.entries(updates).forEach(([key, value]) => {
          if (value === "" || value === undefined) {
            prev.delete(key);
          } else {
            prev.set(key, String(value));
          }
        });
        return prev;
      });
    },
    [setSearchParams],
  );

  // 페이지 변경
  const setPage = useCallback(
    (page: number) => updateParams({ page }),
    [updateParams],
  );

  // 정렬 변경 (페이지 1로 초기화)
  const setSort = useCallback(
    (sort: string) => updateParams({ sort, page: 1 }),
    [updateParams],
  );

  // 검색 (페이지 1로 초기화)
  const search = useCallback(
    (keyword: string, searchType: string = SEARCH_TYPE.TITLE_CONTENT) => {
      updateParams({ keyword, searchType, page: 1 });
    },
    [updateParams],
  );

  // 검색 초기화
  const clearSearch = useCallback(() => {
    updateParams({ keyword: "", searchType: "", page: 1 });
  }, [updateParams]);

  // 카테고리 변경 (페이지 1로 초기화)
  const setCategory = useCallback(
    (category: string) => updateParams({ category, page: 1 }),
    [updateParams],
  );

  // 전체 초기화
  const reset = useCallback(() => {
    setSearchParams(new URLSearchParams());
  }, [setSearchParams]);

  // 서버 응답으로 페이지 정보 업데이트
  const updatePagination = useCallback(
    (data: PaginationData) => {
      setTotalCount(data.totalCount ?? data.total ?? 0);
      setTotalPages(
        data.totalPages ?? Math.ceil((data.totalCount ?? 0) / params.size),
      );
    },
    [params.size],
  );

  // API 요청용 파라미터 객체
  const apiParams = useMemo<ApiParams>(
    () => ({
      page: params.page,
      size: params.size,
      sort: params.sort,
      ...(params.keyword && {
        searchType: params.searchType,
        keyword: params.keyword,
      }),
      ...(params.category && { category: params.category }),
    }),
    [params],
  );

  return {
    // 현재 상태
    ...params,
    totalCount,
    totalPages,

    // 액션
    setPage,
    setSort,
    search,
    clearSearch,
    setCategory,
    reset,
    updatePagination,

    // API 요청용
    apiParams,

    // 헬퍼
    hasNextPage: params.page < totalPages,
    hasPrevPage: params.page > 1,
    isSearching: !!params.keyword,
  };
}
