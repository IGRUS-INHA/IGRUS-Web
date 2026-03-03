import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/stores";
import {
  useGetAdminEventList,
  usePublishEvent,
  useUnpublishEvent,
} from "@/api/model/admin-event/admin-event";
import {
  useCloseEvent,
  useReopenRegistration,
  useCancelEvent,
  useReactivateEvent,
} from "@/api/model/event/event";
import type { AdminEventListResponse } from "@/api/model/models";
import type { GetAdminEventListVisibility } from "@/api/model/models/getAdminEventListVisibility";
import type { GetAdminEventListEventStatus } from "@/api/model/models/getAdminEventListEventStatus";
import type { GetAdminEventListRegistrationStatus } from "@/api/model/models/getAdminEventListRegistrationStatus";
import { formatDate } from "@/utils/date";
import { eventKeys } from "@/hooks/queries/useEvents";

// -- 뱃지 스타일 매핑 --

const VISIBILITY_BADGE: Record<string, string> = {
  PUBLISHED: "bg-success/10 text-success",
  UNPUBLISHED: "bg-muted text-muted-foreground",
};
const VISIBILITY_LABEL: Record<string, string> = {
  PUBLISHED: "공개",
  UNPUBLISHED: "비공개",
};

const EVENT_STATUS_BADGE: Record<string, string> = {
  UPCOMING: "bg-primary/10 text-primary",
  ONGOING: "bg-success/10 text-success",
  COMPLETED: "bg-muted text-muted-foreground",
  CANCELED: "bg-destructive/10 text-destructive",
};
const EVENT_STATUS_LABEL: Record<string, string> = {
  UPCOMING: "예정",
  ONGOING: "진행중",
  COMPLETED: "완료",
  CANCELED: "취소됨",
};

const REG_STATUS_BADGE: Record<string, string> = {
  NOT_STARTED: "bg-muted text-muted-foreground",
  OPEN: "bg-success/10 text-success",
  CLOSED: "bg-warning/10 text-warning",
};
const REG_STATUS_LABEL: Record<string, string> = {
  NOT_STARTED: "대기",
  OPEN: "모집중",
  CLOSED: "마감",
};

// -- 필터 옵션 --

const VISIBILITY_OPTIONS: {
  value: "" | GetAdminEventListVisibility;
  label: string;
}[] = [
  { value: "", label: "전체 공개상태" },
  { value: "PUBLISHED", label: "공개" },
  { value: "UNPUBLISHED", label: "비공개" },
];

const EVENT_STATUS_OPTIONS: {
  value: "" | GetAdminEventListEventStatus;
  label: string;
}[] = [
  { value: "", label: "전체 행사상태" },
  { value: "UPCOMING", label: "예정" },
  { value: "ONGOING", label: "진행중" },
  { value: "COMPLETED", label: "완료" },
  { value: "CANCELED", label: "취소됨" },
];

const REG_STATUS_OPTIONS: {
  value: "" | GetAdminEventListRegistrationStatus;
  label: string;
}[] = [
  { value: "", label: "전체 등록상태" },
  { value: "NOT_STARTED", label: "대기" },
  { value: "OPEN", label: "모집중" },
  { value: "CLOSED", label: "마감" },
];

// -- 사유 입력 다이얼로그 --

function ReasonDialog({
  title,
  onConfirm,
  onCancel,
}: {
  title: string;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}) {
  const [reason, setReason] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-r4 border bg-card border-border p-s6 shadow-xl space-y-s4">
        <h3 className="text-lg font-bold">{title}</h3>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="사유를 입력하세요"
          className="w-full h-24 px-s3 py-s2 rounded-r2 border border-border bg-background text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <div className="flex justify-end gap-s2">
          <button
            type="button"
            onClick={onCancel}
            className="px-s4 py-s2 rounded-r2 text-sm font-medium border border-border hover:bg-muted transition cursor-pointer"
          >
            취소
          </button>
          <button
            type="button"
            onClick={() => onConfirm(reason)}
            disabled={!reason.trim()}
            className="px-s4 py-s2 rounded-r2 text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
}

// -- Badge 컴포넌트 --

function Badge({
  value,
  map,
  labelMap,
}: {
  value?: string;
  map: Record<string, string>;
  labelMap: Record<string, string>;
}) {
  if (!value) return <span className="text-muted-foreground">-</span>;
  return (
    <span
      className={cn(
        "px-2 py-1 rounded-r2 typo-c2 font-bold whitespace-nowrap",
        map[value] ?? "bg-muted text-muted-foreground",
      )}
    >
      {labelMap[value] ?? value}
    </span>
  );
}

// -- 메인 컴포넌트 --

export default function EventsTab() {
  const queryClient = useQueryClient();
  const addToast = useUIStore((s) => s.addToast);

  // 필터 상태
  const [visibility, setVisibility] = useState<
    "" | GetAdminEventListVisibility
  >("");
  const [eventStatus, setEventStatus] = useState<
    "" | GetAdminEventListEventStatus
  >("");
  const [regStatus, setRegStatus] = useState<
    "" | GetAdminEventListRegistrationStatus
  >("");

  // 사유 입력 다이얼로그 상태
  const [reasonDialog, setReasonDialog] = useState<{
    title: string;
    onConfirm: (reason: string) => void;
  } | null>(null);

  // 확장된 행 (액션 버튼 표시용)
  const [expandedId, setExpandedId] = useState<number | null>(null);

  // 관리자 행사 목록 조회
  const { data: response, isLoading } = useGetAdminEventList({
    ...(visibility && { visibility }),
    ...(eventStatus && { eventStatus }),
    ...(regStatus && { registrationStatus: regStatus }),
  });

  const events: AdminEventListResponse[] =
    response?.status === 200 ? response.data : [];

  // 쿼리 무효화 헬퍼
  const invalidateAll = () => {
    void queryClient.invalidateQueries({ queryKey: ["/api/v1/admin/events"] });
    void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
  };

  // -- Mutations --

  const { mutate: publish, isPending: isPublishing } = usePublishEvent({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "행사가 공개되었습니다." });
        invalidateAll();
      },
      onError: () =>
        addToast({ type: "error", message: "공개 처리에 실패했습니다." }),
    },
  });

  const { mutate: unpublish, isPending: isUnpublishing } = useUnpublishEvent({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "행사가 비공개 처리되었습니다." });
        invalidateAll();
      },
      onError: () =>
        addToast({ type: "error", message: "비공개 처리에 실패했습니다." }),
    },
  });

  const { mutate: closeEvent, isPending: isClosingEvent } = useCloseEvent({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "등록이 마감되었습니다." });
        invalidateAll();
      },
      onError: () =>
        addToast({ type: "error", message: "등록 마감에 실패했습니다." }),
    },
  });

  const { mutate: reopenReg, isPending: isReopening } = useReopenRegistration({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "등록이 재오픈되었습니다." });
        invalidateAll();
      },
      onError: () =>
        addToast({ type: "error", message: "등록 재오픈에 실패했습니다." }),
    },
  });

  const { mutate: cancelEvent, isPending: isCancelingEvent } = useCancelEvent({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "행사가 취소되었습니다." });
        invalidateAll();
      },
      onError: () =>
        addToast({ type: "error", message: "행사 취소에 실패했습니다." }),
    },
  });

  const { mutate: reactivate, isPending: isReactivating } = useReactivateEvent({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "행사가 재활성화되었습니다." });
        invalidateAll();
      },
      onError: () =>
        addToast({ type: "error", message: "행사 재활성화에 실패했습니다." }),
    },
  });

  const isMutating =
    isPublishing ||
    isUnpublishing ||
    isClosingEvent ||
    isReopening ||
    isCancelingEvent ||
    isReactivating;

  // -- 액션 핸들러 --

  const handlePublish = (eventId: number) => {
    publish({ eventId });
  };

  const handleUnpublish = (eventId: number) => {
    unpublish({ eventId });
  };

  const handleClose = (eventId: number) => {
    setReasonDialog({
      title: "등록 마감 사유",
      onConfirm: (reason) => {
        closeEvent({ eventId, data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  const handleReopen = (eventId: number) => {
    setReasonDialog({
      title: "등록 재오픈 사유",
      onConfirm: (reason) => {
        reopenReg({ eventId, data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  const handleCancel = (eventId: number) => {
    setReasonDialog({
      title: "행사 취소 사유",
      onConfirm: (reason) => {
        cancelEvent({ eventId, data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  const handleReactivate = (eventId: number) => {
    setReasonDialog({
      title: "행사 재활성화 사유",
      onConfirm: (reason) => {
        reactivate({ eventId, data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  // -- 행별 액션 버튼 결정 --

  function getActions(ev: AdminEventListResponse) {
    const actions: {
      label: string;
      onClick: () => void;
      variant?: "destructive" | "default";
    }[] = [];
    const id = ev.id;
    if (!id) return actions;

    // 공개/비공개 토글
    if (ev.visibility === "UNPUBLISHED") {
      actions.push({ label: "공개", onClick: () => handlePublish(id) });
    } else if (ev.visibility === "PUBLISHED") {
      actions.push({ label: "비공개", onClick: () => handleUnpublish(id) });
    }

    // 등록 마감/재오픈
    if (ev.registrationStatus === "OPEN") {
      actions.push({ label: "등록 마감", onClick: () => handleClose(id) });
    }
    if (
      ev.registrationStatus === "CLOSED" &&
      (ev.eventStatus === "UPCOMING" || ev.eventStatus === "ONGOING")
    ) {
      actions.push({ label: "등록 재오픈", onClick: () => handleReopen(id) });
    }

    // 행사 취소/재활성화
    if (ev.eventStatus === "UPCOMING" || ev.eventStatus === "ONGOING") {
      actions.push({
        label: "행사 취소",
        onClick: () => handleCancel(id),
        variant: "destructive",
      });
    }
    if (ev.eventStatus === "CANCELED") {
      actions.push({ label: "재활성화", onClick: () => handleReactivate(id) });
    }

    return actions;
  }

  // -- 렌더링 --

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-s5">
      {/* 필터 영역 */}
      <div className="rounded-r4 border bg-card border-border p-s5">
        <div className="flex flex-col sm:flex-row gap-s3">
          <select
            value={visibility}
            onChange={(e) => setVisibility(e.target.value as typeof visibility)}
            className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
          >
            {VISIBILITY_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <select
            value={eventStatus}
            onChange={(e) =>
              setEventStatus(e.target.value as typeof eventStatus)
            }
            className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
          >
            {EVENT_STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <select
            value={regStatus}
            onChange={(e) => setRegStatus(e.target.value as typeof regStatus)}
            className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
          >
            {REG_STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 결과 요약 */}
      <p className="text-sm text-muted-foreground">
        총 <span className="font-semibold">{events.length}</span>개
      </p>

      {/* 테이블 */}
      <div className="overflow-x-auto rounded-r4 border bg-card border-border">
        <table className="w-full text-center">
          <thead>
            <tr className="typo-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
              <th className="pb-s4 pt-s4 px-s3 font-bold">제목</th>
              <th className="pb-s4 pt-s4 px-s3 font-bold">공개</th>
              <th className="pb-s4 pt-s4 px-s3 font-bold">행사상태</th>
              <th className="pb-s4 pt-s4 px-s3 font-bold">등록상태</th>
              <th className="pb-s4 pt-s4 px-s3 font-bold">인원</th>
              <th className="pb-s4 pt-s4 px-s3 font-bold hidden lg:table-cell">
                행사 일시
              </th>
              <th className="pb-s4 pt-s4 px-s3 font-bold hidden lg:table-cell">
                등록 마감
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {events.map((ev) => {
              const isExpanded = expandedId === ev.id;
              const actions = getActions(ev);

              return (
                <tr
                  key={ev.id}
                  className={cn(
                    "group cursor-pointer transition-colors",
                    isExpanded ? "bg-muted/30" : "hover:bg-muted/20",
                  )}
                  onClick={() =>
                    setExpandedId(isExpanded ? null : (ev.id ?? null))
                  }
                >
                  <td className="py-s3 px-s3 text-left">
                    <div>
                      <p className="font-medium text-sm truncate max-w-[200px]">
                        {ev.title}
                      </p>
                      {/* 확장 시 액션 버튼 */}
                      {isExpanded && actions.length > 0 && (
                        <div
                          className="flex flex-wrap gap-s2 mt-s2"
                          onClick={(e) => e.stopPropagation()}
                        >
                          {actions.map((action) => (
                            <button
                              key={action.label}
                              type="button"
                              onClick={action.onClick}
                              disabled={isMutating}
                              className={cn(
                                "px-s3 py-s1 rounded-r2 text-xs font-medium transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed",
                                action.variant === "destructive"
                                  ? "bg-destructive/10 text-destructive hover:bg-destructive/20"
                                  : "bg-primary/10 text-primary hover:bg-primary/20",
                              )}
                            >
                              {action.label}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="py-s3 px-s3">
                    <Badge
                      value={ev.visibility}
                      map={VISIBILITY_BADGE}
                      labelMap={VISIBILITY_LABEL}
                    />
                  </td>
                  <td className="py-s3 px-s3">
                    <Badge
                      value={ev.eventStatus}
                      map={EVENT_STATUS_BADGE}
                      labelMap={EVENT_STATUS_LABEL}
                    />
                  </td>
                  <td className="py-s3 px-s3">
                    <Badge
                      value={ev.registrationStatus}
                      map={REG_STATUS_BADGE}
                      labelMap={REG_STATUS_LABEL}
                    />
                  </td>
                  <td className="py-s3 px-s3 text-sm">
                    {ev.currentCount ?? 0}/{ev.capacity ?? 0}
                  </td>
                  <td className="py-s3 px-s3 text-sm hidden lg:table-cell whitespace-nowrap">
                    {formatDate(ev.eventStartAt)}
                  </td>
                  <td className="py-s3 px-s3 text-sm hidden lg:table-cell whitespace-nowrap">
                    {formatDate(ev.registrationEndAt)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {events.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">
            행사가 없습니다.
          </div>
        )}
      </div>

      {/* 사유 입력 다이얼로그 */}
      {reasonDialog && (
        <ReasonDialog
          title={reasonDialog.title}
          onConfirm={reasonDialog.onConfirm}
          onCancel={() => setReasonDialog(null)}
        />
      )}
    </div>
  );
}
