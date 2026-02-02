import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useEvents } from '@/hooks/queries/useEvents';
import EventCard from '@/components/feature/event/EventCard';
import { useAuthStore } from '@/stores/authStore';

export default function EventListPage() {
  const navigate = useNavigate();
  const { data: events, isLoading } = useEvents();
  const user = useAuthStore((state) => state.user);

  // OPERATOR 이상만 행사 작성 가능
  const canCreateEvent = user?.role === 'OPERATOR' || user?.role === 'ADMIN';

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading...</div>
      </div>
    );
  }

  return (
    <div className="animate-in slide-in-from-bottom-4 duration-500">
      <div className="flex justify-between items-center mb-s6">
        <h2 className="text-2xl font-bold">Upcoming Events</h2>
        {canCreateEvent && (
          <button
            type="button"
            onClick={() => navigate('/events/write')}
            className="flex items-center gap-s2 px-s4 py-s2 rounded-full bg-primary text-primary-foreground text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 cursor-pointer"
          >
            <Plus size={16} /> Create Event
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-s8">
        {events?.map((event) => (
          <div
            key={event.id}
            onClick={() => navigate(`/events/${event.id}`)}
            className="cursor-pointer"
          >
            <EventCard event={event} />
          </div>
        ))}
      </div>

      {events?.length === 0 && (
        <div className="text-center py-12 text-muted-foreground">
          등록된 행사가 없습니다.
        </div>
      )}
    </div>
  );
}
