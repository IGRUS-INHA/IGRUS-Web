import { useNavigate, useParams } from 'react-router-dom';
import { FullPageSpinner } from '@/components/ui';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Calendar, MapPin, Users, Image as ImageIcon, Clock, Save } from 'lucide-react';
import { useEvent, useUpdateEvent } from '@/hooks/queries/useEvents';
import { cn } from '@/lib/utils';
import { useEffect } from 'react';
import { getErrorMessage, isForbiddenError, isEventAccessDenied, isEventOperatorRequired } from '@/utils/error';

const eventSchema = z.object({
  title: z.string().min(1, '행사 제목을 입력하세요'),
  description: z.string().min(1, '행사 설명을 입력하세요'),
  date: z.string().min(1, '행사 날짜를 선택하세요'),
  time: z.string().min(1, '행사 시간을 선택하세요'),
  location: z.string().min(1, '장소를 입력하세요'),
  capacity: z.number().min(1, '최대 인원은 1명 이상이어야 합니다'),
  registrationDeadlineDate: z.string().min(1, '신청 마감일을 선택하세요'),
  registrationDeadlineTime: z.string().min(1, '신청 마감 시간을 선택하세요'),
});

type EventForm = z.infer<typeof eventSchema>;

export default function EventEditPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const { data: eventResponse, isLoading, error } = useEvent(Number(eventId));
  const { mutate: updateEvent, isPending } = useUpdateEvent();
  const event = eventResponse?.data;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EventForm>({
    resolver: zodResolver(eventSchema),
    defaultValues: {
      capacity: 30,
    },
  });

  // 권한 체크 (권한 없으면 상세 페이지로 리다이렉트)
  useEffect(() => {
    if (event && !event.canEdit && !event.isAuthor) {
      navigate(`/events/${eventId}`);
    }
  }, [event, eventId, navigate]);

  // 기존 데이터로 폼 초기화
  useEffect(() => {
    if (event) {
      // ISO 8601 → date, time 안전하게 분리
      const parseDateTime = (isoString?: string) => {
        if (!isoString) return { date: '', time: '' };
        const d = new Date(isoString);
        if (isNaN(d.getTime())) return { date: '', time: '' };
        return {
          date: d.toISOString().split('T')[0], // YYYY-MM-DD
          time: d.toTimeString().slice(0, 5),   // HH:mm
        };
      };

      const eventDateTime = parseDateTime(event.eventStartAt);
      const regEndDateTime = parseDateTime(event.registrationEndAt);

      reset({
        title: event.title || '',
        description: event.description || '',
        date: eventDateTime.date,
        time: eventDateTime.time,
        location: event.location || '',
        capacity: event.capacity || 30,
        registrationDeadlineDate: regEndDateTime.date,
        registrationDeadlineTime: regEndDateTime.time,
      });
    }
  }, [event, reset]);

  const onSubmit = (data: EventForm) => {
    if (!eventId) return;

    // ISO 8601 형식으로 변환 (타임존 포함)
    const eventStartAt = new Date(`${data.date}T${data.time}:00`).toISOString();
    const registrationEndAt = new Date(`${data.registrationDeadlineDate}T${data.registrationDeadlineTime}:00`).toISOString();

    updateEvent(
      {
        eventId: Number(eventId),
        data: {
          title: data.title,
          description: data.description,
          location: data.location,
          eventStartAt,
          eventEndAt: eventStartAt, // 종료 시간은 시작 시간과 동일하게 설정
          registrationStartAt: event?.registrationStartAt || '', // 기존 값 유지
          registrationEndAt,
          capacity: data.capacity,
        },
      },
      {
        onSuccess: () => {
          alert('행사가 수정되었습니다.');
          navigate(`/events/${eventId}`);
        },
        onError: (error: unknown) => {
          if (isForbiddenError(error) || isEventOperatorRequired(error)) {
            alert('행사 수정 권한이 없습니다.');
          } else {
            alert(getErrorMessage(error));
          }
        },
      }
    );
  };

  // 로딩 상태
  if (isLoading) {
    return <FullPageSpinner />;
  }

  // 403 에러 체크 (조회 권한 없음)
  const isForbidden = isForbiddenError(error) || isEventAccessDenied(error);

  if (isForbidden) {
    return (
      <div className="text-center py-12 space-y-s4">
        <p className="text-muted-foreground">정회원 승인 후 행사 조회가 가능합니다.</p>
        <button
          type="button"
          onClick={() => navigate('/events')}
          className="text-sm text-primary hover:underline cursor-pointer"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  // 에러 상태
  if (error || !event) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">행사를 찾을 수 없습니다.</p>
        <button
          type="button"
          onClick={() => navigate('/events')}
          className="mt-s4 text-primary hover:underline cursor-pointer"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="flex justify-between items-center mb-s8">
          <button
            type="button"
            onClick={() => navigate(`/events/${eventId}`)}
            className="flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
          >
            <ArrowLeft size={18} /> Cancel
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="bg-primary text-primary-foreground px-s6 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save size={16} /> {isPending ? 'Updating...' : 'Update Event'}
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-s8">
          {/* Left Column - Main Info */}
          <div className="md:col-span-2 space-y-s8">
            <div className="p-s8 rounded-r4 border bg-card border-border shadow-sm">
              <h3 className="text-xl font-bold mb-s6">Event Details</h3>

              <div className="space-y-s6">
                <div>
                  <label className="block text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                    Event Title
                  </label>
                  <input
                    type="text"
                    {...register('title')}
                    className={cn(
                      'w-full text-2xl font-bold bg-transparent border-b pb-s2 focus:outline-none focus:border-primary border-border',
                      errors.title && 'border-destructive'
                    )}
                    placeholder="Ex: Spring Networking Night"
                  />
                  {errors.title && (
                    <p className="text-destructive text-sm mt-s1">{errors.title.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                    Description
                  </label>
                  <textarea
                    rows={8}
                    {...register('description')}
                    className={cn(
                      'w-full rounded-r4 p-s4 border focus:outline-none focus:border-primary resize-none bg-muted/50 border-border',
                      errors.description && 'border-destructive'
                    )}
                    placeholder="Describe the event agenda, speakers, and requirements..."
                  />
                  {errors.description && (
                    <p className="text-destructive text-sm mt-s1">{errors.description.message}</p>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Right Column - Logistics */}
          <div className="space-y-s6">
            <div className="p-s8 rounded-r4 border bg-card border-border shadow-sm">
              <h3 className="text-lg font-bold mb-s6">Logistics</h3>

              <div className="space-y-s4">
                <div className="relative group cursor-pointer h-40 rounded-r4 bg-muted/50 overflow-hidden flex items-center justify-center border border-dashed border-border hover:border-primary transition-colors">
                  <div className="text-center text-muted-foreground">
                    <ImageIcon className="mx-auto mb-s2" />
                    <span className="text-xs font-bold">Upload Cover</span>
                    <p className="text-c2 mt-s1">(Coming soon)</p>
                  </div>
                </div>

                <div>
                  <label className="flex items-center gap-s2 text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                    <Calendar size={14} /> Date
                  </label>
                  <input
                    type="date"
                    {...register('date')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border focus:outline-none focus:border-primary bg-muted/50 border-border',
                      errors.date && 'border-destructive'
                    )}
                  />
                  {errors.date && (
                    <p className="text-destructive text-sm mt-s1">{errors.date.message}</p>
                  )}
                </div>

                <div>
                  <label className="flex items-center gap-s2 text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                    <Clock size={14} /> Time
                  </label>
                  <input
                    type="time"
                    {...register('time')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border focus:outline-none focus:border-primary bg-muted/50 border-border',
                      errors.time && 'border-destructive'
                    )}
                  />
                  {errors.time && (
                    <p className="text-destructive text-sm mt-s1">{errors.time.message}</p>
                  )}
                </div>

                <div>
                  <label className="flex items-center gap-s2 text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                    <MapPin size={14} /> Location
                  </label>
                  <input
                    type="text"
                    {...register('location')}
                    placeholder="Room 202 or Zoom Link"
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border focus:outline-none focus:border-primary bg-muted/50 border-border',
                      errors.location && 'border-destructive'
                    )}
                  />
                  {errors.location && (
                    <p className="text-destructive text-sm mt-s1">{errors.location.message}</p>
                  )}
                </div>

                <div>
                  <label className="flex items-center gap-s2 text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                    <Users size={14} /> Max Capacity
                  </label>
                  <input
                    type="number"
                    {...register('capacity', { valueAsNumber: true })}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border focus:outline-none focus:border-primary bg-muted/50 border-border',
                      errors.capacity && 'border-destructive'
                    )}
                  />
                  {errors.capacity && (
                    <p className="text-destructive text-sm mt-s1">{errors.capacity.message}</p>
                  )}
                </div>

                <div className="pt-s4 border-t border-border">
                  <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s4">
                    Registration Deadline
                  </h4>

                  <div className="space-y-s4">
                    <div>
                      <label className="flex items-center gap-s2 text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                        <Calendar size={14} /> Date
                      </label>
                      <input
                        type="date"
                        {...register('registrationDeadlineDate')}
                        className={cn(
                          'w-full rounded-r3 px-s4 py-s3 border focus:outline-none focus:border-primary bg-muted/50 border-border',
                          errors.registrationDeadlineDate && 'border-destructive'
                        )}
                      />
                      {errors.registrationDeadlineDate && (
                        <p className="text-destructive text-sm mt-s1">{errors.registrationDeadlineDate.message}</p>
                      )}
                    </div>

                    <div>
                      <label className="flex items-center gap-s2 text-xs font-bold text-muted-foreground uppercase tracking-widest mb-s2">
                        <Clock size={14} /> Time
                      </label>
                      <input
                        type="time"
                        {...register('registrationDeadlineTime')}
                        className={cn(
                          'w-full rounded-r3 px-s4 py-s3 border focus:outline-none focus:border-primary bg-muted/50 border-border',
                          errors.registrationDeadlineTime && 'border-destructive'
                        )}
                      />
                      {errors.registrationDeadlineTime && (
                        <p className="text-destructive text-sm mt-s1">{errors.registrationDeadlineTime.message}</p>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
