import { useQueryClient } from '@tanstack/react-query';

// Orval로 생성된 이벤트 API hooks
import {
  useGetEventList,
  useGetEvent,
  useCreateEvent as useCreateEventMutation,
  useUpdateEvent as useUpdateEventMutation,
  useDeleteEvent as useDeleteEventMutation,
} from '@/api/model/event/event';
import {
  useRegisterEvent,
  useCancelRegistration,
} from '@/api/model/event-registration/event-registration';
import type { GetEventListParams, CreateEventRequest } from '@/api/model/models';
import { myPageKeys } from '@/hooks/queries/useMyPage';

// 쿼리 키 - Orval이 자동으로 생성하지만 invalidation을 위해 정의
export const eventKeys = {
  all: ['/api/v1/events'] as const,
  lists: () => [...eventKeys.all] as const,
  list: (filters?: GetEventListParams) =>
    [...eventKeys.all, ...(filters ? [filters] : [])] as const,
  details: () => [...eventKeys.all, 'detail'] as const,
  detail: (id: number) => [`/api/v1/events/${id}`] as const,
};

// 행사 목록 조회 (실제 API 사용)
export function useEvents(params?: GetEventListParams) {
  return useGetEventList(params);
}

// 행사 상세 조회 (실제 API 사용)
export function useEvent(eventId: number) {
  return useGetEvent(eventId);
}

// 행사 신청 (실제 API 사용)
export function useApplyEvent() {
  const queryClient = useQueryClient();

  return useRegisterEvent({
    mutation: {
      onSuccess: (_data, variables) => {
        // 행사 상세 및 목록 새로고침
        void queryClient.invalidateQueries({ queryKey: eventKeys.detail(variables.eventId) });
        void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
        // 마이페이지 행사 신청 목록 새로고침
        void queryClient.invalidateQueries({ queryKey: myPageKeys.registrations() });
      },
    },
  });
}

// 행사 신청 취소 (실제 API 사용)
export function useCancelEventApplication() {
  const queryClient = useQueryClient();

  return useCancelRegistration({
    mutation: {
      onSuccess: (_data, variables) => {
        // 행사 상세 및 목록 새로고침
        void queryClient.invalidateQueries({ queryKey: eventKeys.detail(variables.eventId) });
        void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
        // 마이페이지 행사 신청 목록 새로고침
        void queryClient.invalidateQueries({ queryKey: myPageKeys.registrations() });
      },
    },
  });
}

// 행사 생성 (실제 API 사용)
export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useCreateEventMutation({
    mutation: {
      onSuccess: () => {
        // 행사 목록 새로고침
        void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
      },
    },
  });
}

// 행사 수정 (실제 API 사용)
export function useUpdateEvent() {
  const queryClient = useQueryClient();

  return useUpdateEventMutation({
    mutation: {
      onSuccess: (_data, variables) => {
        // 행사 상세 및 목록 새로고침
        void queryClient.invalidateQueries({ queryKey: eventKeys.detail(variables.eventId) });
        void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
      },
    },
  });
}

// 행사 삭제 (실제 API 사용)
export function useDeleteEvent() {
  const queryClient = useQueryClient();

  return useDeleteEventMutation({
    mutation: {
      onSuccess: () => {
        // 행사 목록 새로고침 (상세는 더 이상 존재하지 않음)
        void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
      },
    },
  });
}
