import { cn } from '@/lib/utils';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

/**
 * 페이지네이션 컴포넌트
 */
export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  className,
}: PaginationProps) {
  if (totalPages <= 1) return undefined;

  // 표시할 페이지 번호 계산 (최대 5개)
  const getPageNumbers = () => {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);

    // 끝에서 시작점 재조정
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  };

  const pageNumbers = getPageNumbers();

  return (
    <nav className={cn('flex items-center justify-center gap-s1', className)}>
      {/* 이전 페이지 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
        className="h-8 w-8 p-0"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {/* 첫 페이지 */}
      {pageNumbers[0] > 1 && (
        <>
          <Button
            variant={currentPage === 1 ? 'default' : 'outline'}
            size="sm"
            onClick={() => onPageChange(1)}
            className="h-8 w-8 p-0"
          >
            1
          </Button>
          {pageNumbers[0] > 2 && (
            <span className="px-s2 text-muted-foreground">...</span>
          )}
        </>
      )}

      {/* 페이지 번호들 */}
      {pageNumbers.map((page) => (
        <Button
          key={page}
          variant={currentPage === page ? 'default' : 'outline'}
          size="sm"
          onClick={() => onPageChange(page)}
          className="h-8 w-8 p-0"
        >
          {page}
        </Button>
      ))}

      {/* 마지막 페이지 */}
      {pageNumbers[pageNumbers.length - 1] < totalPages && (
        <>
          {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && (
            <span className="px-s2 text-muted-foreground">...</span>
          )}
          <Button
            variant={currentPage === totalPages ? 'default' : 'outline'}
            size="sm"
            onClick={() => onPageChange(totalPages)}
            className="h-8 w-8 p-0"
          >
            {totalPages}
          </Button>
        </>
      )}

      {/* 다음 페이지 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
        className="h-8 w-8 p-0"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </nav>
  );
}
