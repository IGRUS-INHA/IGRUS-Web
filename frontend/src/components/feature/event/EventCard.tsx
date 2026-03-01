import { Calendar, MapPin, Users, ArrowUpRight } from 'lucide-react';
import type { Event } from '@/types/entities';
import { formatDate } from '@/utils/date';

const STATUS_STYLES: Record<string, string> = {
  UPCOMING: 'bg-primary text-primary-foreground',
  ONGOING: 'bg-primary text-primary-foreground',
  COMPLETED: 'bg-muted text-muted-foreground',
  CLOSED: 'bg-muted text-muted-foreground',
};

const STATUS_LABELS: Record<string, string> = {
  UPCOMING: '예정',
  ONGOING: '진행중',
  COMPLETED: '완료',
  CLOSED: '마감',
};

interface EventCardProps {
  event: Event;
}

export default function EventCard({ event }: EventCardProps) {
  const isAvailable = event.status === 'UPCOMING' || event.status === 'ONGOING';

  return (
    <div className="rounded-r4 overflow-hidden border transition-all hover:scale-[1.01] bg-card border-border shadow-xl shadow-black/5 dark:shadow-none">

      <div className="h-48 relative bg-muted/30">
        <img
          src={event.image || '/igruslogo2.png'}
          alt={event.title}
          className={event.image ? 'w-full h-full object-cover opacity-80' : 'absolute inset-0 m-auto h-40 w-40 object-contain'}
        />
        <div
          className={`absolute top-s4 right-s4 px-s3 py-s1 rounded-full typo-c2 font-bold uppercase tracking-wider ${
            STATUS_STYLES[event.status] ?? STATUS_STYLES.CLOSED
          }`}
        >
          {STATUS_LABELS[event.status] ?? event.status}
        </div>
      </div>

      <div className="p-s6">
        <h3 className="text-2xl font-bold mb-s4">{event.title}</h3>

        <div className="space-y-s3 mb-s6">
          <div className="flex items-center gap-s3 text-muted-foreground">
            <Calendar size={18} className="text-primary" />
            <span className="text-sm">{formatDate(event.date)}</span>
          </div>
          <div className="flex items-center gap-s3 text-muted-foreground">
            <MapPin size={18} className="text-primary" />
            <span className="text-sm">{event.location}</span>
          </div>
          <div className="flex items-center gap-s3 text-muted-foreground">
            <Users size={18} className="text-primary" />
            <span className="text-sm">
              {event.attendees ?? event.currentCount ?? 0} / {event.maxCapacity ?? event.capacity ?? 'Unlimited'} applied
            </span>
          </div>
        </div>

        <div
          className={`w-full py-s4 rounded-r4 font-bold flex items-center justify-center gap-s2 transition-all ${
            isAvailable
              ? 'bg-primary text-primary-foreground hover:bg-primary/90 shadow-lg shadow-primary/20 cursor-pointer'
              : 'bg-muted text-muted-foreground cursor-not-allowed'
          }`}
        >
          {isAvailable ? 'Apply Now' : 'Application Unavailable'}
          {isAvailable && <ArrowUpRight size={18} />}
        </div>
      </div>
    </div>
  );
}
