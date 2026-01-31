import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 데이터가 오래됐다고 판단하는 시간 (5분)
      staleTime: 1000 * 60 * 5,
      // 캐시 유지 시간 (30분)
      gcTime: 1000 * 60 * 30,
      // 실패 시 재시도 횟수
      retry: 1,
      // 윈도우 포커스 시 자동 리페치
      refetchOnWindowFocus: false,
    },
    mutations: {
      // 뮤테이션 실패 시 재시도 안 함
      retry: false,
    },
  },
});
