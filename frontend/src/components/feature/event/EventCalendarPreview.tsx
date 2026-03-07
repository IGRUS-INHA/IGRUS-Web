import DatePicker, { registerLocale } from "react-datepicker";
import { ko } from "date-fns/locale";
import "react-datepicker/dist/react-datepicker.css";
import { Calendar, ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { formatDateLocal } from "@/utils/event";
import { cn } from "@/lib/utils";

registerLocale("ko", ko);

type EditMode = "registration" | "event";

interface EventCalendarPreviewProps {
  eventStartDate: string;
  eventEndDate: string;
  registrationStartDate: string;
  registrationEndDate: string;
  onEventDateChange?: (startDate: string, endDate: string) => void;
  onRegistrationDateChange?: (startDate: string, endDate: string) => void;
  onRegistrationPresetChange?: () => void;
  onSelectionStart?: (mode: EditMode) => void;
  showModeToggle?: boolean;
}

function parseDateParts(str: string): [number, number, number] | null {
  const parts = str.split("-").map(Number);
  const y = parts[0];
  const m = parts[1];
  const d = parts[2];
  if (y === undefined || m === undefined || d === undefined) return null;
  return [y, m, d];
}

function toDate(str: string): Date | null {
  if (!str) return null;
  const p = parseDateParts(str);
  if (!p) return null;
  return new Date(p[0], p[1] - 1, p[2]);
}

function getDateRange(startStr: string, endStr: string): Date[] {
  if (!startStr || !endStr) return [];
  const start = toDate(startStr);
  const end = toDate(endStr);
  if (!start || !end || isNaN(start.getTime()) || isNaN(end.getTime()))
    return [];

  const dates: Date[] = [];
  const current = new Date(start);
  while (current <= end) {
    dates.push(new Date(current));
    current.setDate(current.getDate() + 1);
  }
  return dates;
}

export function EventCalendarPreview({
  eventStartDate,
  eventEndDate,
  registrationStartDate,
  registrationEndDate,
  onEventDateChange,
  onRegistrationDateChange,
  onRegistrationPresetChange,
  onSelectionStart,
  showModeToggle = true,
}: EventCalendarPreviewProps) {
  const [mode, setMode] = useState<EditMode>("registration");
  // 범위 선택 중간 상태 (첫 번째 클릭 후, 두 번째 클릭 전)
  const [selectingStart, setSelectingStart] = useState<Date | null>(null);

  const openToDate = useMemo(() => {
    return (
      toDate(registrationStartDate) ?? toDate(eventStartDate) ?? new Date()
    );
  }, [eventStartDate, registrationStartDate]);

  // preview 모드 월 네비게이션
  const [viewDate, setViewDate] = useState<Date>(openToDate);

  // 외부에서 날짜가 변경되면 선택 중간 상태 초기화
  useEffect(() => {
    setSelectingStart(null);
  }, [
    eventStartDate,
    eventEndDate,
    registrationStartDate,
    registrationEndDate,
  ]);

  // openToDate가 변경되면 viewDate 동기화 (preview 모드)
  useEffect(() => {
    setViewDate(openToDate);
  }, [openToDate]);

  // 활성 범위: 선택 중이면 내부 상태 사용, 아니면 부모 props 사용
  const committedStart =
    mode === "event" ? toDate(eventStartDate) : toDate(registrationStartDate);
  const committedEnd =
    mode === "event" ? toDate(eventEndDate) : toDate(registrationEndDate);
  const activeStart = selectingStart ?? committedStart;
  const activeEnd = selectingStart ? null : committedEnd;

  // 비활성 범위: 항상 하이라이트로 표시
  const inactiveHighlight = useMemo(() => {
    if (mode === "event") {
      return [
        {
          "cal-preview-reg": getDateRange(
            registrationStartDate,
            registrationEndDate,
          ),
        },
      ];
    }
    return [
      { "cal-preview-event": getDateRange(eventStartDate, eventEndDate) },
    ];
  }, [
    mode,
    eventStartDate,
    eventEndDate,
    registrationStartDate,
    registrationEndDate,
  ]);

  // preview 모드 월 네비게이션 범위
  const minMonth = useMemo(() => {
    const dates = [
      registrationStartDate,
      registrationEndDate,
      eventStartDate,
      eventEndDate,
    ]
      .map(toDate)
      .filter((d): d is Date => d !== null);
    if (!dates.length) return null;
    return dates.reduce((a, b) => (a < b ? a : b));
  }, [
    registrationStartDate,
    registrationEndDate,
    eventStartDate,
    eventEndDate,
  ]);

  const maxMonth = useMemo(() => {
    const dates = [
      registrationStartDate,
      registrationEndDate,
      eventStartDate,
      eventEndDate,
    ]
      .map(toDate)
      .filter((d): d is Date => d !== null);
    if (!dates.length) return null;
    return dates.reduce((a, b) => (a > b ? a : b));
  }, [
    registrationStartDate,
    registrationEndDate,
    eventStartDate,
    eventEndDate,
  ]);

  const spansMultipleMonths =
    minMonth !== null &&
    maxMonth !== null &&
    (minMonth.getFullYear() !== maxMonth.getFullYear() ||
      minMonth.getMonth() !== maxMonth.getMonth());

  const toMonthIndex = (d: Date) => d.getFullYear() * 12 + d.getMonth();
  const viewMonthIndex = toMonthIndex(viewDate);
  const canPrev = minMonth ? viewMonthIndex > toMonthIndex(minMonth) : true;
  const canNext = maxMonth ? viewMonthIndex < toMonthIndex(maxMonth) : true;

  const handlePrevMonth = () => {
    setViewDate((d) => {
      const prev = new Date(d.getFullYear(), d.getMonth() - 1, 1);
      return prev;
    });
  };

  const handleNextMonth = () => {
    setViewDate((d) => {
      const next = new Date(d.getFullYear(), d.getMonth() + 1, 1);
      return next;
    });
  };

  // 신청기간 모드에서 행사 시작일 이전까지만 선택 가능
  const registrationMaxDate = useMemo(() => {
    if (mode !== "registration") return undefined;
    return toDate(eventStartDate) ?? undefined;
  }, [mode, eventStartDate]);

  const handleRangeChange = (dates: [Date | null, Date | null]) => {
    const [start, end] = dates;
    if (!start) return;

    if (!end) {
      // 첫 번째 클릭: 내부 상태로만 추적 (부모 폼 건드리지 않음)
      onSelectionStart?.(mode);
      setSelectingStart(start);
    } else {
      // 두 번째 클릭: 범위 완성 → 부모에 커밋
      setSelectingStart(null);
      const startStr = formatDateLocal(start);
      const endStr = formatDateLocal(end);
      if (mode === "event") {
        onEventDateChange?.(startStr, endStr);
      } else {
        onRegistrationPresetChange?.();
        onRegistrationDateChange?.(startStr, endStr);
      }
    }
  };

  const handleModeChange = (newMode: EditMode) => {
    setSelectingStart(null); // 모드 전환 시 선택 중간 상태 초기화
    setMode(newMode);
  };

  return (
    <div className="px-s5 py-s5">
      <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
        <Calendar size={14} /> 일정 캘린더
      </label>

      {/* Mode toggle tabs */}
      {showModeToggle && (
        <div className="flex flex-col gap-s1 mb-s3">
          <button
            type="button"
            onClick={() => handleModeChange("registration")}
            className={cn(
              "flex-1 px-s3 py-s2 rounded-r2 text-xs font-medium transition-colors cursor-pointer",
              mode === "registration"
                ? "bg-brand-l2 text-brand-l7"
                : "bg-muted/50 text-muted-foreground hover:bg-muted",
            )}
          >
            신청 기간
          </button>
          <button
            type="button"
            onClick={() => handleModeChange("event")}
            className={cn(
              "flex-1 px-s3 py-s2 rounded-r2 text-xs font-medium transition-colors cursor-pointer",
              mode === "event"
                ? "bg-primary text-primary-foreground"
                : "bg-muted/50 text-muted-foreground hover:bg-muted",
            )}
          >
            행사 기간
          </button>
        </div>
      )}

      {/* preview 모드 월 네비게이션 */}
      {!showModeToggle && spansMultipleMonths && (
        <div className="flex items-center justify-between mb-s2">
          <button
            type="button"
            onClick={handlePrevMonth}
            disabled={!canPrev}
            className={cn(
              "p-1 rounded hover:bg-muted transition cursor-pointer text-muted-foreground hover:text-foreground",
              !canPrev && "opacity-30 cursor-default pointer-events-none",
            )}
          >
            <ChevronLeft size={14} />
          </button>
          <span className="text-sm font-semibold text-foreground">
            {viewDate.getFullYear()}년 {viewDate.getMonth() + 1}월
          </span>
          <button
            type="button"
            onClick={handleNextMonth}
            disabled={!canNext}
            className={cn(
              "p-1 rounded hover:bg-muted transition cursor-pointer text-muted-foreground hover:text-foreground",
              !canNext && "opacity-30 cursor-default pointer-events-none",
            )}
          >
            <ChevronRight size={14} />
          </button>
        </div>
      )}

      {/* Interactive calendar */}
      <div
        className={cn(
          "cal-preview-wrapper",
          `cal-mode-${mode}`,
          !showModeToggle && "pointer-events-none select-none",
        )}
      >
        <DatePicker
          key={
            !showModeToggle
              ? `preview-${viewDate.getFullYear()}-${viewDate.getMonth()}`
              : selectingStart
                ? "selecting"
                : `${mode}-${committedStart?.toISOString()}-${committedEnd?.toISOString()}`
          }
          inline
          locale="ko"
          selectsRange
          startDate={activeStart}
          endDate={activeEnd}
          onChange={handleRangeChange}
          openToDate={!showModeToggle ? viewDate : openToDate}
          {...(registrationMaxDate ? { maxDate: registrationMaxDate } : {})}
          highlightDates={inactiveHighlight}
          calendarClassName="cal-preview"
        />
      </div>

      {/* Legend */}
      <div className="flex items-center gap-s4 mt-s3">
        <div className="flex items-center gap-s1">
          <div className="w-3 h-3 rounded-sm bg-primary" />
          <span className="typo-c1 text-muted-foreground">행사 기간</span>
        </div>
        <div className="flex items-center gap-s1">
          <div className="w-3 h-3 rounded-sm bg-brand-l2" />
          <span className="typo-c1 text-muted-foreground">신청 기간</span>
        </div>
      </div>
    </div>
  );
}
