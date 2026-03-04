import { useParams, useNavigate } from "react-router-dom";
import { FullPageSpinner } from "@/components/ui";
import { useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  Calendar,
  MapPin,
  Users,
  Clock,
  ChevronLeft,
  ChevronRight,
  MoreHorizontal,
  Edit,
  Trash2,
  Lock,
  Eye,
  EyeOff,
  LockOpen,
  XCircle,
  RotateCcw,
} from "lucide-react";
import MarkdownPreview from "@uiw/react-markdown-preview";
import {
  useEvent,
  useAdminEvent,
  useApplyEvent,
  useCancelEventApplication,
  useDeleteEvent,
  useCloseEvent,
  eventKeys,
  adminEventKeys,
} from "@/hooks/queries/useEvents";
import {
  useCancelEvent,
  useReactivateEvent,
  useReopenRegistration,
} from "@/api/model/event/event";
import {
  usePublishEvent,
  useUnpublishEvent,
} from "@/api/model/admin-event/admin-event";
import { registrationKeys } from "@/hooks/queries/useEventRegistrations";
import { useAuth } from "@/hooks";
import { useEffect, useRef, useState } from "react";
import {
  getErrorMessage,
  isForbiddenError,
  isEventAccessDenied,
  isEventAlreadyRegistered,
  isEventCapacityFull,
  isEventRegistrationClosed,
  isEventNotFound,
  isEventOperatorRequired,
  hasErrorCode,
} from "@/utils/error";
import { myPageKeys } from "@/hooks/queries/useMyPage";
import { formatDateTime } from "@/utils/date";
import {
  EVENT_STATUS_BADGE,
  EVENT_STATUS_LABEL,
  REG_STATUS_BADGE,
  REG_STATUS_LABEL,
  VISIBILITY_BADGE,
  VISIBILITY_LABEL,
} from "@/constants/eventStatus";
import ReasonDialog from "@/components/feature/event/ReasonDialog";

export default function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isOperator = user?.role === "OPERATOR" || user?.role === "ADMIN";
  const numericEventId = Number(eventId);

  // OPERATOR+는 admin API로 비공개 행사도 조회 가능
  const publicQuery = useEvent(numericEventId, !isOperator);
  const adminQuery = useAdminEvent(numericEventId, isOperator);
  const {
    data: eventResponse,
    isLoading,
    error,
  } = isOperator ? adminQuery : publicQuery;

  const { mutate: applyEvent, isPending: isApplying } = useApplyEvent();
  const { mutate: cancelRegistration, isPending: isCanceling } =
    useCancelEventApplication();
  const { mutate: deleteEvent, isPending: isDeleting } = useDeleteEvent();
  const { mutate: closeEvent, isPending: isClosing } = useCloseEvent();

  // 쿼리 무효화 헬퍼
  const invalidateEvent = () => {
    if (!eventId) return;
    void queryClient.invalidateQueries({
      queryKey: eventKeys.detail(Number(eventId)),
    });
    void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
    void queryClient.invalidateQueries({
      queryKey: adminEventKeys.detail(Number(eventId)),
    });
    void queryClient.invalidateQueries({ queryKey: adminEventKeys.lists() });
  };

  // 관리 mutation 훅
  const { mutate: publishEvent, isPending: isPublishing } = usePublishEvent({
    mutation: {
      onSuccess: invalidateEvent,
      onError: () => alert("공개 처리에 실패했습니다."),
    },
  });

  const { mutate: unpublishEvent, isPending: isUnpublishing } =
    useUnpublishEvent({
      mutation: {
        onSuccess: invalidateEvent,
        onError: () => alert("비공개 처리에 실패했습니다."),
      },
    });

  const { mutate: cancelEvent, isPending: isCancelingEvent } = useCancelEvent({
    mutation: {
      onSuccess: invalidateEvent,
      onError: () => alert("행사 취소에 실패했습니다."),
    },
  });

  const { mutate: reactivateEvent, isPending: isReactivating } =
    useReactivateEvent({
      mutation: {
        onSuccess: invalidateEvent,
        onError: () => alert("행사 재활성화에 실패했습니다."),
      },
    });

  const { mutate: reopenRegistration, isPending: isReopening } =
    useReopenRegistration({
      mutation: {
        onSuccess: invalidateEvent,
        onError: () => alert("등록 재오픈에 실패했습니다."),
      },
    });

  const isMutating =
    isPublishing ||
    isUnpublishing ||
    isClosing ||
    isReopening ||
    isCancelingEvent ||
    isReactivating ||
    isDeleting;

  // API 응답 데이터 추출
  const event = eventResponse?.data;

  // 이미지 캐러셀 상태
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // More Menu 상태
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);
  const moreMenuRef = useRef<HTMLDivElement>(null);

  // ReasonDialog 상태
  const [reasonDialog, setReasonDialog] = useState<{
    title: string;
    onConfirm: (reason: string) => void;
  } | null>(null);

  // 외부 클릭 감지 (More Menu 닫기)
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        moreMenuRef.current &&
        !moreMenuRef.current.contains(e.target as Node)
      ) {
        setIsMoreMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // -- 사용자 액션 핸들러 --

  const handleApply = () => {
    if (!eventId) return;
    applyEvent(
      { eventId: Number(eventId) },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/events/${eventId}`],
          });
          void queryClient.invalidateQueries({
            queryKey: myPageKeys.registrations(),
          });
        },
        onError: (error: unknown) => {
          if (isEventAlreadyRegistered(error)) {
            alert("이미 신청한 행사입니다.");
          } else if (isEventCapacityFull(error)) {
            alert("정원이 마감되었습니다.");
          } else if (isEventRegistrationClosed(error)) {
            alert("신청 기간이 종료되었습니다.");
          } else if (hasErrorCode(error, "EVENT_UNPUBLISHED")) {
            alert("비공개인 행사는 신청할 수 없습니다.");
          } else if (isForbiddenError(error)) {
            alert("행사 신청 권한이 없습니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    );
  };

  const handleCancelRegistration = () => {
    if (!eventId) return;

    const isReregistrationBlocked =
      event?.closeReason === "DEADLINE_PASSED" ||
      event?.closeReason === "MANUAL_CLOSE";

    const message = isReregistrationBlocked
      ? "신청을 취소하면 재신청이 불가능합니다. 정말 취소하시겠습니까?"
      : "행사 신청을 취소하시겠습니까?";

    if (!confirm(message)) return;

    cancelRegistration(
      { eventId: Number(eventId) },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/events/${eventId}`],
          });
          void queryClient.invalidateQueries({
            queryKey: myPageKeys.registrations(),
          });
        },
        onError: (error: unknown) => {
          if (hasErrorCode(error, "EVENT_ALREADY_CANCELED")) {
            alert("이미 취소된 신청입니다.");
          } else if (hasErrorCode(error, "CANCEL_DEADLINE_PASSED")) {
            alert("취소 가능 기간이 지났습니다.");
          } else if (hasErrorCode(error, "EVENT_NOT_CANCELABLE")) {
            alert("취소된 행사의 신청은 취소할 수 없습니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    );
  };

  // -- 관리 액션 핸들러 --

  const handleEdit = () => {
    setIsMoreMenuOpen(false);
    navigate(`/events/${eventId}/edit`);
  };

  const handlePublish = () => {
    if (!eventId) return;
    setIsMoreMenuOpen(false);
    publishEvent({ eventId: Number(eventId) });
  };

  const handleUnpublish = () => {
    if (!eventId) return;
    setIsMoreMenuOpen(false);
    unpublishEvent({ eventId: Number(eventId) });
  };

  const handleCloseEvent = () => {
    if (!eventId) return;
    setIsMoreMenuOpen(false);
    setReasonDialog({
      title: "모집 마감 사유",
      onConfirm: (reason) => {
        closeEvent(
          { eventId: Number(eventId), data: { reason } },
          {
            onError: (error: unknown) => {
              if (isForbiddenError(error) || isEventOperatorRequired(error)) {
                alert("행사 마감 권한이 없습니다.");
              } else {
                alert(getErrorMessage(error));
              }
            },
          },
        );
        setReasonDialog(null);
      },
    });
  };

  const handleReopenRegistration = () => {
    if (!eventId) return;
    setIsMoreMenuOpen(false);
    setReasonDialog({
      title: "모집 재오픈 사유",
      onConfirm: (reason) => {
        reopenRegistration({ eventId: Number(eventId), data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  const handleCancelEvent = () => {
    if (!eventId) return;
    setIsMoreMenuOpen(false);
    setReasonDialog({
      title: "행사 취소 사유",
      onConfirm: (reason) => {
        cancelEvent({ eventId: Number(eventId), data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  const handleReactivate = () => {
    if (!eventId) return;
    setIsMoreMenuOpen(false);
    setReasonDialog({
      title: "행사 재활성화 사유",
      onConfirm: (reason) => {
        reactivateEvent({ eventId: Number(eventId), data: { reason } });
        setReasonDialog(null);
      },
    });
  };

  const handleDelete = () => {
    if (
      !window.confirm(
        "이 행사를 삭제하시겠습니까?\n삭제된 행사는 복구할 수 없습니다.",
      )
    ) {
      return;
    }

    setIsMoreMenuOpen(false);
    deleteEvent(
      { eventId: Number(eventId) },
      {
        onSuccess: () => {
          navigate("/events");
        },
        onError: (error: unknown) => {
          if (hasErrorCode(error, "EVENT_NOT_DELETABLE")) {
            alert("신청자가 있는 행사는 삭제할 수 없습니다.");
          } else if (
            isForbiddenError(error) ||
            isEventOperatorRequired(error)
          ) {
            alert("행사 삭제 권한이 없습니다.");
          } else if (isEventNotFound(error)) {
            alert("이미 삭제된 행사입니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    );
  };

  if (isLoading) {
    return <FullPageSpinner />;
  }

  const isForbidden = isForbiddenError(error) || isEventAccessDenied(error);

  if (isForbidden) {
    return (
      <div className="text-center py-12 space-y-s4">
        <p className="text-muted-foreground">
          정회원 승인 후 행사 상세 조회가 가능합니다.
        </p>
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="text-sm text-primary hover:underline cursor-pointer"
        >
          행사 목록으로 돌아가기
        </button>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        행사를 찾을 수 없습니다.
      </div>
    );
  }

  // 상태 확인
  const isOpen = event.isRegistrable ?? false;
  const hasApplied = event.isRegistered ?? false;
  const isUnpublished = event.visibility === "UNPUBLISHED";
  const canApply = user && isOpen && !hasApplied && !isUnpublished;
  // CANCELED 상태의 행사는 신청 취소 불가 (백엔드: EventRegistrationService.cancelRegistration)
  const canCancel = user && hasApplied && event.eventStatus !== "CANCELED";
  const canManage = event.canEdit;
  const hasRegistrants = (event.currentCount ?? 0) > 0;

  // 2축 상태 배지 (EventCard와 동일 로직)
  const regStatus = event.registrationStatus;
  // COMPLETED 또는 CANCELED → 종료 상태 (모집 배지 숨김, 수정/신청취소 불가)
  const isEventEnded =
    event.eventStatus === "COMPLETED" || event.eventStatus === "CANCELED";
  const isUpcomingWithRegStatus =
    event.eventStatus === "UPCOMING" &&
    (regStatus === "NOT_STARTED" || regStatus === "OPEN");

  const showEventStatus = !isUpcomingWithRegStatus;
  const eventStatusBadge = event.eventStatus
    ? (EVENT_STATUS_BADGE[event.eventStatus] ??
      "bg-muted text-muted-foreground")
    : "bg-muted text-muted-foreground";
  const eventStatusLabel = event.eventStatus
    ? (EVENT_STATUS_LABEL[event.eventStatus] ?? event.eventStatus)
    : "알 수 없음";

  const showRegStatus =
    isUpcomingWithRegStatus ||
    (!isEventEnded &&
      regStatus &&
      (regStatus === "OPEN" || regStatus === "CLOSED"));
  const regStatusBadge =
    showRegStatus && regStatus ? REG_STATUS_BADGE[regStatus] : null;
  const regStatusLabel =
    showRegStatus && regStatus ? REG_STATUS_LABEL[regStatus] : null;

  const { date: dateStr, time: timeStr } = formatDateTime(event.eventStartAt);

  // 이미지 목록 (추후 다중 이미지 API 지원 시 교체)
  // TODO: API에서 이미지 배열을 받으면 아래 데모 데이터 교체
  const images = [
    "/igruslogo2.png",
    "https://placehold.co/400x400/1a1a2e/e0e0e0?text=Event+Photo+1",
    "https://placehold.co/400x400/16213e/e0e0e0?text=Event+Photo+2",
  ];
  const hasPrev = currentImageIndex > 0;
  const hasNext = currentImageIndex < images.length - 1;

  // 더보기 메뉴 항목 구성
  // COMPLETED 또는 CANCELED는 수정 불가 (백엔드: Event.update)
  const isCanceled = event.eventStatus === "CANCELED";
  const showEdit = !isCanceled;
  const showPublish = event.visibility === "UNPUBLISHED";
  const showUnpublish = event.visibility === "PUBLISHED";
  const showClose = event.registrationStatus === "OPEN";
  const showReopen =
    event.registrationStatus === "CLOSED" &&
    (event.eventStatus === "UPCOMING" || event.eventStatus === "ONGOING");
  const showCancelEvent =
    event.eventStatus === "UPCOMING" || event.eventStatus === "ONGOING";
  const showReactivate = event.eventStatus === "CANCELED";

  return (
    <div className="animate-in slide-in-from-right-8 duration-300 max-w-4xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-s6">
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="flex items-center gap-s2 text-sm font-medium transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
        >
          <ArrowLeft size={18} /> 행사 목록
        </button>
        {canManage && (
          <div className="relative" ref={moreMenuRef}>
            <button
              onClick={() => setIsMoreMenuOpen(!isMoreMenuOpen)}
              type="button"
              className="p-s2 rounded-full transition cursor-pointer hover:bg-muted text-muted-foreground hover:text-foreground"
            >
              <MoreHorizontal size={20} />
            </button>
            {isMoreMenuOpen && (
              <div className="absolute right-0 top-full mt-s2 w-48 rounded-r3 shadow-2xl border overflow-hidden z-20 animate-in fade-in zoom-in-95 duration-200 bg-popover border-border">
                {/* 일반 관리 영역 */}
                {showEdit && (
                  <button
                    onClick={handleEdit}
                    type="button"
                    className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer text-foreground hover:bg-muted"
                  >
                    <Edit size={16} /> 수정하기
                  </button>
                )}

                {showPublish && (
                  <button
                    onClick={handlePublish}
                    type="button"
                    disabled={isMutating}
                    className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Eye size={16} /> 공개
                  </button>
                )}

                {showUnpublish && (
                  <button
                    onClick={handleUnpublish}
                    type="button"
                    disabled={isMutating}
                    className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <EyeOff size={16} /> 비공개
                  </button>
                )}

                {showClose && (
                  <button
                    onClick={handleCloseEvent}
                    type="button"
                    disabled={isMutating}
                    className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Lock size={16} /> 모집 마감
                  </button>
                )}

                {showReopen && (
                  <button
                    onClick={handleReopenRegistration}
                    type="button"
                    disabled={isMutating}
                    className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <LockOpen size={16} /> 모집 재오픈
                  </button>
                )}

                {/* 구분선 */}
                <div className="border-t border-border my-1" />

                {/* 파괴적 액션 영역 */}
                {showCancelEvent && (
                  <button
                    onClick={handleCancelEvent}
                    type="button"
                    disabled={isMutating}
                    className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <XCircle size={16} /> 행사 취소
                  </button>
                )}

                {showReactivate && (
                  <button
                    onClick={handleReactivate}
                    type="button"
                    disabled={isMutating}
                    className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <RotateCcw size={16} /> 행사 재활성화
                  </button>
                )}

                <button
                  onClick={handleDelete}
                  type="button"
                  disabled={isDeleting || hasRegistrants}
                  title={
                    hasRegistrants
                      ? "신청자가 있는 행사는 삭제할 수 없습니다"
                      : undefined
                  }
                  className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Trash2 size={16} /> 삭제하기
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 상단: 이미지 + 정보 */}
      <div className="rounded-r4 overflow-hidden border bg-card border-border shadow-sm">
        <div className="grid grid-cols-1 md:grid-cols-[3fr_2fr]">
          {/* 이미지 캐러셀 */}
          <div className="relative bg-muted/30 flex items-center justify-center aspect-square">
            <img
              src={images[currentImageIndex]}
              alt={event.title}
              className="w-[200px] h-[200px] object-contain"
            />
            {/* 상태 뱃지 (EventCard와 동일 로직) */}
            <div className="absolute top-s4 left-s4 flex gap-s2">
              {event.visibility === "UNPUBLISHED" && (
                <span
                  className={`px-s3 py-s1 rounded-full text-xs font-bold tracking-wide ${VISIBILITY_BADGE["UNPUBLISHED"]}`}
                >
                  {VISIBILITY_LABEL["UNPUBLISHED"]}
                </span>
              )}
              {showEventStatus && (
                <span
                  className={`px-s3 py-s1 rounded-full text-xs font-bold tracking-wide ${eventStatusBadge}`}
                >
                  {eventStatusLabel}
                </span>
              )}
              {regStatusLabel && (
                <span
                  className={`px-s3 py-s1 rounded-full text-xs font-bold tracking-wide ${regStatusBadge}`}
                >
                  {regStatusLabel}
                </span>
              )}
            </div>
            {/* 캐러셀 화살표 */}
            {images.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={() => setCurrentImageIndex((i) => i - 1)}
                  disabled={!hasPrev}
                  className="absolute left-s3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-background/80 backdrop-blur-sm flex items-center justify-center transition hover:bg-background cursor-pointer disabled:opacity-0 disabled:cursor-default"
                >
                  <ChevronLeft size={20} />
                </button>
                <button
                  type="button"
                  onClick={() => setCurrentImageIndex((i) => i + 1)}
                  disabled={!hasNext}
                  className="absolute right-s3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-background/80 backdrop-blur-sm flex items-center justify-center transition hover:bg-background cursor-pointer disabled:opacity-0 disabled:cursor-default"
                >
                  <ChevronRight size={20} />
                </button>
                {/* 인디케이터 */}
                <div className="absolute bottom-s3 left-1/2 -translate-x-1/2 flex gap-s2">
                  {images.map((_, idx) => (
                    <span
                      key={idx}
                      className={`w-1.5 h-1.5 rounded-full transition ${
                        idx === currentImageIndex
                          ? "bg-primary"
                          : "bg-muted-foreground/30"
                      }`}
                    />
                  ))}
                </div>
              </>
            )}
          </div>

          {/* 오른쪽: 정보 + 버튼 */}
          <div className="p-s6 flex flex-col justify-between gap-s6">
            <div className="space-y-s6">
              {/* 제목 */}
              <h1 className="text-2xl font-bold">{event.title}</h1>

              {/* 행사 정보 */}
              <div className="space-y-s4">
                <div className="space-y-s1">
                  <p className="text-xs text-muted-foreground flex items-center gap-s1">
                    <Calendar size={12} /> 날짜
                  </p>
                  <p className="text-sm font-bold">{dateStr}</p>
                </div>
                <div className="space-y-s1">
                  <p className="text-xs text-muted-foreground flex items-center gap-s1">
                    <Clock size={12} /> 시간
                  </p>
                  <p className="text-sm font-bold">{timeStr}</p>
                </div>
                <div className="space-y-s1">
                  <p className="text-xs text-muted-foreground flex items-center gap-s1">
                    <MapPin size={12} /> 장소
                  </p>
                  <p className="text-sm font-bold">{event.location || "TBD"}</p>
                </div>
                <div className="space-y-s1">
                  <p className="text-xs text-muted-foreground flex items-center gap-s1">
                    <Users size={12} /> 정원
                  </p>
                  <p className="text-sm font-bold">
                    {event.currentCount ?? 0} / {event.capacity ?? 0}
                  </p>
                </div>
              </div>
            </div>

            {/* 액션 버튼 */}
            <div className="space-y-s3">
              {canApply && (
                <button
                  type="button"
                  onClick={handleApply}
                  disabled={isApplying}
                  className="w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all bg-primary text-primary-foreground hover:bg-primary/90 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isApplying ? "신청 중..." : "행사 신청"}
                </button>
              )}

              {canCancel && (
                <button
                  type="button"
                  onClick={handleCancelRegistration}
                  disabled={isCanceling}
                  className="w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all bg-destructive text-destructive-foreground hover:bg-destructive/90 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isCanceling ? "취소 중..." : "신청 취소"}
                </button>
              )}

              {!canApply && !canCancel && (
                <button
                  type="button"
                  disabled
                  className="w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all bg-muted text-muted-foreground cursor-not-allowed"
                >
                  {hasApplied ? "신청 완료" : "신청 불가"}
                </button>
              )}

              {canManage && (
                <button
                  type="button"
                  onClick={() => {
                    void queryClient.invalidateQueries({
                      queryKey: registrationKeys.all(Number(eventId)),
                    });
                    navigate(`/events/${eventId}/registrations`);
                  }}
                  className="w-full py-s3 rounded-r4 font-medium flex items-center justify-center gap-s2 transition-all border border-border text-foreground hover:bg-muted cursor-pointer"
                >
                  <Users size={16} /> 신청자 관리
                </button>
              )}
            </div>
          </div>
        </div>

        {/* 하단: 상세 설명 */}
        <div className="border-t border-border p-s6">
          <h3 className="text-sm font-bold text-muted-foreground mb-s4">
            상세 설명
          </h3>
          {event.description ? (
            <MarkdownPreview
              source={event.description.replace(/\n/g, "  \n")}
              className="!leading-relaxed"
            />
          ) : (
            <p className="text-sm text-muted-foreground">
              상세 설명이 없습니다.
            </p>
          )}
        </div>
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
