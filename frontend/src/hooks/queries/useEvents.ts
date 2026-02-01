import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryResult,
  type UseMutationResult,
} from '@tanstack/react-query';
import { eventsApi } from '@/api/events';
import { MOCK_EVENTS } from '@/mocks';
import type { Event, EventDetail } from '@/types/entities';
import type { EventListParams } from '@/types/api';

// 쿼리 키 상수
export const eventKeys = {
  all: ['events'] as const,
  lists: () => [...eventKeys.all, 'list'] as const,
  list: (filters: EventListParams) => [...eventKeys.lists(), filters] as const,
  details: () => [...eventKeys.all, 'detail'] as const,
  detail: (id: string) => [...eventKeys.details(), id] as const,
};

// 행사 목록 조회 (Mock)
export function useEvents(
  options: EventListParams = {}
): UseQueryResult<Event[]> {
  return useQuery({
    queryKey: eventKeys.list(options),
    queryFn: async (): Promise<Event[]> => {
      // TODO: 백엔드 연동 시 아래 주석 해제
      // const response = await eventsApi.getList(options);
      // return response.data;
      await new Promise((resolve) => setTimeout(resolve, 300));
      return [...MOCK_EVENTS];
    },
  });
}

// 행사 상세 조회 (Mock)
export function useEvent(eventId: string): UseQueryResult<EventDetail> {
  return useQuery({
    queryKey: eventKeys.detail(eventId),
    queryFn: async (): Promise<EventDetail> => {
      // TODO: 백엔드 연동 시 아래 주석 해제
      // const response = await eventsApi.getDetail(eventId);
      // return response.data;
      await new Promise((resolve) => setTimeout(resolve, 300));
      const event = [...MOCK_EVENTS].find((e) => e.id === eventId);
      if (!event) throw new Error('Event not found');
      return event as EventDetail;
    },
    enabled: !!eventId,
  });
}

// 행사 신청
export function useApplyEvent(): UseMutationResult<void, Error, string> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (eventId: string): Promise<void> => {
      await eventsApi.register(eventId);
    },
    onSuccess: (_: void, eventId: string): void => {
      void queryClient.invalidateQueries({ queryKey: eventKeys.detail(eventId) });
      void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
    },
  });
}

// 행사 신청 취소
export function useCancelEventApplication(): UseMutationResult<
  void,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (eventId: string): Promise<void> => {
      await eventsApi.cancelRegistration(eventId);
    },
    onSuccess: (_: void, eventId: string): void => {
      void queryClient.invalidateQueries({ queryKey: eventKeys.detail(eventId) });
      void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
    },
  });
}
