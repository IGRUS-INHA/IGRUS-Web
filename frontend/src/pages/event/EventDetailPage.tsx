import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, MapPin, Users, Clock } from 'lucide-react';
import { useEvent, useApplyEvent, useCancelEventApplication } from '@/hooks/queries/useEvents';
import { useAuthStore } from '@/stores/authStore';

export default function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const { data: event, isLoading } = useEvent(eventId ?? '');
  const { mutate: applyEvent, isPending: isApplying } = useApplyEvent();
  const { mutate: cancelEvent, isPending: isCanceling } = useCancelEventApplication();
  const user = useAuthStore((state) => state.user);

  const handleApply = () => {
    if (!eventId) return;
    applyEvent(eventId, {
      onSuccess: () => {
        alert('행사 신청이 완료되었습니다.');
      },
      onError: (error) => {
        alert(`신청 실패: ${error.message}`);
      },
    });
  };

  const handleCancel = () => {
    if (!eventId) return;
    if (!confirm('행사 신청을 취소하시겠습니까?')) return;

    cancelEvent(eventId, {
      onSuccess: () => {
        alert('행사 신청이 취소되었습니다.');
      },
      onError: (error) => {
        alert(`취소 실패: ${error.message}`);
      },
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading...</div>
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

  const isOpen = event.status === 'UPCOMING' || event.status === 'Open';
  const hasApplied = event.myRegistration?.status === 'CONFIRMED';
  const canApply = user && isOpen && !hasApplied;
  const canCancel = user && hasApplied;

  // 날짜와 시간 분리
  const [dateStr, timeStr] = event.date.split(' ');

  return (
    <div className="animate-in slide-in-from-right-8 duration-300">
      <button
        type="button"
        onClick={() => navigate('/events')}
        className="mb-s6 flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
      >
        <ArrowLeft size={18} /> Back to Events
      </button>

      <div className="rounded-[2.5rem] overflow-hidden border bg-card border-border shadow-sm">
        <div className="h-64 md:h-80 relative">
          {event.image && (
            <>
              <img src={event.image} alt={event.title} className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
            </>
          )}
          <div className="absolute bottom-8 left-8 right-8">
            <span
              className={`px-s3 py-1 rounded-full text-xs font-bold uppercase tracking-widest mb-s4 inline-block ${
                isOpen ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'
              }`}
            >
              {isOpen ? '신청 가능' : '마감'}
            </span>
            <h1 className="text-3xl md:text-4xl font-bold text-white">{event.title}</h1>
          </div>
        </div>

        <div className="p-s8 md:p-12 grid grid-cols-1 md:grid-cols-3 gap-12">
          <div className="md:col-span-2 space-y-s8">
            <div>
              <h3 className="text-xl font-bold mb-s4">About Event</h3>
              <p className="leading-relaxed text-muted-foreground whitespace-pre-wrap">
                {event.description || event.content || '상세 설명이 없습니다.'}
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
                  <p className="font-bold">{timeStr || 'TBD'}</p>
                </div>
              </div>
              <div className="flex items-center gap-s4">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                  <MapPin size={20} />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground uppercase font-bold">Location</p>
                  <p className="font-bold">{event.location}</p>
                </div>
              </div>
              <div className="flex items-center gap-s4">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                  <Users size={20} />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground uppercase font-bold">Capacity</p>
                  <p className="font-bold">
                    {event.attendees ?? event.currentCount ?? 0} / {event.maxCapacity ?? event.capacity}
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
