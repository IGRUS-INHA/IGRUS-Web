import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getRegistrationList,
  useGetCancelRequestList,
  getGetCancelRequestListQueryKey,
  useCancelRegistrationByAdmin,
  useRejectCancelRequest,
  useApproveRegistration,
  useRejectRegistration,
  useRevertRegistration,
} from "@/api/model/event-registration/event-registration";
import type {
  GetCancelRequestListParams,
  GetRegistrationListParams,
  RegistrationListResponse,
} from "@/api/model/models";
import { eventKeys, adminEventKeys } from "@/hooks/queries/useEvents";

// 쿼리 키 - 캐시 무효화를 위해 정의
export const registrationKeys = {
  all: (eventId: number) =>
    [`/api/v1/events/${eventId}/registrations`] as const,
  list: (eventId: number, params?: GetRegistrationListParams) =>
    [...registrationKeys.all(eventId), ...(params ? [params] : [])] as const,
};

const REGISTRATION_FETCH_SIZE = 100;

// 전체 신청자 조회 — 모든 페이지를 순회해 신청일 최신순으로 반환한다.
// 오름차순으로 순회해 조회 도중 들어온 새 신청이 앞 페이지를 밀어내지 않게 하고,
// 신청 시각이 같은 행이 페이지마다 다른 순서로 나와 중복/누락되지 않도록 id를 타이브레이커로 둔다.
async function fetchAllRegistrations(
  eventId: number,
): Promise<RegistrationListResponse[]> {
  const all: RegistrationListResponse[] = [];
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const response = await getRegistrationList(eventId, {
      page,
      size: REGISTRATION_FETCH_SIZE,
      sort: ["registeredAt,ASC", "id,ASC"],
    });
    // customFetch는 실패 시 ApiError를 던지므로 타입 좁히기용 — 일부만 모인 목록을 반환하지 않는다
    if (response.status !== 200) {
      throw new Error("신청자 목록을 불러오지 못했습니다.");
    }
    all.push(...(response.data.content ?? []));
    totalPages = response.data.totalPages ?? 0;
    page += 1;
  }
  return all.reverse();
}

// 신청자 전체 목록 조회 (화면 목록·통계·CSV 내보내기 공용)
export function useAllRegistrations(eventId: number) {
  return useQuery({
    queryKey: [...registrationKeys.all(eventId), "all"],
    queryFn: () => fetchAllRegistrations(eventId),
    enabled: !!eventId,
  });
}

// 신청 취소 요청 알림 목록 조회 (OPERATOR 이상)
export function useCancelRequestList(params?: GetCancelRequestListParams) {
  return useGetCancelRequestList(params);
}

// 취소 요청 처리(취소/거절)는 여러 행사에 걸친 목록에서 호출되므로 행사 관련 쿼리를 모두 무효화한다
function invalidateCancelRequestQueries(
  queryClient: ReturnType<typeof useQueryClient>,
) {
  void queryClient.invalidateQueries({
    queryKey: getGetCancelRequestListQueryKey(),
  });
  void queryClient.invalidateQueries({
    predicate: ({ queryKey }) => {
      const root = queryKey[0];
      return (
        typeof root === "string" &&
        (root.startsWith(eventKeys.all[0]) ||
          root.startsWith(adminEventKeys.all[0]))
      );
    },
  });
}

// 운영진의 신청 취소 처리
export function useCancelEventRegistrationByAdmin() {
  const queryClient = useQueryClient();
  return useCancelRegistrationByAdmin({
    // 실패(다른 운영진이 먼저 처리한 경우 등)해도 목록이 최신 상태로 돌아오게 onSettled에서 무효화한다
    mutation: { onSettled: () => invalidateCancelRequestQueries(queryClient) },
  });
}

// 운영진의 취소 요청 거절 — 요청이 없던 것으로 되돌아가고 신청은 유지된다
export function useRejectEventCancelRequest() {
  const queryClient = useQueryClient();
  return useRejectCancelRequest({
    // 실패(다른 운영진이 먼저 처리한 경우 등)해도 목록이 최신 상태로 돌아오게 onSettled에서 무효화한다
    mutation: { onSettled: () => invalidateCancelRequestQueries(queryClient) },
  });
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
