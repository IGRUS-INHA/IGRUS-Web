import DatePicker, { registerLocale } from 'react-datepicker';
import { ko } from 'date-fns/locale';
import 'react-datepicker/dist/react-datepicker.css';
import { formatDateLocal, formatTimeLocal, parseDateTimeString, extractDateFromPicker } from '@/utils/event';
import { cn } from '@/lib/utils';

registerLocale('ko', ko);

interface EventDateTimePickerProps {
  date: string;
  time: string;
  endDate: string;
  endTime: string;
  onDateChange: (date: string) => void;
  onTimeChange: (time: string) => void;
  onEndDateChange: (date: string) => void;
  onEndTimeChange: (time: string) => void;
  dateError?: string | undefined;
  timeError?: string | undefined;
  endDateError?: string | undefined;
  endTimeError?: string | undefined;
}

export function EventDateTimePicker({
  date,
  time,
  endDate,
  endTime,
  onDateChange,
  onTimeChange,
  onEndDateChange,
  onEndTimeChange,
  dateError,
  timeError,
  endDateError,
  endTimeError,
}: EventDateTimePickerProps) {
  const startSelected = parseDateTimeString(date, time);
  const endSelected = parseDateTimeString(endDate, endTime);

  const hasStartError = !!(dateError || timeError);
  const hasEndError = !!(endDateError || endTimeError);

  return (
    <div className="space-y-s4">
      {/* 시작 */}
      <div className="space-y-s2">
        <p className="text-xs font-medium text-muted-foreground">시작</p>
        <DatePicker
          selected={startSelected}
          onChange={(value: Date | Date[] | null) => {
            const d = extractDateFromPicker(value);
            if (!d) return;
            onDateChange(formatDateLocal(d));
            onTimeChange(formatTimeLocal(d));
            if (!endDate) {
              onEndDateChange(formatDateLocal(d));
            }
          }}
          showTimeSelect
          timeFormat="HH:mm"
          timeIntervals={30}
          timeCaption="시간"
          dateFormat="yyyy년 MM월 dd일 HH:mm"
          locale="ko"
          placeholderText="시작 날짜와 시간을 선택하세요"
          className={cn(
            'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
            'focus:outline-none focus:border-primary',
            hasStartError && 'border-destructive',
          )}
          wrapperClassName="w-full"
          calendarClassName="event-datepicker"
        />
        {dateError && <p className="typo-c1 text-destructive">{dateError}</p>}
        {timeError && !dateError && <p className="typo-c1 text-destructive">{timeError}</p>}
      </div>

      {/* 종료 */}
      <div className="space-y-s2">
        <p className="text-xs font-medium text-muted-foreground">종료</p>
        <DatePicker
          selected={endSelected}
          onChange={(value: Date | Date[] | null) => {
            const d = extractDateFromPicker(value);
            if (!d) return;
            onEndDateChange(formatDateLocal(d));
            onEndTimeChange(formatTimeLocal(d));
          }}
          showTimeSelect
          timeFormat="HH:mm"
          timeIntervals={30}
          timeCaption="시간"
          dateFormat="yyyy년 MM월 dd일 HH:mm"
          locale="ko"
          placeholderText="종료 날짜와 시간을 선택하세요"
          {...(startSelected ? { minDate: startSelected } : {})}
          className={cn(
            'w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm',
            'focus:outline-none focus:border-primary',
            hasEndError && 'border-destructive',
          )}
          wrapperClassName="w-full"
          calendarClassName="event-datepicker"
        />
        {endDateError && <p className="typo-c1 text-destructive">{endDateError}</p>}
        {endTimeError && !endDateError && <p className="typo-c1 text-destructive">{endTimeError}</p>}
      </div>
    </div>
  );
}
