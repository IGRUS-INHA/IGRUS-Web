import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
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
  isEventAlreadyRegistered,
  isEventCapacityFull,
  isEventRegistrationClosed,
  isEventOperatorRequired,
  hasErrorCode,
} from "@/utils/error";
import ReasonDialog from "@/components/feature/event/ReasonDialog";
import { cn } from "@/lib/utils";

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
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isOperator = user?.role === "OPERATOR" || user?.role === "ADMIN";

  const [isExpanded, setIsExpanded] = useState(false);
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);
  const moreMenuRef = useRef<HTMLDivElement>(null);
  const [reasonDialog, setReasonDialog] = useState<{
    title: string;
    onConfirm: (reason: string) => void;
  } | null>(null);

  const numericId = Number(event.id);

  // Lazy fetch detail when expanded
  const publicQuery = useEvent(numericId, isExpanded && !isOperator);
  const adminQuery = useAdminEvent(numericId, isExpanded && isOperator);
  const { data: detailResponse, isLoading: isDetailLoading } = isOperator
    ? adminQuery
    : publicQuery;
  const detail = detailResponse?.data;

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
  const isRegistrable = detail?.isRegistrable ?? false;
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

  // Images (only when event.image exists)
  const hasImage = !!event.image;
  const expandedImages = hasImage ? [event.image as string] : [];

  return (
    <div
      className={cn(
        "border border-border rounded-r4 bg-card overflow-hidden",
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
        onClick={() => setIsExpanded(!isExpanded)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") setIsExpanded(!isExpanded);
        }}
        className="relative w-full px-s5 py-s4 flex items-center gap-s4 text-left hover:bg-muted/30 transition cursor-pointer"
      >
        {/* Thumbnail */}
        {hasImage && (
          <div className="w-20 h-20 shrink-0 rounded-r3 overflow-hidden bg-muted/30 flex items-center justify-center">
            <img
              src={event.image}
              alt={event.title}
              className="w-full h-full object-cover"
            />
          </div>
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
          {/* Image thumbnails (only if event has image) */}
          {expandedImages.length > 0 && (
            <div className="flex gap-s3">
              {expandedImages.map((src, idx) => (
                <div
                  key={idx}
                  className="w-24 h-24 rounded-r3 overflow-hidden bg-muted/30 shrink-0"
                >
                  <img
                    src={src}
                    alt={`${event.title} ${idx + 1}`}
                    className="w-full h-full object-cover"
                  />
                </div>
              ))}
            </div>
          )}

          {/* Description */}
          {isDetailLoading ? (
            <p className="text-sm text-muted-foreground">불러오는 중...</p>
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
              <div className="flex items-center justify-between mb-s2">
                <span className="text-sm font-medium text-muted-foreground">
                  모집 현황
                </span>
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
            {canApply && (
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
            {canCancel && (
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
            {!canApply && !canCancel && (
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
