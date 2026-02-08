import { useParams, useNavigate } from 'react-router-dom';
import { FullPageSpinner } from '@/components/ui';
import { useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Calendar, MapPin, Users, Clock, MoreHorizontal, Edit, Trash2 } from 'lucide-react';
import { useEvent, useApplyEvent, useCancelEventApplication, useDeleteEvent } from '@/hooks/queries/useEvents';
import { useAuth } from '@/hooks';
import { useEffect, useRef, useState } from 'react';
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
} from '@/utils/error';

export default function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: eventResponse, isLoading, error } = useEvent(Number(eventId));
  const { mutate: applyEvent, isPending: isApplying } = useApplyEvent();
  const { mutate: cancelEvent, isPending: isCanceling } = useCancelEventApplication();
  const { mutate: deleteEvent, isPending: isDeleting } = useDeleteEvent();
  const { user } = useAuth();

  // API 응답 데이터 추출
  const event = eventResponse?.data;

  // More Menu 상태
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);
  const moreMenuRef = useRef<HTMLDivElement>(null);

  // 외부 클릭 감지 (More Menu 닫기)
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (moreMenuRef.current && !moreMenuRef.current.contains(event.target as Node)) {
        setIsMoreMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleApply = () => {
    if (!eventId) return;
    applyEvent(
      { eventId: Number(eventId) },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/events/${eventId}`],
          });
        },
        onError: (error: unknown) => {
          if (isEventAlreadyRegistered(error)) {
            alert('이미 신청한 행사입니다.');
          } else if (isEventCapacityFull(error)) {
            alert('정원이 마감되었습니다.');
          } else if (isEventRegistrationClosed(error)) {
            alert('신청 기간이 종료되었습니다.');
          } else if (isForbiddenError(error)) {
            alert('행사 신청 권한이 없습니다.');
          } else {
            alert(getErrorMessage(error));
          }
        },
      }
    );
  };

  const handleCancel = () => {
    console.log('[handleCancel] eventId:', eventId);
    if (!eventId) return;
    if (!confirm('행사 신청을 취소하시겠습니까?')) return;
    console.log('[handleCancel] calling cancelEvent');

    try {
      cancelEvent(
        { eventId: Number(eventId) },
        {
          onSuccess: () => {
            console.log('[cancelEvent] onSuccess');
            void queryClient.invalidateQueries({
              queryKey: [`/api/v1/events/${eventId}`],
            });
          },
          onError: (error: unknown) => {
            console.log('[cancelEvent] onError:', error);
            if (hasErrorCode(error, 'EVENT_ALREADY_CANCELED')) {
              alert('이미 취소된 신청입니다.');
            } else if (hasErrorCode(error, 'CANCEL_DEADLINE_PASSED')) {
              alert('취소 가능 기간이 지났습니다.');
            } else {
              alert(getErrorMessage(error));
            }
          },
          onSettled: () => {
            console.log('[cancelEvent] onSettled');
          },
      }
    );
    console.log('[handleCancel] cancelEvent() returned');
    } catch (e) {
      console.error('[handleCancel] cancelEvent threw:', e);
    }
  };

  const handleEdit = () => {
    navigate(`/events/${eventId}/edit`);
  };

  const handleDelete = () => {
    if (!window.confirm('이 행사를 삭제하시겠습니까?\n삭제된 행사는 복구할 수 없습니다.')) {
      return;
    }

    setIsMoreMenuOpen(false);
    deleteEvent(
      { eventId: Number(eventId) },
      {
        onSuccess: () => {
          navigate('/events');
        },
        onError: (error: unknown) => {
          if (isForbiddenError(error) || isEventOperatorRequired(error)) {
            alert('행사 삭제 권한이 없습니다.');
          } else if (isEventNotFound(error)) {
            alert('이미 삭제된 행사입니다.');
          } else {
            alert(getErrorMessage(error));
          }
        },
      }
    );
  };

  if (isLoading) {
    return <FullPageSpinner />;
  }

  // 403 에러 체크 (권한 없음)
  const isForbidden = isForbiddenError(error) || isEventAccessDenied(error);

  if (isForbidden) {
    return (
      <div className="text-center py-12 space-y-s4">
        <p className="text-muted-foreground">정회원 승인 후 행사 상세 조회가 가능합니다.</p>
        <button
          type="button"
          onClick={() => navigate('/events')}
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
  const canApply = user && isOpen && !hasApplied;
  const canCancel = user && hasApplied;
  const canManage = event.canEdit || event.isAuthor;

  // 날짜 포맷팅 (ISO 8601 형식에서 파싱)
  const formatDateTime = (isoString?: string) => {
    if (!isoString) return { date: 'TBD', time: 'TBD' };

    try {
      const date = new Date(isoString);
      const dateStr = date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
      const timeStr = date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
      });
      return { date: dateStr, time: timeStr };
    } catch {
      return { date: 'TBD', time: 'TBD' };
    }
  };

  const { date: dateStr, time: timeStr } = formatDateTime(event.eventStartAt);

  return (
    <div className="animate-in slide-in-from-right-8 duration-300">
      <button
        type="button"
        onClick={() => navigate('/events')}
        className="mb-s6 flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
      >
        <ArrowLeft size={18} /> Back to Events
      </button>

      <div className="rounded-r4 overflow-hidden border bg-card border-border shadow-sm">
        <div className="h-64 md:h-80 relative bg-gradient-to-br from-primary/20 to-primary/5">
          <div className="absolute bottom-s6 left-s6 right-s6">
            <span
              className={`px-s3 py-s1 rounded-full text-xs font-bold uppercase tracking-widest mb-s4 inline-block ${
                isOpen ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'
              }`}
            >
              {isOpen ? '신청 가능' : '마감'}
            </span>
            <h1 className="text-3xl md:text-4xl font-bold">{event.title}</h1>
          </div>
        </div>

        <div className="p-s6 md:p-s7 grid grid-cols-1 md:grid-cols-3 gap-s7">
          <div className="md:col-span-2 space-y-s8">
            <div>
              <div className="flex items-center justify-between mb-s4">
                <h3 className="text-xl font-bold">About Event</h3>
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
                        <button
                          onClick={handleEdit}
                          type="button"
                          className="w-full text-left px-s4 py-s3 text-sm font-medium flex items-center gap-s2 transition-colors cursor-pointer text-foreground hover:bg-muted"
                        >
                          <Edit size={16} /> 수정하기
                        </button>
                        <button
                          onClick={handleDelete}
                          type="button"
                          disabled={isDeleting}
                          className="w-full text-left px-s4 py-s3 text-sm font-medium text-destructive hover:bg-destructive/10 flex items-center gap-s2 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <Trash2 size={16} /> {isDeleting ? '삭제 중...' : '삭제하기'}
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
              <p className="leading-relaxed text-muted-foreground whitespace-pre-wrap">
                {event.description || '상세 설명이 없습니다.'}
              </p>
            </div>
          </div>

          <div className="space-y-s6">
            <div className="p-s6 rounded-r4 space-y-s4 bg-muted/50">
              <div className="flex items-center gap-s4">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                  <Calendar size={20} />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground uppercase font-bold">Date</p>
                  <p className="font-bold">{dateStr}</p>
                </div>
              </div>
              <div className="flex items-center gap-s4">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                  <Clock size={20} />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground uppercase font-bold">Time</p>
                  <p className="font-bold">{timeStr}</p>
                </div>
              </div>
              <div className="flex items-center gap-s4">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                  <MapPin size={20} />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground uppercase font-bold">Location</p>
                  <p className="font-bold">{event.location || 'TBD'}</p>
                </div>
              </div>
              <div className="flex items-center gap-s4">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                  <Users size={20} />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground uppercase font-bold">Capacity</p>
                  <p className="font-bold">
                    {event.currentCount ?? 0} / {event.capacity ?? 0}
                  </p>
                </div>
              </div>
            </div>

            {canApply && (
              <button
                type="button"
                onClick={handleApply}
                disabled={isApplying}
                className="w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all shadow-lg bg-primary text-primary-foreground hover:bg-primary/90 shadow-primary/20 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isApplying ? '신청 중...' : 'Apply Now'}
              </button>
            )}

            {canCancel && (
              <button
                type="button"
                onClick={handleCancel}
                disabled={isCanceling}
                className="w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all shadow-lg bg-destructive text-destructive-foreground hover:bg-destructive/90 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isCanceling ? '취소 중...' : 'Cancel Application'}
              </button>
            )}

            {!canApply && !canCancel && (
              <button
                type="button"
                disabled
                className="w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all shadow-lg bg-muted text-muted-foreground cursor-not-allowed"
              >
                {hasApplied ? 'Already Applied' : 'Application Closed'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
