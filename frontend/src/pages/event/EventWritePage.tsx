import { useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Calendar, MapPin, Users, Clock, Save, Image as ImageIcon, ListChecks } from 'lucide-react';
import { WysiwygEditor } from '@/components/feature/editor';
import { useCreateEvent } from '@/hooks/queries/useEvents';
import { CreateEventRequestRegistrationType } from '@/api/model/models';
import { cn } from '@/lib/utils';
import { useUIStore } from '@/stores';
import { isForbiddenError, isEventOperatorRequired, getErrorMessage } from '@/utils/error';

const eventSchema = z.object({
  title: z.string().min(1, '행사 제목을 입력하세요'),
  description: z.string().min(1, '행사 설명을 입력하세요'),
  date: z.string().min(1, '행사 날짜를 선택하세요'),
  time: z.string().min(1, '행사 시간을 선택하세요'),
  location: z.string().min(1, '장소를 입력하세요'),
  capacity: z.number().min(1, '최대 인원은 1명 이상이어야 합니다'),
  registrationType: z.enum(['AUTO_APPROVE', 'MANUAL_APPROVE']),
  registrationStartDate: z.string().min(1, '신청 시작일을 선택하세요'),
  registrationStartTime: z.string().min(1, '신청 시작 시간을 선택하세요'),
  registrationDeadlineDate: z.string().min(1, '신청 마감일을 선택하세요'),
  registrationDeadlineTime: z.string().min(1, '신청 마감 시간을 선택하세요'),
});

type EventForm = z.infer<typeof eventSchema>;

export default function EventWritePage() {
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const { mutate: createEvent, isPending } = useCreateEvent();

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<EventForm>({
    resolver: zodResolver(eventSchema),
    defaultValues: {
      capacity: 30,
      registrationType: 'AUTO_APPROVE',
    },
  });

  const registrationType = watch('registrationType');

  const onSubmit = (data: EventForm) => {
    const eventStartAt = new Date(`${data.date}T${data.time}:00`).toISOString();
    const registrationStartAt = new Date(`${data.registrationStartDate}T${data.registrationStartTime}:00`).toISOString();
    const registrationEndAt = new Date(`${data.registrationDeadlineDate}T${data.registrationDeadlineTime}:00`).toISOString();

    createEvent(
      {
        data: {
          title: data.title,
          description: data.description,
          location: data.location,
          eventStartAt,
          eventEndAt: eventStartAt,
          registrationStartAt,
          registrationEndAt,
          capacity: data.capacity,
          registrationType: data.registrationType as CreateEventRequestRegistrationType,
        },
      },
      {
        onSuccess: () => {
          alert('행사가 등록되었습니다.');
          navigate('/events');
        },
        onError: (error: unknown) => {
          if (isForbiddenError(error) || isEventOperatorRequired(error)) {
            alert('행사 등록 권한이 없습니다.');
          } else {
            alert(getErrorMessage(error));
          }
        },
      }
    );
  };

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      <form onSubmit={handleSubmit(onSubmit)}>
        {/* Sticky Top Bar */}
        <div className="flex justify-between items-center mb-s6 sticky top-0 z-10 py-s4 backdrop-blur-md bg-background/80">
          <button
            type="button"
            onClick={() => navigate('/events')}
            className="flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
          >
            <ArrowLeft size={18} /> 취소
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="bg-primary text-primary-foreground px-s6 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save size={16} /> {isPending ? '등록 중...' : '행사 등록'}
          </button>
        </div>

        {/* 2-Column Layout */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-s6">
          {/* Left Column - Title + MDEditor + Bottom Toolbar */}
          <div className={cn(
            'md:col-span-2 rounded-r4 border shadow-sm flex flex-col',
            isDark ? 'bg-card border-border' : 'bg-card border-border'
          )}>
            {/* 행사 제목 */}
            <div className="px-s6 py-s5 border-b border-border">
              <input
                type="text"
                {...register('title')}
                className={cn(
                  'w-full text-2xl font-bold bg-transparent border-none focus:outline-none focus:ring-0 placeholder:text-muted-foreground/50',
                  errors.title && 'border-b-2 border-b-destructive'
                )}
                placeholder="행사 제목을 입력하세요"
              />
              {errors.title && (
                <p className="typo-c1 text-destructive mt-s2">{errors.title.message}</p>
              )}
            </div>

            {/* WYSIWYG Editor */}
            <div className="flex-1">
              <Controller
                name="description"
                control={control}
                render={({ field }) => (
                  <WysiwygEditor
                    value={field.value ?? ''}
                    onChange={field.onChange}
                    hasError={!!errors.description}
                    className="border-0 rounded-none"
                  />
                )}
              />
              {errors.description && (
                <p className="typo-c1 text-destructive px-s6 pb-s2">{errors.description.message}</p>
              )}
            </div>

            {/* Bottom Toolbar */}
            <div className="px-s6 py-s5 border-t border-border flex items-center">
              <button
                type="button"
                className={cn(
                  'p-2 rounded-lg transition cursor-pointer',
                  isDark ? 'text-gray-400 hover:bg-white/10' : 'text-gray-500 hover:bg-gray-100'
                )}
              >
                <ImageIcon size={20} />
              </button>
            </div>
          </div>

          {/* Right Column - Date / Location / Capacity / Registration Period */}
          <div className="rounded-r4 border bg-card border-border shadow-sm">
            {/* 행사 일시 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Calendar size={14} /> 행사 일시
              </label>
              <div className="space-y-s3">
                <div>
                  <input
                    type="date"
                    {...register('date')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                      'focus:outline-none focus:border-primary',
                      errors.date && 'border-destructive'
                    )}
                  />
                  {errors.date && (
                    <p className="typo-c1 text-destructive mt-s1">{errors.date.message}</p>
                  )}
                </div>
                <div>
                  <input
                    type="time"
                    {...register('time')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                      'focus:outline-none focus:border-primary',
                      errors.time && 'border-destructive'
                    )}
                  />
                  {errors.time && (
                    <p className="typo-c1 text-destructive mt-s1">{errors.time.message}</p>
                  )}
                </div>
              </div>
            </div>

            {/* 장소 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <MapPin size={14} /> 장소
              </label>
              <input
                type="text"
                {...register('location')}
                placeholder="강의실, Zoom 링크 등"
                className={cn(
                  'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                  'focus:outline-none focus:border-primary',
                  errors.location && 'border-destructive'
                )}
              />
              {errors.location && (
                <p className="typo-c1 text-destructive mt-s1">{errors.location.message}</p>
              )}
            </div>

            {/* 최대 인원 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Users size={14} /> 최대 인원
              </label>
              <input
                type="number"
                {...register('capacity', { valueAsNumber: true })}
                className={cn(
                  'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                  'focus:outline-none focus:border-primary',
                  errors.capacity && 'border-destructive'
                )}
              />
              {errors.capacity && (
                <p className="typo-c1 text-destructive mt-s1">{errors.capacity.message}</p>
              )}
            </div>

            {/* 신청 방식 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <ListChecks size={14} /> 신청 방식
              </label>
              <div className="space-y-s2">
                <button
                  type="button"
                  onClick={() => setValue('registrationType', 'AUTO_APPROVE')}
                  className={cn(
                    'w-full rounded-r3 px-s4 py-s3 border text-left text-sm transition-colors cursor-pointer',
                    registrationType === 'AUTO_APPROVE'
                      ? 'border-primary bg-primary/5'
                      : 'border-border bg-muted/50'
                  )}
                >
                  <div className="flex items-center gap-s3">
                    <div className={cn(
                      'w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0',
                      registrationType === 'AUTO_APPROVE' ? 'border-primary' : 'border-muted-foreground/40'
                    )}>
                      {registrationType === 'AUTO_APPROVE' && (
                        <div className="w-2 h-2 rounded-full bg-primary" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium">선착순 (자동 승인)</p>
                      <p className="typo-c1 text-muted-foreground">신청 즉시 승인됩니다</p>
                    </div>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => setValue('registrationType', 'MANUAL_APPROVE')}
                  className={cn(
                    'w-full rounded-r3 px-s4 py-s3 border text-left text-sm transition-colors cursor-pointer',
                    registrationType === 'MANUAL_APPROVE'
                      ? 'border-primary bg-primary/5'
                      : 'border-border bg-muted/50'
                  )}
                >
                  <div className="flex items-center gap-s3">
                    <div className={cn(
                      'w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0',
                      registrationType === 'MANUAL_APPROVE' ? 'border-primary' : 'border-muted-foreground/40'
                    )}>
                      {registrationType === 'MANUAL_APPROVE' && (
                        <div className="w-2 h-2 rounded-full bg-primary" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium">선발제 (수동 승인)</p>
                      <p className="typo-c1 text-muted-foreground">관리자가 승인해야 합니다</p>
                    </div>
                  </div>
                </button>
              </div>
            </div>

            {/* 신청 시작 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Clock size={14} /> 신청 시작
              </label>
              <div className="space-y-s3">
                <div>
                  <input
                    type="date"
                    {...register('registrationStartDate')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                      'focus:outline-none focus:border-primary',
                      errors.registrationStartDate && 'border-destructive'
                    )}
                  />
                  {errors.registrationStartDate && (
                    <p className="typo-c1 text-destructive mt-s1">{errors.registrationStartDate.message}</p>
                  )}
                </div>
                <div>
                  <input
                    type="time"
                    {...register('registrationStartTime')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                      'focus:outline-none focus:border-primary',
                      errors.registrationStartTime && 'border-destructive'
                    )}
                  />
                  {errors.registrationStartTime && (
                    <p className="typo-c1 text-destructive mt-s1">{errors.registrationStartTime.message}</p>
                  )}
                </div>
              </div>
            </div>

            {/* 신청 마감 */}
            <div className="px-s5 py-s5">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Clock size={14} /> 신청 마감
              </label>
              <div className="space-y-s3">
                <div>
                  <input
                    type="date"
                    {...register('registrationDeadlineDate')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                      'focus:outline-none focus:border-primary',
                      errors.registrationDeadlineDate && 'border-destructive'
                    )}
                  />
                  {errors.registrationDeadlineDate && (
                    <p className="typo-c1 text-destructive mt-s1">{errors.registrationDeadlineDate.message}</p>
                  )}
                </div>
                <div>
                  <input
                    type="time"
                    {...register('registrationDeadlineTime')}
                    className={cn(
                      'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
                      'focus:outline-none focus:border-primary',
                      errors.registrationDeadlineTime && 'border-destructive'
                    )}
                  />
                  {errors.registrationDeadlineTime && (
                    <p className="typo-c1 text-destructive mt-s1">{errors.registrationDeadlineTime.message}</p>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
