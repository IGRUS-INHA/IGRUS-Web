import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { Clock } from "lucide-react";
import { REGISTRATION_PERIOD_PRESETS } from "@/constants/event";
import type { RegistrationPeriodPresetValue } from "@/constants/event";
import {
  formatDateLocal,
  formatTimeLocal,
  parseDateTimeString,
  extractDateFromPicker,
} from "@/utils/event";
import { cn } from "@/lib/utils";

interface RegistrationPeriodSelectorProps {
  preset: RegistrationPeriodPresetValue;
  registrationStartDate: string;
  registrationStartTime: string;
  registrationDeadlineDate: string;
  registrationDeadlineTime: string;
  onPresetChange: (value: RegistrationPeriodPresetValue) => void;
  onFieldChange: (field: string, value: string) => void;
  errors?: {
    registrationStartDate?: { message?: string };
    registrationStartTime?: { message?: string };
    registrationDeadlineDate?: { message?: string };
    registrationDeadlineTime?: { message?: string };
  };
}

export function RegistrationPeriodSelector({
  preset,
  registrationStartDate,
  registrationStartTime,
  registrationDeadlineDate,
  registrationDeadlineTime,
  onPresetChange,
  onFieldChange,
  errors,
}: RegistrationPeriodSelectorProps) {
  const pickerClass = (hasError?: boolean) =>
    cn(
      "w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm",
      "focus:outline-none focus:border-primary",
      hasError && "border-destructive",
    );

  const startSelected = parseDateTimeString(
    registrationStartDate,
    registrationStartTime,
  );
  const endSelected = parseDateTimeString(
    registrationDeadlineDate,
    registrationDeadlineTime,
  );
  const hasStartError = !!(
    errors?.registrationStartDate || errors?.registrationStartTime
  );
  const hasEndError = !!(
    errors?.registrationDeadlineDate || errors?.registrationDeadlineTime
  );

  return (
    <div className="px-s5 py-s5">
      <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
        <Clock size={14} /> 신청 기간
      </label>

      {/* 프리셋 라디오 카드 */}
      <div className="space-y-s2 mb-s3">
        {REGISTRATION_PERIOD_PRESETS.map((p) => (
          <button
            key={p.value}
            type="button"
            onClick={() =>
              onPresetChange(p.value as RegistrationPeriodPresetValue)
            }
            className={cn(
              "w-full rounded-r3 px-s4 py-s3 border text-left text-sm transition-colors cursor-pointer",
              preset === p.value
                ? "border-primary bg-primary/5"
                : "border-border bg-muted/50",
            )}
          >
            <div className="flex items-center gap-s3">
              <div
                className={cn(
                  "w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0",
                  preset === p.value
                    ? "border-primary"
                    : "border-muted-foreground/40",
                )}
              >
                {preset === p.value && (
                  <div className="w-2 h-2 rounded-full bg-primary" />
                )}
              </div>
              <div>
                <p className="font-medium">{p.label}</p>
                <p className="typo-c1 text-muted-foreground">{p.description}</p>
              </div>
            </div>
          </button>
        ))}
      </div>

      {/* 수동 입력 (custom 프리셋만) */}
      {preset === "custom" && (
        <div className="space-y-s3">
          {/* 신청 시작 */}
          <div>
            <p className="typo-c1 text-muted-foreground mb-s1">신청 시작</p>
            <DatePicker
              selected={startSelected}
              onChange={(value: Date | Date[] | null) => {
                const d = extractDateFromPicker(value);
                if (!d) return;
                onFieldChange("registrationStartDate", formatDateLocal(d));
                onFieldChange("registrationStartTime", formatTimeLocal(d));
              }}
              showTimeSelect
              timeFormat="HH:mm"
              timeIntervals={30}
              timeCaption="시간"
              dateFormat="yyyy년 MM월 dd일 HH:mm"
              locale="ko"
              placeholderText="신청 시작 날짜와 시간을 선택하세요"
              className={pickerClass(hasStartError)}
              wrapperClassName="w-full"
              calendarClassName="event-datepicker"
            />
            {errors?.registrationStartDate && (
              <p className="typo-c1 text-destructive mt-s1">
                {errors.registrationStartDate.message}
              </p>
            )}
            {errors?.registrationStartTime &&
              !errors?.registrationStartDate && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.registrationStartTime.message}
                </p>
              )}
          </div>

          {/* 신청 마감 */}
          <div>
            <p className="typo-c1 text-muted-foreground mb-s1">신청 마감</p>
            <DatePicker
              selected={endSelected}
              onChange={(value: Date | Date[] | null) => {
                const d = extractDateFromPicker(value);
                if (!d) return;
                onFieldChange("registrationDeadlineDate", formatDateLocal(d));
                onFieldChange("registrationDeadlineTime", formatTimeLocal(d));
              }}
              showTimeSelect
              timeFormat="HH:mm"
              timeIntervals={30}
              timeCaption="시간"
              dateFormat="yyyy년 MM월 dd일 HH:mm"
              locale="ko"
              placeholderText="신청 마감 날짜와 시간을 선택하세요"
              {...(startSelected ? { minDate: startSelected } : {})}
              className={pickerClass(hasEndError)}
              wrapperClassName="w-full"
              calendarClassName="event-datepicker"
            />
            {errors?.registrationDeadlineDate && (
              <p className="typo-c1 text-destructive mt-s1">
                {errors.registrationDeadlineDate.message}
              </p>
            )}
            {errors?.registrationDeadlineTime &&
              !errors?.registrationDeadlineDate && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.registrationDeadlineTime.message}
                </p>
              )}
          </div>
        </div>
      )}
    </div>
  );
}
