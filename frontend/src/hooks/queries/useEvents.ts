import { useQueryClient } from "@tanstack/react-query";

// Orval로 생성된 이벤트 API hooks
import {
  useGetEventList,
  useGetEvent,
  useCreateEvent as useCreateEventMutation,
  useUpdateEvent as useUpdateEventMutation,
  useDeleteEvent as useDeleteEventMutation,
  useCloseEvent as useCloseEventMutation,
} from "@/api/model/event/event";
import {
  useGetAdminEventList,
  useGetAdminEvent,
} from "@/api/model/admin-event/admin-event";
import {
  useRegisterEvent,
  useCancelRegistration,
} from "@/api/model/event-registration/event-registration";
import type {
  GetEventListParams,
  GetAdminEventListParams,
} from "@/api/model/models";
import { myPageKeys } from "@/hooks/queries/useMyPage";

// 쿼리 키 - Orval이 자동으로 생성하지만 invalidation을 위해 정의
export const eventKeys = {
  all: ["/api/v1/events"] as const,
  lists: () => [...eventKeys.all] as const,
  list: (filters?: GetEventListParams) =>
    [...eventKeys.all, ...(filters ? [filters] : [])] as const,
  details: () => [...eventKeys.all, "detail"] as const,
  detail: (id: number) => [`/api/v1/events/${id}`] as const,
};

export const adminEventKeys = {
  all: ["/api/v1/admin/events"] as const,
  lists: () => [...adminEventKeys.all] as const,
  list: (filters?: GetAdminEventListParams) =>
    [...adminEventKeys.all, ...(filters ? [filters] : [])] as const,
  detail: (id: number) => [`/api/v1/admin/events/${id}`] as const,
};

// 행사 목록 조회 (실제 API 사용)
export function useEvents(params?: GetEventListParams, enabled = true) {
  return useGetEventList(params, { query: { enabled } });
}

// 행사 상세 조회 (실제 API 사용)
export function useEvent(eventId: number, enabled = true) {
  return useGetEvent(eventId, { query: { enabled: enabled && !!eventId } });
}

// 관리자 행사 목록 조회 (OPERATOR 이상)
export function useAdminEvents(
  params?: GetAdminEventListParams,
  enabled = true,
) {
  return useGetAdminEventList(params, { query: { enabled } });
}

// 관리자 행사 상세 조회 (OPERATOR 이상)
export function useAdminEvent(eventId: number, enabled = true) {
  return useGetAdminEvent(eventId, {
    query: { enabled: enabled && !!eventId },
  });
}

// 행사 관련 쿼리 전체 무효화 헬퍼
function invalidateEventQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  eventId?: number,
) {
  void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
  void queryClient.invalidateQueries({ queryKey: adminEventKeys.lists() });
  if (eventId) {
    void queryClient.invalidateQueries({
      queryKey: eventKeys.detail(eventId),
    });
    void queryClient.invalidateQueries({
      queryKey: adminEventKeys.detail(eventId),
    });
  }
}

// 행사 신청 (실제 API 사용)
export function useApplyEvent() {
  const queryClient = useQueryClient();

  return useRegisterEvent({
    mutation: {
      onSuccess: (_data, variables) => {
        invalidateEventQueries(queryClient, variables.eventId);
        void queryClient.invalidateQueries({
          queryKey: myPageKeys.registrations(),
        });
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
        invalidateEventQueries(queryClient, variables.eventId);
        void queryClient.invalidateQueries({
          queryKey: myPageKeys.registrations(),
        });
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
        invalidateEventQueries(queryClient);
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
        invalidateEventQueries(queryClient, variables.eventId);
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
        invalidateEventQueries(queryClient);
      },
    },
  });
}

// 행사 신청 조기 마감 (실제 API 사용)
export function useCloseEvent() {
  const queryClient = useQueryClient();

  return useCloseEventMutation({
    mutation: {
      onSuccess: (_data, variables) => {
        invalidateEventQueries(queryClient, variables.eventId);
      },
    },
  });
}
