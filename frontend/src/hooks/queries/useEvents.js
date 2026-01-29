import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventsApi } from '@/api/events';

export const eventKeys = {
  all: ['events'],
  lists: () => [...eventKeys.all, 'list'],
  list: (filters) => [...eventKeys.lists(), filters],
  details: () => [...eventKeys.all, 'detail'],
  detail: (id) => [...eventKeys.details(), id],
};

// 행사 목록 조회
export function useEvents(options = {}) {
  return useQuery({
    queryKey: eventKeys.list(options),
    queryFn: () => eventsApi.getList(options),
  });
}

// 행사 상세 조회
export function useEvent(eventId) {
  return useQuery({
    queryKey: eventKeys.detail(eventId),
    queryFn: () => eventsApi.getDetail(eventId),
    enabled: !!eventId,
  });
}

// 행사 신청
export function useApplyEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId) => eventsApi.apply(eventId),
    onSuccess: (_, eventId) => {
      queryClient.invalidateQueries({ queryKey: eventKeys.detail(eventId) });
      queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
    },
  });
}

// 행사 신청 취소
export function useCancelEventApplication() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId) => eventsApi.cancelApplication(eventId),
    onSuccess: (_, eventId) => {
      queryClient.invalidateQueries({ queryKey: eventKeys.detail(eventId) });
      queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
    },
  });
}
