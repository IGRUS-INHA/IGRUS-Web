import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { useNavigate, useLocation } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  ChevronDown,
  ChevronUp,
  MapPin,
  Calendar,
  MoreHorizontal,
  Edit,
  Eye,
  EyeOff,
  Lock,
  LockOpen,
  XCircle,
  RotateCcw,
  Trash2,
  Users,
  ClipboardList,
  FileText,
} from "lucide-react";
import { RichTextViewer } from "@/components/feature/editor";
import type { Event } from "@/types/entities";
import { useAuth } from "@/hooks";
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
import { useRegisterEventExternal } from "@/api/model/event-external-registration/event-external-registration";
import {
  usePublishEvent,
  useUnpublishEvent,
} from "@/api/model/admin-event/admin-event";
import { registrationKeys } from "@/hooks/queries/useEventRegistrations";
import {
  EVENT_STATUS_BADGE,
  EVENT_STATUS_LABEL,
  REG_STATUS_BADGE,
  REG_STATUS_LABEL,
  VISIBILITY_BADGE,
  VISIBILITY_LABEL,
} from "@/constants/eventStatus";
import {
  getErrorMessage,
  isForbiddenError,
  isConflictError,
  isNotFoundError,
  isEventAlreadyRegistered,
  isEventCapacityFull,
  isEventRegistrationClosed,
  isEventOperatorRequired,
  hasErrorCode,
} from "@/utils/error";
import { formatPhoneNumber } from "@/utils";
import { majorOptions } from "@/constants/majorOptions";
import ReasonDialog from "@/components/feature/event/ReasonDialog";
import { cn } from "@/lib/utils";
import { useResolvedImageUrls } from "@/hooks/useResolvedImageUrls";
import { useMemo } from "react";
import { ImageLightbox } from "@/components/ui";

function formatShortDate(isoString?: string): string {
  if (!isoString) return "";
  try {
    return new Date(isoString).toLocaleDateString("ko-KR", {
      month: "long",
      day: "numeric",
    });
  } catch {
    return isoString;
  }
}

interface EventAccordionItemProps {
  event: Event;
}

export default function EventAccordionItem({ event }: EventAccordionItemProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { user, isAuthenticated } = useAuth();
  const isOperator = user?.role === "OPERATOR" || user?.role === "ADMIN";

  const hashId = `event-${event.id}`;
  const isHashTarget = location.hash === `#${hashId}`;

  const [isExpanded, setIsExpanded] = useState(() => isHashTarget);
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [externalForm, setExternalForm] = useState({
    name: "",
    studentId: "",
    phone: "",
    department: "",
    customDepartment: "",
  });
  const [externalFormErrors, setExternalFormErrors] = useState<
    Record<string, string>
  >({});
  const itemRef = useRef<HTMLDivElement>(null);
  const moreMenuRef = useRef<HTMLDivElement>(null);
  const [reasonDialog, setReasonDialog] = useState<{
    title: string;
    onConfirm: (reason: string) => void;
  } | null>(null);

  const numericId = Number(event.id);

  // Lazy fetch detail when expanded
  // allowExternal=true 행사는 비인증 사용자도 상세 조회 가능
  const publicQuery = useEvent(
    numericId,
    isExpanded && !isOperator && (isAuthenticated || !!event.allowExternal),
  );
  const adminQuery = useAdminEvent(numericId, isExpanded && isOperator);
  const { data: detailResponse, isLoading: isDetailLoading } = isOperator
    ? adminQuery
    : publicQuery;
  const detail = detailResponse?.data;

  // External registration mutation
  const { mutate: registerExternal, isPending: isRegisteringExternal } =
    useRegisterEventExternal({
      mutation: {
        onSuccess: () => {
          alert("행사 신청이 완료되었습니다.");
          setExternalForm({
            name: "",
            studentId: "",
            phone: "",
            department: "",
            customDepartment: "",
          });
          setExternalFormErrors({});
          void queryClient.invalidateQueries({
            queryKey: eventKeys.detail(numericId),
          });
          void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
          void queryClient.invalidateQueries({
            queryKey: adminEventKeys.detail(numericId),
          });
          void queryClient.invalidateQueries({
            queryKey: adminEventKeys.lists(),
          });
        },
        onError: (error: unknown) => {
          if (isConflictError(error)) {
            alert("이미 신청한 행사입니다. (동일 학번 또는 연락처)");
          } else if (isNotFoundError(error)) {
            alert("행사를 찾을 수 없습니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    });

  const handleExternalApply = (e: React.MouseEvent) => {
    e.stopPropagation();
    const errors: Record<string, string> = {};
    if (!externalForm.name.trim()) {
      errors.name = "이름을 입력해주세요.";
    } else if (externalForm.name.trim().length > 50) {
      errors.name = "이름은 50자 이내여야 합니다.";
    }
    if (!externalForm.studentId.trim()) {
      errors.studentId = "학번을 입력해주세요.";
    } else if (!/^\d{8}$/.test(externalForm.studentId)) {
      errors.studentId = "학번은 8자리 숫자여야 합니다.";
    }
    if (!externalForm.phone.trim()) {
      errors.phone = "연락처를 입력해주세요.";
    } else if (!/^\d{3}-\d{4}-\d{4}$/.test(externalForm.phone)) {
      errors.phone = "올바른 전화번호를 입력해주세요. (예: 010-1234-5678)";
    }
    const resolvedDepartment =
      externalForm.department === "기타"
        ? externalForm.customDepartment.trim()
        : externalForm.department;
    if (!externalForm.department) {
      errors.department = "학과를 선택해주세요.";
    } else if (
      externalForm.department === "기타" &&
      !externalForm.customDepartment.trim()
    ) {
      errors.department = "학과를 직접 입력해주세요.";
    }
    if (Object.keys(errors).length > 0) {
      setExternalFormErrors(errors);
      return;
    }
    if (event.surveyId) {
      const params = new URLSearchParams({
        surveyId: String(event.surveyId),
        name: externalForm.name,
        studentId: externalForm.studentId,
        phone: externalForm.phone,
        department: resolvedDepartment,
      });
      navigate(`/events/${numericId}/apply/external?${params.toString()}`);
      return;
    }
    registerExternal({
      eventId: numericId,
      data: {
        name: externalForm.name,
        studentId: externalForm.studentId,
        phone: externalForm.phone,
        department: resolvedDepartment,
      },
    });
  };

  // Mutations
  const { mutate: applyEvent, isPending: isApplying } = useApplyEvent();
  const { mutate: cancelRegistration, isPending: isCanceling } =
    useCancelEventApplication();
  const { mutate: deleteEvent, isPending: isDeleting } = useDeleteEvent();
  const { mutate: closeEvent, isPending: isClosing } = useCloseEvent();

  const invalidateEvent = () => {
    void queryClient.invalidateQueries({
      queryKey: eventKeys.detail(numericId),
    });
    void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
    void queryClient.invalidateQueries({
      queryKey: adminEventKeys.detail(numericId),
    });
    void queryClient.invalidateQueries({ queryKey: adminEventKeys.lists() });
  };

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

  // 해시 타겟이면 마운트 시 스크롤
  useEffect(() => {
    if (isHashTarget && itemRef.current) {
      setTimeout(() => {
        itemRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 50);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleToggle = () => {
    const next = !isExpanded;
    setIsExpanded(next);
    if (next) {
      navigate(`${location.pathname}${location.search}#${hashId}`, {
        replace: true,
      });
    } else if (location.hash === `#${hashId}`) {
      navigate(`${location.pathname}${location.search}`, { replace: true });
    }
  };

  // Outside click to close more menu
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
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Badge logic (same as EventCard/EventDetailPage)
  const regStatus = event.registrationStatus;
  const isEventEnded =
    event.status === "COMPLETED" || event.status === "CANCELED";
  const isUpcomingWithRegStatus =
    event.status === "UPCOMING" &&
    (regStatus === "NOT_STARTED" || regStatus === "OPEN");
  const showEventStatus = !isUpcomingWithRegStatus;
  const eventStatusBadge =
    EVENT_STATUS_BADGE[event.status] ?? "bg-muted text-muted-foreground";
  const eventStatusLabel = EVENT_STATUS_LABEL[event.status] ?? event.status;
  const showRegStatus =
    isUpcomingWithRegStatus ||
    (!isEventEnded &&
      regStatus &&
      (regStatus === "OPEN" || regStatus === "CLOSED"));
  const regStatusBadge =
    showRegStatus && regStatus ? REG_STATUS_BADGE[regStatus] : null;
  const regStatusLabel =
    showRegStatus && regStatus ? REG_STATUS_LABEL[regStatus] : null;

  // Action state
  const isRegistrable = detail?.isRegistrable ?? event.isRegistrable ?? false;
  const hasApplied = detail?.isRegistered ?? false;
  const isUnpublished =
    (detail?.visibility ?? event.visibility) === "UNPUBLISHED";
  const canApply = user && isRegistrable && !hasApplied && !isUnpublished;
  const canCancel =
    user && hasApplied && (detail?.eventStatus ?? event.status) !== "CANCELED";
  const hasRegistrants = (detail?.currentCount ?? event.currentCount ?? 0) > 0;

  // OPERATOR menu items
  const currentEventStatus = detail?.eventStatus ?? event.status;
  const currentRegStatus =
    detail?.registrationStatus ?? event.registrationStatus;
  const currentVisibility = detail?.visibility ?? event.visibility;
  const isCanceled = currentEventStatus === "CANCELED";
  const showEdit = !isCanceled;
  const showPublish = currentVisibility === "UNPUBLISHED";
  const showUnpublish = currentVisibility === "PUBLISHED";
  const showClose = currentRegStatus === "OPEN";
  const showReopen =
    currentRegStatus === "CLOSED" &&
    (currentEventStatus === "UPCOMING" || currentEventStatus === "ONGOING");
  const showCancelEvent =
    currentEventStatus === "UPCOMING" || currentEventStatus === "ONGOING";
  const showReactivate = currentEventStatus === "CANCELED";

  // Handlers
  const handleApply = () => {
    if (detail?.surveyId) {
      navigate(`/events/${numericId}/apply`);
      return;
    }
    applyEvent(
      { eventId: numericId, data: {} },
      {
        onError: (error: unknown) => {
          if (isEventAlreadyRegistered(error)) {
            alert("이미 신청한 행사입니다.");
          } else if (isEventCapacityFull(error)) {
            alert("정원이 마감되었습니다.");
          } else if (isEventRegistrationClosed(error)) {
            alert("신청 기간이 종료되었습니다.");
          } else if (hasErrorCode(error, "EVENT_SURVEY_RESPONSE_REQUIRED")) {
            navigate(`/events/${numericId}/apply`);
          } else if (hasErrorCode(error, "EVENT_SURVEY_NOT_READY")) {
            alert("설문이 아직 시작되지 않았습니다.");
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
    const isReregistrationBlocked =
      detail?.closeReason === "DEADLINE_PASSED" ||
      detail?.closeReason === "MANUAL_CLOSE";
    const message = isReregistrationBlocked
      ? "신청을 취소하면 재신청이 불가능합니다. 정말 취소하시겠습니까?"
      : "행사 신청을 취소하시겠습니까?";
    if (!confirm(message)) return;
    cancelRegistration(
      { eventId: numericId },
      {
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

  const handleDelete = () => {
    if (
      !window.confirm(
        "이 행사를 삭제하시겠습니까?\n삭제된 행사는 복구할 수 없습니다.",
      )
    )
      return;
    setIsMoreMenuOpen(false);
    deleteEvent(
      { eventId: numericId },
      {
        onError: (error: unknown) => {
          if (hasErrorCode(error, "EVENT_NOT_DELETABLE")) {
            alert("신청자가 있는 행사는 삭제할 수 없습니다.");
          } else if (
            isForbiddenError(error) ||
            isEventOperatorRequired(error)
          ) {
            alert("행사 삭제 권한이 없습니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    );
  };

  // Top gradient bar: deterministic color from bubble palette based on event id
  const BUBBLE_ACCENT_COLORS = [
    "var(--cat-algo)",
    "var(--cat-web)",
    "var(--cat-ai)",
    "var(--cat-hackathon)",
    "var(--cat-study)",
    "var(--cat-game)",
    "var(--cat-security)",
  ] as const;
  const accentColor = (() => {
    const sum = [...event.id].reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
    return BUBBLE_ACCENT_COLORS[sum % BUBBLE_ACCENT_COLORS.length];
  })();

  // Date range label
  const startLabel = formatShortDate(event.startDate ?? event.date);
  const endLabel = formatShortDate(event.endDate);
  const dateRangeLabel =
    endLabel && endLabel !== startLabel
      ? `${startLabel} ~ ${endLabel}`
      : startLabel;

  // Progress
  const currentCount = detail?.currentCount ?? event.currentCount ?? 0;
  const capacity = detail?.capacity ?? event.capacity ?? 0;
  const progressPercent =
    capacity > 0 ? Math.min((currentCount / capacity) * 100, 100) : 0;

  // 이미지 — 확장 시 detail.attachments에서 objectKey 추출
  const imageObjectKeys = useMemo(
    () =>
      (detail?.attachments ?? [])
        .map((a) => a.objectKey ?? "")
        .filter((key) => Boolean(key) && key !== event.thumbnailObjectKey),
    [detail?.attachments, event.thumbnailObjectKey],
  );
  const { urls: resolvedImageUrls } = useResolvedImageUrls(imageObjectKeys);

  // 썸네일 — collapsed 상태에서 제목 좌측에 표시
  // event.thumbnailObjectKey가 없으면 detail.attachments[0] 폴백 사용
  // (resolvedImageUrls는 이미 위에서 계산되므로 추가 API 호출 없음)
  const thumbnailKeys = useMemo(
    () => (event.thumbnailObjectKey ? [event.thumbnailObjectKey] : []),
    [event.thumbnailObjectKey],
  );
  const { urls: thumbnailUrls } = useResolvedImageUrls(thumbnailKeys);
  const thumbnailSrc = event.thumbnailObjectKey
    ? thumbnailUrls.get(event.thumbnailObjectKey)
    : imageObjectKeys[0]
      ? resolvedImageUrls.get(imageObjectKeys[0])
      : undefined;

  // 라이트박스용 전체 이미지 배열: [썸네일, ...갤러리]
  // thumbnailObjectKey가 없으면 thumbnailSrc == 첫 번째 갤러리 이미지이므로 중복 없음
  const allLightboxSrcs = useMemo(() => {
    const srcs: string[] = [];
    if (thumbnailSrc && event.thumbnailObjectKey) {
      srcs.push(thumbnailSrc);
    }
    for (const key of imageObjectKeys) {
      const url = resolvedImageUrls.get(key);
      if (url) srcs.push(url);
    }
    return srcs;
  }, [
    thumbnailSrc,
    event.thumbnailObjectKey,
    imageObjectKeys,
    resolvedImageUrls,
  ]);

  // 갤러리 이미지 클릭 시 라이트박스 인덱스 오프셋 (전용 썸네일이 배열 앞에 오는 경우)
  const galleryLightboxOffset = event.thumbnailObjectKey ? 1 : 0;

  return (
    <div
      ref={itemRef}
      className={cn(
        "border border-border rounded-r4 bg-card overflow-hidden scroll-mt-20",
        event.status === "CANCELED" && "opacity-60 grayscale",
      )}
    >
      {/* Status gradient top bar */}
      <div
        className="h-1"
        style={{
          backgroundImage: `linear-gradient(to right, ${accentColor}, transparent)`,
        }}
      />
      {/* Header row — entire div is clickable to toggle accordion */}
      <div
        role="button"
        tabIndex={0}
        onClick={handleToggle}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") handleToggle();
        }}
        className={cn(
          "relative w-full pl-s4 pr-s5 flex items-center gap-s4 text-left hover:bg-muted/30 transition cursor-pointer",
          thumbnailSrc ? "py-s3" : "py-s4",
        )}
      >
        {/* Thumbnail */}
        {thumbnailSrc && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setLightboxIndex(0);
            }}
            className="w-[84px] h-[84px] overflow-hidden bg-muted/30 shrink-0 cursor-zoom-in"
          >
            <img
              src={thumbnailSrc}
              alt={event.title}
              className="w-full h-full object-cover"
            />
          </button>
        )}

        {/* Info area */}
        <div className="flex-1 min-w-0">
          {/* Title + badges on same row */}
          <div className="flex items-start gap-s2">
            <p
              className={cn(
                "text-xl font-bold leading-tight flex-1",
                event.status === "CANCELED" && "line-through",
              )}
            >
              {event.title}
            </p>
            <div className="flex flex-wrap items-center gap-s1 shrink-0 mt-0.5">
              {/* Visibility badge */}
              {event.visibility === "UNPUBLISHED" && (
                <span
                  className={`px-s2 py-0.5 rounded-full text-xs font-bold ${VISIBILITY_BADGE["UNPUBLISHED"]}`}
                >
                  {VISIBILITY_LABEL["UNPUBLISHED"]}
                </span>
              )}
              {/* Event status badge */}
              {showEventStatus && (
                <span
                  className={`px-s2 py-0.5 rounded-full text-xs font-bold flex items-center gap-1 ${eventStatusBadge}`}
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-current" />
                  {eventStatusLabel}
                </span>
              )}
              {/* Registration status badge */}
              {regStatusLabel && (
                <span
                  className={`px-s2 py-0.5 rounded-full text-xs font-bold flex items-center gap-1 ${regStatusBadge}`}
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-current" />
                  {regStatusLabel}
                </span>
              )}
              {/* 비회원 신청 가능 badge */}
              {event.allowExternal && (
                <span className="px-s2 py-0.5 rounded-full text-xs font-bold bg-secondary text-secondary-foreground">
                  비회원 신청 가능
                </span>
              )}
            </div>
          </div>

          {/* Date + location */}
          <div className="flex flex-col gap-s1 mt-s2">
            <span className="text-sm text-muted-foreground flex items-center gap-s1">
              <Calendar size={13} className="text-primary shrink-0" />
              <span className="whitespace-nowrap">{dateRangeLabel}</span>
            </span>
            {event.location && (
              <span className="text-sm text-muted-foreground flex items-center gap-s1 min-w-0">
                <MapPin size={13} className="text-primary shrink-0" />
                <span className="truncate">{event.location}</span>
              </span>
            )}
          </div>
        </div>

        {/* Bottom-right: chevron */}
        <div className="absolute bottom-s3 right-s4">
          <span className="flex items-center justify-center w-8 h-8 rounded-lg bg-muted text-muted-foreground">
            {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </span>
        </div>
      </div>

      {/* Expanded body */}
      {isExpanded && (
        <div className="relative border-t border-border px-s6 pb-s6 pt-s4 space-y-s4">
          {/* OPERATOR more menu — 토글 바로 아래 */}
          {isOperator && (
            <div className="absolute top-s2 right-s4" ref={moreMenuRef}>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setIsMoreMenuOpen(!isMoreMenuOpen);
                }}
                className="p-1.5 rounded-full hover:bg-muted transition cursor-pointer text-muted-foreground hover:text-foreground"
              >
                <MoreHorizontal size={18} />
              </button>
              {isMoreMenuOpen && (
                <div className="absolute right-0 top-full mt-s2 w-48 rounded-r3 shadow-2xl border overflow-hidden z-20 animate-in fade-in zoom-in-95 duration-200 bg-popover border-border">
                  {showEdit && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        navigate(`/events/${event.id}/edit`);
                      }}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 cursor-pointer text-foreground hover:bg-muted"
                    >
                      <Edit size={16} /> 수정하기
                    </button>
                  )}
                  {showPublish && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        publishEvent({ eventId: numericId });
                      }}
                      disabled={isMutating}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Eye size={16} /> 공개
                    </button>
                  )}
                  {showUnpublish && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        unpublishEvent({ eventId: numericId });
                      }}
                      disabled={isMutating}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <EyeOff size={16} /> 비공개
                    </button>
                  )}
                  {showClose && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        setReasonDialog({
                          title: "모집 마감 사유",
                          onConfirm: (reason) => {
                            closeEvent(
                              { eventId: numericId, data: { reason } },
                              {
                                onError: (err: unknown) => {
                                  if (
                                    isForbiddenError(err) ||
                                    isEventOperatorRequired(err)
                                  ) {
                                    alert("행사 마감 권한이 없습니다.");
                                  } else {
                                    alert(getErrorMessage(err));
                                  }
                                },
                              },
                            );
                            setReasonDialog(null);
                          },
                        });
                      }}
                      disabled={isMutating}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Lock size={16} /> 모집 마감
                    </button>
                  )}
                  {showReopen && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        setReasonDialog({
                          title: "모집 재오픈 사유",
                          onConfirm: (reason) => {
                            reopenRegistration({
                              eventId: numericId,
                              data: { reason },
                            });
                            setReasonDialog(null);
                          },
                        });
                      }}
                      disabled={isMutating}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 cursor-pointer text-foreground hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <LockOpen size={16} /> 모집 재오픈
                    </button>
                  )}
                  <div className="border-t border-border my-1" />
                  {showCancelEvent && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        setReasonDialog({
                          title: "행사 취소 사유",
                          onConfirm: (reason) => {
                            cancelEvent({
                              eventId: numericId,
                              data: { reason },
                            });
                            setReasonDialog(null);
                          },
                        });
                      }}
                      disabled={isMutating}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <XCircle size={16} /> 행사 취소
                    </button>
                  )}
                  {showReactivate && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsMoreMenuOpen(false);
                        setReasonDialog({
                          title: "행사 재활성화 사유",
                          onConfirm: (reason) => {
                            reactivateEvent({
                              eventId: numericId,
                              data: { reason },
                            });
                            setReasonDialog(null);
                          },
                        });
                      }}
                      disabled={isMutating}
                      className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <RotateCcw size={16} /> 행사 재활성화
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete();
                    }}
                    disabled={isDeleting || hasRegistrants}
                    title={
                      hasRegistrants
                        ? "신청자가 있는 행사는 삭제할 수 없습니다"
                        : undefined
                    }
                    className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Trash2 size={16} /> 삭제하기
                  </button>
                </div>
              )}
            </div>
          )}
          {/* Image gallery (presigned URLs) */}
          {imageObjectKeys.length > 0 && (
            <div className="flex gap-s3">
              {imageObjectKeys.map((key, idx) => {
                const src = resolvedImageUrls.get(key);
                if (!src) return null;
                return (
                  <button
                    key={key}
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setLightboxIndex(galleryLightboxOffset + idx);
                    }}
                    className="w-[84px] h-[84px] overflow-hidden bg-muted/30 shrink-0 cursor-pointer"
                  >
                    <img
                      src={src}
                      alt={`${event.title} ${idx + 1}`}
                      className="w-full h-full object-cover"
                    />
                  </button>
                );
              })}
            </div>
          )}

          {/* Description */}
          <div className="flex items-center gap-1.5">
            <FileText className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-semibold text-foreground">
              상세 정보
            </span>
          </div>
          {isDetailLoading ? (
            <p className="text-sm text-muted-foreground">불러오는 중...</p>
          ) : !isAuthenticated && !event.allowExternal ? (
            <p className="text-sm text-muted-foreground">
              로그인하면 상세 정보를 확인할 수 있습니다.
            </p>
          ) : detail?.description ? (
            <RichTextViewer
              content={detail.description}
              className="text-sm leading-relaxed"
            />
          ) : (
            <p className="text-sm text-muted-foreground">
              상세 설명이 없습니다.
            </p>
          )}

          {/* Progress bar */}
          {capacity > 0 && (
            <div>
              <div className="flex items-center justify-between mb-s4">
                <div className="flex items-center gap-1.5">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm font-semibold text-foreground">
                    모집 현황
                  </span>
                </div>
                <span className="text-sm font-medium text-muted-foreground">
                  {currentCount} / {capacity}
                </span>
              </div>
              <div className="w-full h-2 rounded-full bg-muted overflow-hidden">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
            </div>
          )}

          {/* Action buttons */}
          <div className="space-y-s3">
            {/* 비인증 외부인 신청 폼 */}
            {!isAuthenticated &&
              event.allowExternal &&
              event.registrationStatus === "OPEN" && (
                <div
                  className="space-y-s3"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex items-center gap-1.5">
                    <ClipboardList className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm font-semibold text-foreground">
                      비회원 신청 정보
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-s3">
                    {(
                      [
                        { key: "name", placeholder: "이름", type: "text" },
                        {
                          key: "studentId",
                          placeholder: "학번 (8자리)",
                          type: "text",
                        },
                        {
                          key: "phone",
                          placeholder: "010-0000-0000",
                          type: "tel",
                        },
                      ] as const
                    ).map(({ key, placeholder, type }) => (
                      <div key={key}>
                        <input
                          type={type}
                          placeholder={placeholder}
                          value={externalForm[key]}
                          onChange={(e) => {
                            const value =
                              key === "phone"
                                ? formatPhoneNumber(e.target.value)
                                : e.target.value;
                            setExternalForm((prev) => ({
                              ...prev,
                              [key]: value,
                            }));
                            if (externalFormErrors[key]) {
                              setExternalFormErrors((prev) => ({
                                ...prev,
                                [key]: "",
                              }));
                            }
                          }}
                          className={cn(
                            "w-full px-s3 py-s2 text-sm border rounded-r3 bg-background focus:outline-none focus:ring-1 focus:ring-primary",
                            externalFormErrors[key]
                              ? "border-destructive"
                              : "border-border",
                          )}
                        />
                        {externalFormErrors[key] && (
                          <p className="text-xs text-destructive mt-1">
                            {externalFormErrors[key]}
                          </p>
                        )}
                      </div>
                    ))}
                    {/* 학과 선택 */}
                    <div className="relative">
                      <select
                        value={externalForm.department}
                        onChange={(e) => {
                          setExternalForm((prev) => ({
                            ...prev,
                            department: e.target.value,
                            customDepartment: "",
                          }));
                          if (externalFormErrors.department) {
                            setExternalFormErrors((prev) => ({
                              ...prev,
                              department: "",
                            }));
                          }
                        }}
                        className={cn(
                          "w-full appearance-none pl-s3 pr-8 py-s2 text-sm border rounded-r3 bg-background focus:outline-none focus:ring-1 focus:ring-primary cursor-pointer",
                          externalForm.department
                            ? "text-foreground"
                            : "text-muted-foreground",
                          externalFormErrors.department
                            ? "border-destructive"
                            : "border-border",
                        )}
                      >
                        <option value="">학과 선택</option>
                        {majorOptions.map((group) => (
                          <optgroup key={group.title} label={group.title}>
                            {group.items.map((item) => (
                              <option key={item.key} value={item.value}>
                                {item.value}
                              </option>
                            ))}
                          </optgroup>
                        ))}
                        <option value="기타">기타 (직접 입력)</option>
                      </select>
                      <ChevronDown className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    </div>
                  </div>
                  {/* 기타 직접 입력 + 에러 (그리드 외부) */}
                  {externalForm.department === "기타" && (
                    <input
                      type="text"
                      placeholder="학과를 직접 입력해주세요"
                      value={externalForm.customDepartment}
                      onChange={(e) => {
                        setExternalForm((prev) => ({
                          ...prev,
                          customDepartment: e.target.value,
                        }));
                        if (externalFormErrors.department) {
                          setExternalFormErrors((prev) => ({
                            ...prev,
                            department: "",
                          }));
                        }
                      }}
                      className="w-full px-s3 py-s2 text-sm border border-border rounded-r3 bg-background focus:outline-none focus:ring-1 focus:ring-primary"
                    />
                  )}
                  {externalFormErrors.department && (
                    <p className="text-xs text-destructive -mt-s1">
                      {externalFormErrors.department}
                    </p>
                  )}
                  <button
                    type="button"
                    onClick={handleExternalApply}
                    disabled={isRegisteringExternal}
                    className="w-full py-s3 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all bg-primary text-primary-foreground hover:bg-primary/90 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isRegisteringExternal ? "신청 중..." : "신청하기"}
                  </button>
                </div>
              )}
            {/* 인증 사용자 신청/취소 버튼 */}
            {isAuthenticated && canApply && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  handleApply();
                }}
                disabled={isApplying}
                className="w-full py-s3 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all bg-primary text-primary-foreground hover:bg-primary/90 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isApplying ? "신청 중..." : "신청하기"}
              </button>
            )}
            {isAuthenticated && canCancel && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  handleCancelRegistration();
                }}
                disabled={isCanceling}
                className="w-full py-s3 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all bg-destructive text-destructive-foreground hover:bg-destructive/90 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isCanceling ? "취소 중..." : "신청 취소"}
              </button>
            )}
            {isAuthenticated && !canApply && !canCancel && (
              <button
                type="button"
                disabled
                className="w-full py-s3 rounded-r4 font-bold flex items-center justify-center gap-s2 bg-muted text-muted-foreground cursor-not-allowed"
              >
                {hasApplied ? "신청 완료" : "신청 불가"}
              </button>
            )}
            {isOperator && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  void queryClient.invalidateQueries({
                    queryKey: registrationKeys.all(numericId),
                  });
                  navigate(`/events/${event.id}/registrations`);
                }}
                className="w-full py-s2 rounded-r4 font-medium flex items-center justify-center gap-s2 border border-border text-foreground hover:bg-muted cursor-pointer transition-all"
              >
                <Users size={16} /> 신청자 관리
              </button>
            )}
          </div>
        </div>
      )}

      {/* Image lightbox */}
      {lightboxIndex !== null && allLightboxSrcs.length > 0 && (
        <ImageLightbox
          images={allLightboxSrcs}
          initialIndex={lightboxIndex}
          onClose={() => setLightboxIndex(null)}
        />
      )}

      {/* Reason dialog — grayscale 필터 영향을 피하기 위해 portal로 body에 렌더링 */}
      {reasonDialog &&
        createPortal(
          <ReasonDialog
            title={reasonDialog.title}
            onConfirm={reasonDialog.onConfirm}
            onCancel={() => setReasonDialog(null)}
          />,
          document.body,
        )}
    </div>
  );
}
