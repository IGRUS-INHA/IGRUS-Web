import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { Event } from "@/types/entities";

interface EventSidebarProps {
  events: Event[];
}

const WEEK_DAYS = ["일", "월", "화", "수", "목", "금", "토"];

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month + 1, 0).getDate();
}

function getFirstDayOfMonth(year: number, month: number): number {
  return new Date(year, month, 1).getDay();
}

export default function EventSidebar({ events }: EventSidebarProps) {
  const today = new Date();
  const [calYear, setCalYear] = useState(today.getFullYear());
  const [calMonth, setCalMonth] = useState(today.getMonth());

  // Event dates set for indicator dots
  const eventDatesInMonth = new Set(
    events
      .map((e) => {
        const d = new Date(e.startDate ?? e.date);
        if (d.getFullYear() === calYear && d.getMonth() === calMonth) {
          return d.getDate();
        }
        return null;
      })
      .filter(Boolean),
  );

  // Calendar grid
  const daysInMonth = getDaysInMonth(calYear, calMonth);
  const firstDay = getFirstDayOfMonth(calYear, calMonth);
  const calCells: (number | null)[] = [
    ...Array<null>(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  const prevMonth = () => {
    if (calMonth === 0) {
      setCalYear((y) => y - 1);
      setCalMonth(11);
    } else {
      setCalMonth((m) => m - 1);
    }
  };
  const nextMonth = () => {
    if (calMonth === 11) {
      setCalYear((y) => y + 1);
      setCalMonth(0);
    } else {
      setCalMonth((m) => m + 1);
    }
  };

  // Statistics
  const totalEvents = events.length;
  const ongoingCount = events.filter((e) => e.status === "ONGOING").length;
  const totalParticipants = events.reduce(
    (sum, e) => sum + (e.currentCount ?? 0),
    0,
  );
  const thisMonthCount = events.filter((e) => {
    const d = new Date(e.startDate ?? e.date);
    return (
      d.getFullYear() === today.getFullYear() &&
      d.getMonth() === today.getMonth()
    );
  }).length;

  return (
    <aside className="hidden lg:flex flex-col gap-s4 w-72 shrink-0">
      {/* Mini Calendar */}
      <div className="rounded-r4 border border-border bg-card p-s4">
        {/* Calendar header */}
        <div className="flex items-center justify-between mb-s3">
          <span className="text-sm font-bold">
            {calYear}년 {calMonth + 1}월
          </span>
          <div className="flex gap-s1">
            <button
              type="button"
              onClick={prevMonth}
              className="p-1 rounded hover:bg-muted transition cursor-pointer text-muted-foreground hover:text-foreground"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              type="button"
              onClick={nextMonth}
              className="p-1 rounded hover:bg-muted transition cursor-pointer text-muted-foreground hover:text-foreground"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>

        {/* Week day headers */}
        <div className="grid grid-cols-7 mb-s1">
          {WEEK_DAYS.map((d) => (
            <div
              key={d}
              className="text-center text-xs text-muted-foreground font-medium py-1"
            >
              {d}
            </div>
          ))}
        </div>

        {/* Day cells */}
        <div className="grid grid-cols-7 gap-y-0.5">
          {calCells.map((day, idx) => {
            if (!day) {
              return <div key={`empty-${idx}`} />;
            }
            const isToday =
              day === today.getDate() &&
              calMonth === today.getMonth() &&
              calYear === today.getFullYear();
            const hasEvent = eventDatesInMonth.has(day);
            return (
              <div key={day} className="flex flex-col items-center py-0.5">
                <span
                  className={`w-7 h-7 flex items-center justify-center text-xs rounded-full font-medium transition ${
                    isToday
                      ? "bg-primary text-primary-foreground font-bold"
                      : "hover:bg-muted text-foreground"
                  }`}
                >
                  {day}
                </span>
                {hasEvent && !isToday && (
                  <span className="w-1 h-1 rounded-full bg-primary mt-0.5" />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Statistics */}
      <div className="rounded-r4 border border-border bg-card p-s4">
        <p className="text-xs font-bold text-muted-foreground tracking-wider mb-s3">
          STATISTICS
        </p>
        <div className="grid grid-cols-2 gap-s4">
          <div>
            <p className="text-2xl font-bold">{totalEvents}</p>
            <p className="text-xs text-muted-foreground mt-0.5">전체 행사</p>
          </div>
          <div>
            <p className="text-2xl font-bold">{ongoingCount}</p>
            <p className="text-xs text-muted-foreground mt-0.5">진행중</p>
          </div>
          <div>
            <p className="text-2xl font-bold">{totalParticipants}</p>
            <p className="text-xs text-muted-foreground mt-0.5">총 참여자</p>
          </div>
          <div>
            <p className="text-2xl font-bold">{thisMonthCount}</p>
            <p className="text-xs text-muted-foreground mt-0.5">이번 달</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
