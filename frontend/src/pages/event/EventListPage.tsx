import { useNavigate, useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import { useEvents, useAdminEvents } from "@/hooks/queries/useEvents";
import EventAccordionItem from "@/components/feature/event/EventAccordionItem";
import EventSidebar from "@/components/feature/event/EventSidebar";
import { useAuthStore } from "@/stores/authStore";
import {
  EVENT_FILTER_STATUS,
  EVENT_FILTER_LABELS,
  type EventFilterStatus,
} from "@/constants/event";
import type { EventListResponse } from "@/api/model/models/eventListResponse";
import type { AdminEventListResponse } from "@/api/model/models/adminEventListResponse";
import type { GetEventListParams } from "@/api/model/models/getEventListParams";
import type { GetAdminEventListParams } from "@/api/model/models/getAdminEventListParams";
import type { Event } from "@/types/entities";
import { isForbiddenError } from "@/utils/error";

// Filter status dot colors for non-active tabs
const FILTER_DOT_COLOR: Partial<Record<EventFilterStatus, string>> = {
  [EVENT_FILTER_STATUS.OPEN]: "bg-success",
  [EVENT_FILTER_STATUS.ONGOING]: "bg-primary",
  [EVENT_FILTER_STATUS.COMPLETED]: "bg-muted-foreground",
};

function buildEventListParams(
  filterStatus: EventFilterStatus,
): GetEventListParams | undefined {
  switch (filterStatus) {
    case EVENT_FILTER_STATUS.ALL:
      return undefined;
    case EVENT_FILTER_STATUS.OPEN:
      return { registrationStatus: "OPEN" };
    case EVENT_FILTER_STATUS.ONGOING:
      return { eventStatus: "ONGOING" };
    case EVENT_FILTER_STATUS.COMPLETED:
      return { eventStatus: "COMPLETED" };
    default:
      return undefined;
  }
}

function buildAdminEventListParams(
  filterStatus: EventFilterStatus,
): GetAdminEventListParams | undefined {
  switch (filterStatus) {
    case EVENT_FILTER_STATUS.ALL:
      return undefined;
    case EVENT_FILTER_STATUS.OPEN:
      return { registrationStatus: "OPEN" };
    case EVENT_FILTER_STATUS.ONGOING:
      return { eventStatus: "ONGOING" };
    case EVENT_FILTER_STATUS.COMPLETED:
      return { eventStatus: "COMPLETED" };
    default:
      return undefined;
  }
}

function mapToEvent(
  apiEvent: EventListResponse | AdminEventListResponse,
): Event {
  return {
    id: String(apiEvent.id ?? ""),
    title: apiEvent.title ?? "",
    description: "",
    date: apiEvent.eventStartAt ?? "",
    location: apiEvent.location ?? "",
    status: (apiEvent.eventStatus as Event["status"]) ?? "UPCOMING",
    visibility: apiEvent.visibility,
    registrationStatus: apiEvent.registrationStatus,
    ...(apiEvent.eventStartAt && { startDate: apiEvent.eventStartAt }),
    ...(apiEvent.eventEndAt && { endDate: apiEvent.eventEndAt }),
    ...(apiEvent.capacity !== undefined && { capacity: apiEvent.capacity }),
    ...(apiEvent.currentCount !== undefined && {
      currentCount: apiEvent.currentCount,
    }),
    ...(apiEvent.registrationEndAt && {
      registrationDeadline: apiEvent.registrationEndAt,
    }),
  };
}

function groupByMonth(events: Event[]): [string, Event[]][] {
  const map = new Map<string, Event[]>();
  for (const event of events) {
    const d = new Date(event.startDate ?? event.date);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(event);
  }
  // Descending order (newest month first, newest date first within each month)
  return Array.from(map.entries())
    .sort(([a], [b]) => b.localeCompare(a))
    .map(
      ([key, monthEvents]) =>
        [
          key,
          [...monthEvents].sort((a, b) => {
            const da = new Date(a.startDate ?? a.date).getTime();
            const db = new Date(b.startDate ?? b.date).getTime();
            return db - da;
          }),
        ] as [string, Event[]],
    );
}

function formatMonthLabel(key: string): string {
  const [year, month] = key.split("-");
  return `${year}년 ${Number(month)}월`;
}

const FILTER_TABS = [
  EVENT_FILTER_STATUS.ALL,
  EVENT_FILTER_STATUS.OPEN,
  EVENT_FILTER_STATUS.ONGOING,
  EVENT_FILTER_STATUS.COMPLETED,
] as const;

export default function EventListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const user = useAuthStore((state) => state.user);

  const isOperator = user?.role === "OPERATOR" || user?.role === "ADMIN";

  const filterStatus =
    (searchParams.get("status") as EventFilterStatus) ??
    EVENT_FILTER_STATUS.ALL;

  // Filtered queries
  const publicQuery = useEvents(
    buildEventListParams(filterStatus),
    !isOperator,
  );
  const adminQuery = useAdminEvents(
    buildAdminEventListParams(filterStatus),
    isOperator,
  );
  const activeQuery = isOperator ? adminQuery : publicQuery;
  const { isLoading, error } = activeQuery;

  const eventListData =
    (activeQuery.data?.data as unknown as (
      | EventListResponse
      | AdminEventListResponse
    )[]) ?? [];
  const events: Event[] = eventListData.map(mapToEvent);

  // Unfiltered queries (for sidebar statistics)
  const allPublicQuery = useEvents(undefined, !isOperator);
  const allAdminQuery = useAdminEvents(undefined, isOperator);
  const allQuery = isOperator ? allAdminQuery : allPublicQuery;
  const allEventListData =
    (allQuery.data?.data as unknown as (
      | EventListResponse
      | AdminEventListResponse
    )[]) ?? [];
  const allEvents: Event[] = allEventListData.map(mapToEvent);

  const handleFilterChange = (newStatus: EventFilterStatus) => {
    const newParams = new URLSearchParams(searchParams);
    if (newStatus === EVENT_FILTER_STATUS.ALL) {
      newParams.delete("status");
    } else {
      newParams.set("status", newStatus);
    }
    setSearchParams(newParams);
  };

  const isForbidden = isForbiddenError(error);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading...</div>
      </div>
    );
  }

  if (isForbidden) {
    return (
      <div className="text-center py-12 space-y-s4">
        <p className="text-muted-foreground">
          정회원 승인 후 행사 목록 조회가 가능합니다.
        </p>
      </div>
    );
  }

  const grouped = groupByMonth(events);

  return (
    <div className="space-y-s6 animate-in fade-in duration-300">
      {/* Page header */}
      <div>
        <p className="text-xs font-bold text-primary tracking-widest mb-s1">
          EVENTS
        </p>
        <h1 className="text-xl md:text-3xl font-bold mb-s2">IGRUS 행사</h1>
        <p className="text-xs md:text-sm text-muted-foreground">
          아이그루스에서 진행되는 다양한 활동을 확인해보세요.
        </p>
      </div>

      {/* Filter tabs + action button */}
      <div className="flex items-center gap-s2 flex-wrap">
        {FILTER_TABS.map((tab) => {
          const isActive = filterStatus === tab;
          const dotColor = FILTER_DOT_COLOR[tab];
          return (
            <button
              key={tab}
              type="button"
              onClick={() => handleFilterChange(tab)}
              className={`flex items-center gap-s2 px-s3 md:px-s4 py-s1 md:py-s2 rounded-full text-xs md:text-sm font-bold transition cursor-pointer ${
                isActive
                  ? "bg-primary text-primary-foreground"
                  : "border border-border text-foreground hover:bg-muted"
              }`}
            >
              {dotColor && !isActive && (
                <span className={`w-1.5 h-1.5 rounded-full ${dotColor}`} />
              )}
              {EVENT_FILTER_LABELS[tab]}
            </button>
          );
        })}

        {isOperator && (
          <button
            type="button"
            onClick={() => navigate("/events/create")}
            className="ml-auto flex items-center gap-s2 px-s3 md:px-s4 py-s1 md:py-s2 rounded-full bg-primary text-primary-foreground text-xs md:text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 cursor-pointer"
          >
            <Plus size={16} /> 행사 등록
          </button>
        )}
      </div>

      {/* Main layout: event list + sidebar */}
      <div className="flex gap-s8 items-start">
        {/* Event list */}
        <div className="flex-1 min-w-0">
          {grouped.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              {filterStatus !== EVENT_FILTER_STATUS.ALL
                ? "검색 조건에 맞는 행사가 없습니다."
                : "등록된 행사가 없습니다."}
            </div>
          ) : (
            /* 단일 타임라인 컨테이너: 모든 월 그룹을 하나의 선이 관통 */
            <div className="relative space-y-s6">
              {/* 연속 세로선 */}
              <div className="absolute left-[15px] top-3 -bottom-8 w-0.5 bg-primary/40 rounded-full" />

              {grouped.map(([monthKey, monthEvents]) => (
                <div key={monthKey}>
                  {/* Month header pill — z-10으로 선 위에 올라탐 */}
                  <div className="relative z-10 inline-flex items-center gap-s2 px-s3 py-s1 rounded-full bg-primary text-white text-xs font-bold mb-s3">
                    <span className="w-2 h-2 rounded-full bg-white" />
                    {formatMonthLabel(monthKey)}
                  </div>

                  {/* Event rows */}
                  <div className="ml-s5 space-y-s3">
                    {monthEvents.map((event) => (
                      <EventAccordionItem key={event.id} event={event} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right sidebar (desktop only) */}
        <EventSidebar events={allEvents} />
      </div>
    </div>
  );
}
