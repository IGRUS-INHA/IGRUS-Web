import { Calendar, MapPin, Users, ArrowUpRight } from 'lucide-react';
import type { Event, EventStatus } from '@/types/entities';

const STATUS_STYLES: Record<string, string> = {
  Open: 'bg-primary text-primary-foreground',
  OPEN: 'bg-primary text-primary-foreground',
  UPCOMING: 'bg-primary text-primary-foreground',
  Full: 'bg-warning text-foreground',
  CLOSED: 'bg-muted text-muted-foreground',
  Closed: 'bg-muted text-muted-foreground',
  COMPLETED: 'bg-muted text-muted-foreground',
  ONGOING: 'bg-primary text-primary-foreground',
};

const STATUS_LABELS: Record<string, string> = {
  Open: '신청 가능',
  OPEN: '진행중',
  UPCOMING: '예정',
  Full: '마감',
  CLOSED: '마감',
  Closed: '종료',
  COMPLETED: '종료',
  ONGOING: '진행중',
};

interface EventCardProps {
  event: Event;
}

export default function EventCard({ event }: EventCardProps) {
  const isAvailable = event.status === 'Open' || event.status === 'OPEN' || event.status === 'UPCOMING';

  return (
    <div className="rounded-r4 overflow-hidden border transition-all hover:scale-[1.01] bg-card border-border shadow-xl shadow-black/5 dark:shadow-none">

      <div className="h-48 relative">
        {event.image && (
          <img src={event.image} alt={event.title} className="w-full h-full object-cover opacity-80" />
        )}
        <div
          className={`absolute top-s4 right-s4 px-s3 py-s1 rounded-full text-c2 font-bold uppercase tracking-wider ${
            STATUS_STYLES[event.status] ?? STATUS_STYLES.Closed
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
            <span className="text-sm">{event.date}</span>
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
