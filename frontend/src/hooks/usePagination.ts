import { useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { DEFAULT_PAGE_SIZE } from '@/constants';

interface PaginationData {
  totalCount?: number;
  totalPages?: number;
}

interface UsePaginationReturn {
  page: number;
  limit: number;
  totalCount: number;
  totalPages: number;
  setPage: (newPage: number) => void;
  setLimit: (newLimit: number) => void;
  updatePagination: (data: PaginationData) => void;
  hasNextPage: boolean;
  hasPrevPage: boolean;
}

export const usePagination = (
  initialPage = 1,
  initialLimit = DEFAULT_PAGE_SIZE
): UsePaginationReturn => {
  const [searchParams, setSearchParams] = useSearchParams();

  const page = Number(searchParams.get('page')) || initialPage;
  const limit = Number(searchParams.get('limit')) || initialLimit;

  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const setPage = useCallback(
    (newPage: number) => {
      setSearchParams((prev) => {
        prev.set('page', String(newPage));
        return prev;
      });
    },
    [setSearchParams]
  );

  const setLimit = useCallback(
    (newLimit: number) => {
      setSearchParams((prev) => {
        prev.set('limit', String(newLimit));
        prev.set('page', '1');
        return prev;
      });
    },
    [setSearchParams]
  );

  const updatePagination = useCallback((data: PaginationData) => {
    setTotalCount(data.totalCount ?? 0);
    setTotalPages(data.totalPages ?? 0);
  }, []);

  return {
    page,
    limit,
    totalCount,
    totalPages,
    setPage,
    setLimit,
    updatePagination,
    hasNextPage: page < totalPages,
    hasPrevPage: page > 1,
  };
};
