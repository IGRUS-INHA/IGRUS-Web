import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { PenTool } from 'lucide-react';
import { useGetPostList } from '@/api/model/post/post';
import { useUIStore } from '@/stores';
import PostListItem from '@/components/feature/board/PostListItem';
import { SearchBar } from '@/components/board/SearchBar';
import { SortSelect } from '@/components/board/SortSelect';
import { Pagination } from '@/components/board/Pagination';
import { Button } from '@/components/ui/button';
import { BOARDS, BOARD_LABELS, SORT_TYPE, PAGINATION } from '@/constants/board';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';

export default function BoardListPage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  // State
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchType, setSearchType] = useState('title_content');
  const [sortType, setSortType] = useState(SORT_TYPE.LATEST);
  const [currentPage, setCurrentPage] = useState(PAGINATION.DEFAULT_PAGE);

  // Validate boardType
  const validBoardType = boardType && Object.values(BOARDS).includes(boardType as BoardType)
    ? (boardType as BoardType)
    : BOARDS.NOTICES;

  // Fetch posts
  const { data: response, isLoading } = useGetPostList(validBoardType, {
    keyword: searchKeyword || undefined,
    page: currentPage - 1, // Orval은 0-based pagination
    size: PAGINATION.DEFAULT_SIZE,
  });

  // Orval 응답 unwrap
  const data = response?.data;

  // Handlers
  const handleSearch = (keyword: string, type: string) => {
    setSearchKeyword(keyword);
    setSearchType(type);
    setCurrentPage(1); // Reset to first page on search
  };

  const handleClearSearch = () => {
    setSearchKeyword('');
    setCurrentPage(1);
  };

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
        <Button
          onClick={handleWriteClick}
          type="button"
          className="flex items-center gap-s2 rounded-full"
        >
          <PenTool size={14} /> <span className="hidden sm:inline">글쓰기</span>
        </Button>
      </div>

      {/* Search and Sort */}
      <div className="flex flex-col md:flex-row gap-s4 justify-between">
        <div className="flex-1 max-w-2xl">
          <SearchBar
            onSearch={handleSearch}
            onClear={handleClearSearch}
            initialKeyword={searchKeyword}
            initialSearchType={searchType}
            placeholder="검색어를 입력하세요"
          />
        </div>
        <SortSelect value={sortType} onChange={handleSortChange} />
      </div>

      {/* Posts List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-muted-foreground">로딩 중...</p>
        </div>
      ) : !data?.posts || data.posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 gap-s4">
          <p className="text-muted-foreground">게시글이 없습니다.</p>
          {searchKeyword && (
            <Button variant="outline" onClick={handleClearSearch}>
              검색 초기화
            </Button>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-s4">
          {data.posts.map((post) => (
            <Link key={post.postId} to={`/board/${validBoardType}/${post.postId}`}>
              <PostListItem post={post} />
            </Link>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages && data.totalPages > 1 && (
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
