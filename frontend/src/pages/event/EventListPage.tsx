import { useNavigate, useSearchParams } from 'react-router-dom';
import { FullPageSpinner } from '@/components/ui';
import { Plus } from 'lucide-react';
import { useEvents } from '@/hooks/queries/useEvents';
import EventCard from '@/components/feature/event/EventCard';
import { useAuthStore } from '@/stores/authStore';
import { FilterSelect } from '@/components/board/FilterSelect';
import { EVENT_FILTER_STATUS, EVENT_FILTER_LABELS, type EventFilterStatus } from '@/constants/event';
import type { GetEventListStatus } from '@/api/model/models/getEventListStatus';
import type { EventListResponse } from '@/api/model/models/eventListResponse';
import type { Event } from '@/types/entities';

export default function EventListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const user = useAuthStore((state) => state.user);

  // URL 쿼리 파라미터에서 검색어 및 필터 상태 읽기
  const searchKeyword = searchParams.get('search');
  const filterStatus = (searchParams.get('status') as EventFilterStatus) ?? EVENT_FILTER_STATUS.ALL;

  // 행사 목록 조회 (API에서 필터링)
  const { data: eventsResponse, isLoading } = useEvents(
    filterStatus === EVENT_FILTER_STATUS.ALL
      ? { ...(searchKeyword && { keyword: searchKeyword }) }
      : {
          status: filterStatus as GetEventListStatus,
          ...(searchKeyword && { keyword: searchKeyword })
        }
  );

  // Extract and transform API response to Event type
  const eventListData = (eventsResponse?.data as unknown as EventListResponse[]) ?? [];
  const events: Event[] = eventListData.map((apiEvent) => ({
    id: String(apiEvent.id ?? ''),
    title: apiEvent.title ?? '',
    description: '', // API doesn't provide description in list view
    date: apiEvent.eventStartAt ?? '',
    location: apiEvent.location ?? '',
    status: (apiEvent.status as Event['status']) ?? 'UPCOMING',
    ...(apiEvent.eventStartAt && { startDate: apiEvent.eventStartAt }),
    ...(apiEvent.eventEndAt && { endDate: apiEvent.eventEndAt }),
    ...(apiEvent.capacity !== undefined && { capacity: apiEvent.capacity }),
    ...(apiEvent.currentCount !== undefined && { currentCount: apiEvent.currentCount }),
    ...(apiEvent.registrationEndAt && { registrationDeadline: apiEvent.registrationEndAt }),
  }));

  // 필터 변경 핸들러
  const handleFilterChange = (newStatus: EventFilterStatus) => {
    const newParams = new URLSearchParams(searchParams);
    if (newStatus === EVENT_FILTER_STATUS.ALL) {
      newParams.delete('status');
    } else {
      newParams.set('status', newStatus);
    }
    setSearchParams(newParams);
  };

  // OPERATOR 이상만 행사 작성 가능
  const canCreateEvent = user?.role === 'OPERATOR' || user?.role === 'ADMIN';

  if (isLoading) {
    return <FullPageSpinner />;
  }

  return (
    <div className="space-y-s8 animate-in fade-in duration-300">
      {/* Header with Filter and Actions */}
      <div className="flex items-center justify-end gap-s4 border-b border-border pb-s4">
        <FilterSelect
          value={filterStatus}
          onChange={handleFilterChange}
          options={EVENT_FILTER_LABELS}
        />
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

      {/* Event List */}
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

      {/* Empty State */}
      {events?.length === 0 && (
        <div className="text-center py-s7 text-muted-foreground">
          {filterStatus !== EVENT_FILTER_STATUS.ALL || searchKeyword
            ? '검색 조건에 맞는 행사가 없습니다.'
            : '등록된 행사가 없습니다.'}
        </div>
      )}
    </div>
  );
}
