import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Calendar, MapPin, Users, Image as ImageIcon, Clock, Save } from 'lucide-react';
import { useCreateEvent } from '@/hooks/queries/useEvents';
import { cn } from '@/lib/utils';

const eventSchema = z.object({
  title: z.string().min(1, '행사 제목을 입력하세요'),
  description: z.string().min(1, '행사 설명을 입력하세요'),
  content: z.string().optional(),
  date: z.string().min(1, '행사 날짜를 선택하세요'),
  time: z.string().min(1, '행사 시간을 선택하세요'),
  location: z.string().min(1, '장소를 입력하세요'),
  capacity: z.number().min(1, '최대 인원은 1명 이상이어야 합니다'),
});

type EventForm = z.infer<typeof eventSchema>;

export default function EventWritePage() {
  const navigate = useNavigate();
  const { mutate: createEvent, isPending } = useCreateEvent();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<EventForm>({
    resolver: zodResolver(eventSchema),
    defaultValues: {
      capacity: 30,
    },
  });

  const onSubmit = (data: EventForm) => {
    createEvent(
      {
        title: data.title,
        description: data.description,
        content: data.content,
        startDate: `${data.date}T${data.time}:00`,
        location: data.location,
        capacity: data.capacity,
      },
      {
        onSuccess: (event) => {
          alert('행사가 등록되었습니다.');
          navigate(`/events/${event.id}`);
        },
        onError: (error) => {
          alert(`행사 등록 실패: ${error.message}`);
        },
      }
    );
  };

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="flex justify-between items-center mb-s8">
          <button
            type="button"
            onClick={() => navigate('/events')}
            className="flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
          >
            <ArrowLeft size={18} /> Cancel
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="bg-primary text-primary-foreground px-s6 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save size={16} /> {isPending ? 'Registering...' : 'Register Event'}
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-s8">
          {/* Left Column - Main Info */}
          <div className="md:col-span-2 space-y-s6">
            <div className="p-s8 rounded-[2.5rem] border bg-card border-border shadow-sm">
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
                      'w-full text-2xl font-bold bg-transparent border-b pb-2 focus:outline-none focus:border-primary border-border',
                      errors.title && 'border-destructive'
                    )}
                    placeholder="Ex: Spring Networking Night"
                  />
                  {errors.title && (
                    <p className="text-destructive text-sm mt-1">{errors.title.message}</p>
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
                    <p className="text-destructive text-sm mt-1">{errors.description.message}</p>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Right Column - Logistics */}
          <div className="space-y-s6">
            <div className="p-s8 rounded-[2.5rem] border bg-card border-border shadow-sm">
              <h3 className="text-lg font-bold mb-s6">Logistics</h3>

              <div className="space-y-s4">
                <div className="relative group cursor-pointer h-40 rounded-r4 bg-muted/50 overflow-hidden flex items-center justify-center border border-dashed border-border hover:border-primary transition-colors">
                  <div className="text-center text-muted-foreground">
                    <ImageIcon className="mx-auto mb-s2" />
                    <span className="text-xs font-bold">Upload Cover</span>
                    <p className="text-c2 mt-1">(Coming soon)</p>
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
                    <p className="text-destructive text-sm mt-1">{errors.date.message}</p>
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
                    <p className="text-destructive text-sm mt-1">{errors.time.message}</p>
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
                    <p className="text-destructive text-sm mt-1">{errors.location.message}</p>
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
                    <p className="text-destructive text-sm mt-1">{errors.capacity.message}</p>
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
