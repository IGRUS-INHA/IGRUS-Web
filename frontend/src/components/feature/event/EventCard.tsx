import { Calendar, MapPin } from "lucide-react";
import type { Event } from "@/types/entities";
import { formatDate } from "@/utils/date";
import {
  EVENT_STATUS_BADGE,
  EVENT_STATUS_LABEL,
  REG_STATUS_BADGE,
  REG_STATUS_LABEL,
  VISIBILITY_BADGE,
  VISIBILITY_LABEL,
} from "@/constants/eventStatus";

interface EventCardProps {
  event: Event;
}

export default function EventCard({ event }: EventCardProps) {
  const regStatus = event.registrationStatus;
  // COMPLETED 또는 CANCELED → 종료 상태 (모집 배지 숨김, 수정/신청취소 불가)
  const isEventEnded =
    event.status === "COMPLETED" || event.status === "CANCELED";

  // UPCOMING + 모집 상태 있음 → 모집 상태만 표시 (행사 상태 배지 숨김)
  const isUpcomingWithRegStatus =
    event.status === "UPCOMING" &&
    (regStatus === "NOT_STARTED" || regStatus === "OPEN");

  const showEventStatus = !isUpcomingWithRegStatus;
  const eventStatusBadge =
    EVENT_STATUS_BADGE[event.status] ?? "bg-muted text-muted-foreground";
  const eventStatusLabel = EVENT_STATUS_LABEL[event.status] ?? event.status;

  const showRegStatus =
    isUpcomingWithRegStatus ||
    (!isEventEnded &&
      regStatus &&
      (regStatus === "OPEN" || regStatus === "CLOSED"));
  const regStatusBadge =
    showRegStatus && regStatus ? REG_STATUS_BADGE[regStatus] : null;
  const regStatusLabel =
    showRegStatus && regStatus ? REG_STATUS_LABEL[regStatus] : null;

  const isUnpublished = event.visibility === "UNPUBLISHED";

  return (
    <div className="rounded-r4 overflow-hidden border transition-all hover:scale-[1.01] bg-card border-border shadow-xl shadow-black/5 dark:shadow-none">
      <div className="h-48 relative bg-muted/30">
        <img
          src={event.image || "/igruslogo2.png"}
          alt={event.title}
          className={
            event.image
              ? "w-full h-full object-cover opacity-80"
              : "absolute inset-0 m-auto h-40 w-40 object-contain"
          }
        />
        <div className="absolute top-s4 left-s4 flex gap-s2">
          {isUnpublished && (
            <span
              className={`px-s3 py-s1 rounded-full typo-c2 font-bold tracking-wider ${VISIBILITY_BADGE["UNPUBLISHED"]}`}
            >
              {VISIBILITY_LABEL["UNPUBLISHED"]}
            </span>
          )}
          {showEventStatus && (
            <span
              className={`px-s3 py-s1 rounded-full typo-c2 font-bold tracking-wider ${eventStatusBadge}`}
            >
              {eventStatusLabel}
            </span>
          )}
          {regStatusLabel && (
            <span
              className={`px-s3 py-s1 rounded-full typo-c2 font-bold tracking-wider ${regStatusBadge}`}
            >
              {regStatusLabel}
            </span>
          )}
        </div>
      </div>

      <div className="p-s6">
        <h3 className="text-2xl font-bold mb-s4">{event.title}</h3>

        <div className="space-y-s3">
          <div className="flex items-center gap-s3 text-muted-foreground">
            <Calendar size={18} className="text-primary" />
            <span className="text-sm">{formatDate(event.date)}</span>
          </div>
          <div className="flex items-center gap-s3 text-muted-foreground">
            <MapPin size={18} className="text-primary" />
            <span className="text-sm">{event.location}</span>
          </div>
          <div className="mt-s4">
            <div className="flex items-center justify-between mb-s2">
              <span className="text-sm font-medium text-muted-foreground">
                참여 인원
              </span>
              <span className="text-sm font-medium text-muted-foreground">
                {event.attendees ?? event.currentCount ?? 0}/
                {event.maxCapacity ?? event.capacity ?? "∞"}
              </span>
            </div>
            <div className="w-full h-2 rounded-full bg-muted overflow-hidden">
              <div
                className="h-full rounded-full bg-primary transition-all"
                style={{
                  width: `${Math.min(((event.attendees ?? event.currentCount ?? 0) / (event.maxCapacity ?? event.capacity ?? 1)) * 100, 100)}%`,
                }}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
