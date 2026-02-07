import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link, useSearchParams } from 'react-router-dom';
import { PenTool } from 'lucide-react';
import { useGetPostList } from '@/api/model/post/post';
import PostListItem from '@/components/feature/board/PostListItem';
import { SortSelect } from '@/components/board/SortSelect';
import { Pagination } from '@/components/board/Pagination';
import { Button } from '@/components/ui/button';
import { FullPageSpinner, Spinner } from '@/components/ui';
import { SORT_TYPE, PAGINATION } from '@/constants/board';
import type { PostListPageResponse } from '@/api/model/models';
import { BOARDS, type BoardType } from '@/types/common';
import { cn } from '@/lib/utils';
import { useMockData } from '@/hooks/useMockData';
import { useMockPostList } from '@/hooks/queries/useMockPosts';
import { useBoardList, useBoardByCode, type Board } from '@/hooks/useBoards';
import { useUIStore } from '@/stores';

export default function BoardListPage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { addToast } = useUIStore();

  // State
  const [sortType, setSortType] = useState<string>(SORT_TYPE.LATEST);
  const [currentPage, setCurrentPage] = useState<number>(PAGINATION.DEFAULT_PAGE);

  // URL 쿼리 파라미터에서 검색어 읽기
  const searchKeyword = searchParams.get('search');

  // Validate boardType
  const validBoardType = boardType && Object.values(BOARDS).includes(boardType as BoardType)
    ? (boardType as BoardType)
    : BOARDS.NOTICES;

  // 게시판 목록 조회 (탭용)
  const { boards, isLoading: boardsLoading } = useBoardList();

  // 현재 게시판 권한 정보 조회
  const { board, isLoading: boardLoading } = useBoardByCode(validBoardType);

  // Mock 모드 확인
  const isMockMode = useMockData();

  // 권한 체크: 백엔드 응답 기반으로 접근 권한이 없으면 토스트 + 리다이렉트
  useEffect(() => {
    if (!boardLoading && !board.canRead) {
      addToast({
        type: 'warning',
        title: '접근 권한 부족',
        message: '게시판 조회 권한이 없습니다.',
        duration: 5000,
      });
      navigate('/', { replace: true });
    }
  }, [boardLoading, board.canRead, addToast, navigate]);

  // 검색어가 변경되면 첫 페이지로 이동
  useEffect(() => {
    setCurrentPage(1);
  }, [searchKeyword]);

  // Fetch posts (Mock 또는 실제 API) - 백엔드 권한 체크
  const realQuery = useGetPostList(validBoardType, {
    ...(searchKeyword && { keyword: searchKeyword }),
    page: currentPage - 1, // Orval은 0-based pagination
    size: PAGINATION.DEFAULT_SIZE,
  }, {
    query: {
      enabled: !isMockMode && board.canRead, // 백엔드 권한 체크
      refetchOnMount: 'always', // 페이지 마운트 시 항상 새로운 데이터 가져오기
    },
  });
  const mockQuery = useMockPostList(validBoardType);

  const { data: response, isLoading } = isMockMode ? mockQuery : realQuery;

  // Orval 응답 unwrap (customFetch가 에러 시 throw하므로 data는 항상 성공 타입)
  const data = response?.data as PostListPageResponse | undefined;

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

  // 로딩 중
  if (boardLoading || boardsLoading) {
    return <FullPageSpinner />;
  }

  return (
    <div className="space-y-s8 animate-in fade-in duration-300">
      {/* Header with Tabs and Write Button */}
      <div className="flex justify-between items-center border-b border-border pb-s4">
        <div className="flex gap-s4 overflow-x-auto">
          {boards?.map((board: Board) => (
            <button
              key={board.code}
              onClick={() => handleTabClick(board.code as BoardType)}
              type="button"
              className={cn(
                'px-s6 py-s2 rounded-full text-sm font-bold transition-all uppercase tracking-wider whitespace-nowrap cursor-pointer',
                validBoardType === board.code
                  ? 'bg-primary text-primary-foreground shadow-lg'
                  : 'text-muted-foreground hover:bg-muted'
              )}
            >
              {board.name}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-s4">
          <SortSelect value={sortType} onChange={handleSortChange} />
          <Button
            onClick={handleWriteClick}
            type="button"
            className="flex items-center justify-center gap-s2 rounded-full h-9 px-s4 py-s2 min-w-[100px]"
            disabled={!board.canWrite}
          >
            <PenTool size={14} /> <span className="hidden sm:inline">글쓰기</span>
          </Button>
        </div>
      </div>

      {/* Posts List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
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
