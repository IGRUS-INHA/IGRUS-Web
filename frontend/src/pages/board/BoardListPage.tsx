import { useState, useEffect } from "react";
import {
  useParams,
  useNavigate,
  Link,
  useSearchParams,
} from "react-router-dom";
import { useGetPostList } from "@/api/model/post/post";
import type { PostListPageResponse } from "@/api/model/models";
import PostListItem from "@/components/feature/board/PostListItem";
import { SortSelect } from "@/components/board/SortSelect";
import { Pagination } from "@/components/board/Pagination";
import { Button } from "@/components/ui/button";
import {
  BOARDS,
  BOARD_LABELS,
  SORT_TYPE,
  PAGINATION,
  ENABLED_BOARDS,
} from "@/constants/board";
import type { BoardType } from "@/types/common";
import { cn } from "@/lib/utils";
import { useMockData } from "@/hooks/useMockData";
import { useMockPostList } from "@/hooks/queries/useMockPosts";
import { useBoardByCode } from "@/hooks/useBoards";
import { isBoardReadDenied, isForbiddenError } from "@/utils/error";

export default function BoardListPage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // State
  const [sortType, setSortType] = useState<string>(SORT_TYPE.LATEST);
  const [currentPage, setCurrentPage] = useState<number>(
    PAGINATION.DEFAULT_PAGE,
  );

  // URL 쿼리 파라미터에서 검색어 읽기
  const searchKeyword = searchParams.get("search");

  // Validate boardType
  const validBoardType =
    boardType && Object.values(BOARDS).includes(boardType as BoardType)
      ? (boardType as BoardType)
      : BOARDS.NOTICES;

  // 비활성화된 게시판 접근 시 notices로 리다이렉트
  useEffect(() => {
    if (!ENABLED_BOARDS.includes(validBoardType)) {
      navigate("/board/NOTICES", { replace: true });
    }
  }, [validBoardType, navigate]);

  // Mock 모드 확인
  const isMockMode = useMockData();

  // 게시판 권한 정보
  const { board } = useBoardByCode(validBoardType);

  // 검색어가 변경되면 첫 페이지로 이동
  useEffect(() => {
    setCurrentPage(1);
  }, [searchKeyword]);

  // Fetch posts (Mock 또는 실제 API)
  const realQuery = useGetPostList(
    validBoardType,
    {
      ...(searchKeyword && { keyword: searchKeyword }),
      page: currentPage - 1, // Orval은 0-based pagination
      size: PAGINATION.DEFAULT_SIZE,
    },
    {
      query: {
        enabled: !isMockMode,
        refetchOnMount: "always", // 페이지 마운트 시 항상 새로운 데이터 가져오기
      },
    },
  );
  const mockQuery = useMockPostList(validBoardType);

  const {
    data: response,
    isLoading,
    error,
  } = isMockMode ? mockQuery : realQuery;

  // Orval 응답 unwrap (에러 응답의 data는 void이므로 PostListPageResponse로 캐스트)
  const data = response?.data as PostListPageResponse | undefined;

  // 403 에러 체크 (권한 없음)
  const isForbidden = isBoardReadDenied(error) || isForbiddenError(error);

  // Handlers
  const handleSortChange = (newSortType: string) => {
    setSortType(newSortType);
    setCurrentPage(1); // Reset to first page on sort change
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleWriteClick = () => {
    navigate(`/board/${validBoardType}/write`);
  };

  return (
    <div className="space-y-s6 animate-in fade-in duration-300">
      {/* Page header */}
      <div>
        <p className="text-xs font-bold text-primary tracking-widest mb-s1">
          BOARD
        </p>
        <h1 className="text-xl md:text-3xl font-bold mb-s2">IGRUS 게시판</h1>
        <p className="text-xs md:text-sm text-muted-foreground">
          아이그루스의 주요 공지와 안내 사항을 확인할 수 있는 공간입니다.
        </p>
      </div>

      {/* Header with Tabs and Write Button */}
      <div
        className={cn(
          "flex border-b border-border pb-s4",
          __FEATURE_COMMUNITY__
            ? "flex-col md:flex-row md:justify-between md:items-center gap-s3"
            : "flex-row items-center gap-s2 md:justify-between",
        )}
      >
        <div
          className={cn(
            "flex gap-s4 overflow-x-auto md:justify-start",
            __FEATURE_COMMUNITY__ ? "justify-center" : "justify-start",
          )}
        >
          {ENABLED_BOARDS.map((tab) => (
            <button
              key={tab}
              onClick={() => navigate(`/board/${tab}`)}
              type="button"
              className={cn(
                "px-s4 py-s2 md:px-s5 rounded-full text-xs md:text-sm font-bold transition-all uppercase tracking-wider whitespace-nowrap cursor-pointer",
                validBoardType === tab
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-muted",
              )}
            >
              {BOARD_LABELS[tab]}
            </button>
          ))}
        </div>
        <div
          className={cn(
            "flex items-center gap-s2 md:gap-s4 max-md:flex-1",
            __FEATURE_COMMUNITY__ ? "self-start md:self-auto" : "self-auto",
          )}
        >
          <SortSelect value={sortType} onChange={handleSortChange} />
          {board.canWrite && (
            <Button
              onClick={handleWriteClick}
              type="button"
              className="flex items-center justify-center rounded-full h-8 md:h-9 px-3 md:px-4 py-1 md:py-2 text-xs md:text-sm font-bold min-w-[80px] md:min-w-[100px] ml-auto"
            >
              글쓰기
            </Button>
          )}
        </div>
      </div>

      {/* Posts List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-muted-foreground">로딩 중...</p>
        </div>
      ) : isForbidden ? (
        <div className="flex flex-col items-center justify-center py-12 gap-s4">
          <p className="text-muted-foreground">
            정회원 승인 후 게시판 이용이 가능합니다.
          </p>
        </div>
      ) : !data?.posts || data.posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 gap-s4">
          <p className="text-muted-foreground">게시글이 없습니다.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-s4">
          {data.posts.map((post) => (
            <Link
              key={post.postId}
              to={`/board/${validBoardType}/${post.postId}`}
            >
              <PostListItem post={post} boardType={validBoardType} />
            </Link>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && (data.totalPages ?? 0) > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={data.totalPages ?? 0}
          onPageChange={handlePageChange}
          className="mt-s8"
        />
      )}
    </div>
  );
}
