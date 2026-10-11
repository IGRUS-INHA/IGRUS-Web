import { useState } from "react";
import { Link } from "react-router-dom";
import { Bell } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { FullPageSpinner } from "@/components/ui";
import { Pagination } from "@/components/board/Pagination";
import { useUIStore } from "@/stores";
import {
  useCancelRequestList,
  useCancelEventRegistrationByAdmin,
  useRejectEventCancelRequest,
} from "@/hooks/queries/useEventRegistrations";
import { isForbiddenError, hasErrorCode, getErrorMessage } from "@/utils/error";
import { formatDateTime } from "@/utils";

const PAGE_SIZE = 20;

export default function CancelRequestNotifyPage() {
  const addToast = useUIStore((s) => s.addToast);
  const [page, setPage] = useState(1);

  const { data, isLoading, error } = useCancelRequestList({
    page: page - 1,
    size: PAGE_SIZE,
  });
  const { mutate: cancelRegistration, isPending: isCanceling } =
    useCancelEventRegistrationByAdmin();
  const { mutate: rejectCancelRequest, isPending: isRejecting } =
    useRejectEventCancelRequest();
  const isBusy = isCanceling || isRejecting;

  // 다른 운영진이 먼저 처리한 요청 — 목록은 훅의 onSettled에서 새로고침된다
  const handleMutationError = (mutationError: unknown, action: string) => {
    if (
      hasErrorCode(mutationError, "EVENT_CANCEL_REQUEST_NOT_PENDING") ||
      hasErrorCode(mutationError, "EVENT_INVALID_REGISTRATION_STATUS")
    ) {
      addToast({ type: "error", message: "이미 처리된 요청입니다." });
      return;
    }
    addToast({
      type: "error",
      message: isForbiddenError(mutationError)
        ? `${action} 권한이 없습니다.`
        : getErrorMessage(mutationError),
    });
  };

  const handleCancel = (registrationId: number, userName: string) => {
    if (
      !window.confirm(
        `${userName}님의 신청을 취소하시겠습니까?\n취소 후에는 되돌릴 수 없습니다.`,
      )
    )
      return;
    cancelRegistration(
      { registrationId },
      {
        onSuccess: () =>
          addToast({ type: "success", message: "신청을 취소했습니다." }),
        onError: (mutationError: unknown) =>
          handleMutationError(mutationError, "취소"),
      },
    );
  };

  const handleReject = (registrationId: number, userName: string) => {
    if (
      !window.confirm(
        `${userName}님의 취소 요청을 거절하시겠습니까?\n거절하면 취소 요청이 없던 것으로 처리되며 신청은 유지됩니다.`,
      )
    )
      return;
    rejectCancelRequest(
      { registrationId },
      {
        onSuccess: () =>
          addToast({
            type: "success",
            message: "취소 요청을 거절했습니다. 신청은 유지됩니다.",
          }),
        onError: (mutationError: unknown) =>
          handleMutationError(mutationError, "거절"),
      },
    );
  };

  if (isLoading) return <FullPageSpinner />;

  if (isForbiddenError(error)) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        취소 요청 알림을 볼 권한이 없습니다.
      </div>
    );
  }

  if (error || data?.status !== 200) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        {error
          ? getErrorMessage(error)
          : "취소 요청 알림을 불러오지 못했습니다."}
      </div>
    );
  }

  const requests = data.data.content ?? [];
  const totalPages = data.data.totalPages ?? 0;

  return (
    <div className="animate-in fade-in duration-300">
      <div className="mb-s6 space-y-s2">
        <div className="flex items-center gap-s3">
          <Bell size={24} className="text-primary" />
          <h1 className="typo-h2 font-bold">취소 요청 알림</h1>
        </div>
        <p className="typo-b2 text-muted-foreground">
          회원이 보낸 행사 신청 취소 요청입니다. 사유를 확인한 뒤 취소를
          처리해주세요.
        </p>
      </div>

      {requests.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          취소 요청이 없습니다.
        </div>
      ) : (
        <div className="space-y-s3">
          {requests.map((request) => {
            const { registrationId } = request;
            const userName = request.userName ?? "-";
            return (
              <Card key={registrationId} className="p-s5 space-y-s3">
                <div className="flex items-start justify-between gap-s4">
                  <div className="min-w-0 space-y-s1">
                    {request.eventId !== undefined ? (
                      <Link
                        to={`/events/${request.eventId}/registrations`}
                        className="typo-b1 font-bold hover:underline"
                      >
                        {request.eventTitle ?? "-"}
                      </Link>
                    ) : (
                      <p className="typo-b1 font-bold">
                        {request.eventTitle ?? "-"}
                      </p>
                    )}
                    <p className="typo-b2 text-muted-foreground flex items-center gap-s2 flex-wrap">
                      <span>
                        {userName}
                        {request.studentId ? ` (${request.studentId})` : ""}
                      </span>
                      {request.isExternal && (
                        <span className="px-s2 py-0.5 rounded-r2 typo-c2 font-bold bg-secondary text-secondary-foreground">
                          외부인
                        </span>
                      )}
                      {request.cancelRequestedAt && (
                        <span>{formatDateTime(request.cancelRequestedAt)}</span>
                      )}
                    </p>
                  </div>
                  {request.status === "CANCELED" ? (
                    <span className="shrink-0 px-s2 py-0.5 rounded-r2 typo-c2 font-bold bg-muted text-muted-foreground">
                      취소됨
                    </span>
                  ) : (
                    registrationId !== undefined && (
                      <div className="flex shrink-0 gap-s2">
                        <Button
                          size="xs"
                          variant="destructive"
                          onClick={() => handleCancel(registrationId, userName)}
                          disabled={isBusy}
                        >
                          취소하기
                        </Button>
                        <Button
                          size="xs"
                          variant="outline"
                          onClick={() => handleReject(registrationId, userName)}
                          disabled={isBusy}
                        >
                          거절
                        </Button>
                      </div>
                    )
                  )}
                </div>
                <p className="typo-b2 whitespace-pre-wrap break-words rounded-r2 bg-muted/50 p-s3">
                  {request.cancelReason ?? "-"}
                </p>
              </Card>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-s6">
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
}
