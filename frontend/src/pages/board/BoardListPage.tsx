import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link, useSearchParams } from 'react-router-dom';
import { PenTool } from 'lucide-react';
import { useGetPostList } from '@/api/model/post/post';
import PostListItem from '@/components/feature/board/PostListItem';
import { SortSelect } from '@/components/board/SortSelect';
import { Pagination } from '@/components/board/Pagination';
import { Button } from '@/components/ui/button';
import { BOARDS, BOARD_LABELS, SORT_TYPE, PAGINATION } from '@/constants/board';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';
import { useMockData } from '@/hooks/useMockData';
import { useMockPostList } from '@/hooks/queries/useMockPosts';
import { useBoardByCode } from '@/hooks/useBoards';
import { isBoardReadDenied, isForbiddenError } from '@/utils/error';

export default function BoardListPage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // State
  const [sortType, setSortType] = useState<string>(SORT_TYPE.LATEST);
  const [currentPage, setCurrentPage] = useState<number>(PAGINATION.DEFAULT_PAGE);

  // URL 쿼리 파라미터에서 검색어 읽기
  const searchKeyword = searchParams.get('search');

  // Validate boardType
  const validBoardType = boardType && Object.values(BOARDS).includes(boardType as BoardType)
    ? (boardType as BoardType)
    : BOARDS.NOTICES;

  // Mock 모드 확인
  const isMockMode = useMockData();

  // 게시판 권한 정보
  const { board } = useBoardByCode(validBoardType);

  // 검색어가 변경되면 첫 페이지로 이동
  useEffect(() => {
    setCurrentPage(1);
  }, [searchKeyword]);

  // Fetch posts (Mock 또는 실제 API)
  const realQuery = useGetPostList(validBoardType, {
    ...(searchKeyword && { keyword: searchKeyword }),
    page: currentPage - 1, // Orval은 0-based pagination
    size: PAGINATION.DEFAULT_SIZE,
  }, {
    query: {
      enabled: !isMockMode,
      refetchOnMount: 'always', // 페이지 마운트 시 항상 새로운 데이터 가져오기
    },
  });
  const mockQuery = useMockPostList(validBoardType);

  const { data: response, isLoading, error } = isMockMode ? mockQuery : realQuery;

  // Orval 응답 unwrap
  const data = response?.data;

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

  const handleTabClick = (tab: BoardType) => {
    navigate(`/board/${tab}`);
  };

  const handleWriteClick = () => {
    navigate(`/board/${validBoardType}/write`);
  };

  return (
    <div className="space-y-s8 animate-in fade-in duration-300">
      {/* Header with Tabs and Write Button */}
      <div className="flex justify-between items-center border-b border-border pb-s4">
        <div className="flex gap-s4 overflow-x-auto">
          {(Object.values(BOARDS) as BoardType[]).map((tab) => (
            <button
              key={tab}
              onClick={() => handleTabClick(tab)}
              type="button"
              className={cn(
                'px-s6 py-s2 rounded-full text-sm font-bold transition-all uppercase tracking-wider whitespace-nowrap cursor-pointer',
                validBoardType === tab
                  ? 'bg-primary text-primary-foreground shadow-lg'
                  : 'text-muted-foreground hover:bg-muted'
              )}
            >
              {BOARD_LABELS[tab]}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-s4">
          <SortSelect value={sortType} onChange={handleSortChange} />
          {board.canWrite && (
            <Button
              onClick={handleWriteClick}
              type="button"
              className="flex items-center justify-center gap-s2 rounded-full h-9 px-4 py-2 min-w-[100px]"
            >
              <PenTool size={14} /> <span className="hidden sm:inline">글쓰기</span>
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
          <p className="text-muted-foreground">정회원 승인 후 게시판 이용이 가능합니다.</p>
        </div>
      ) : !data?.posts || data.posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 gap-s4">
          <p className="text-muted-foreground">게시글이 없습니다.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-s4">
          {data.posts.map((post) => (
            <Link key={post.postId} to={`/board/${validBoardType}/${post.postId}`}>
              <PostListItem post={post} boardType={validBoardType} />
            </Link>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={data.totalPages}
          onPageChange={handlePageChange}
          className="mt-s8"
        />
      )}
    </div>
  );
}
