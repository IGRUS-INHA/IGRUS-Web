import { useQueryClient } from "@tanstack/react-query";
import {
  useGetRegistrationList,
  useApproveRegistration,
  useRejectRegistration,
  useRevertRegistration,
} from "@/api/model/event-registration/event-registration";
import type { GetRegistrationListParams } from "@/api/model/models";
import { eventKeys } from "@/hooks/queries/useEvents";

// 쿼리 키 - 캐시 무효화를 위해 정의
export const registrationKeys = {
  all: (eventId: number) =>
    [`/api/v1/events/${eventId}/registrations`] as const,
  list: (eventId: number, params?: GetRegistrationListParams) =>
    [...registrationKeys.all(eventId), ...(params ? [params] : [])] as const,
};

// 신청자 목록 조회
export function useRegistrationList(
  eventId: number,
  params?: GetRegistrationListParams,
) {
  return useGetRegistrationList(eventId, params);
}

// 신청 승인 (선발제)
export function useApproveEventRegistration(eventId: number) {
  const queryClient = useQueryClient();
  return useApproveRegistration({
    mutation: {
      onSuccess: () => {
        void queryClient.invalidateQueries({
          queryKey: registrationKeys.all(eventId),
        });
        void queryClient.invalidateQueries({
          queryKey: eventKeys.detail(eventId),
        });
      },
    },
  });
}

// 신청 거절 (선발제)
export function useRejectEventRegistration(eventId: number) {
  const queryClient = useQueryClient();
  return useRejectRegistration({
    mutation: {
      onSuccess: () => {
        void queryClient.invalidateQueries({
          queryKey: registrationKeys.all(eventId),
        });
        void queryClient.invalidateQueries({
          queryKey: eventKeys.detail(eventId),
        });
      },
    },
  });
}

// 승인/거절 되돌리기 (선발제)
export function useRevertEventRegistration(eventId: number) {
  const queryClient = useQueryClient();
  return useRevertRegistration({
    mutation: {
      onSuccess: () => {
        void queryClient.invalidateQueries({
          queryKey: registrationKeys.all(eventId),
        });
        void queryClient.invalidateQueries({
          queryKey: eventKeys.detail(eventId),
        });
      },
    },
  });
}
