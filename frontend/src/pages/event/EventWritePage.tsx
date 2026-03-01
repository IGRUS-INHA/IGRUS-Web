import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  ArrowLeft,
  Calendar,
  MapPin,
  Users,
  Save,
  Image as ImageIcon,
  ListChecks,
} from "lucide-react";
import { WysiwygEditor } from "@/components/feature/editor";
import { ImagePreviewList } from "@/components/feature/upload";
import {
  LocationSelector,
  DIRECT_INPUT_VALUE,
} from "@/components/feature/event/LocationSelector";
import { RegistrationPeriodSelector } from "@/components/feature/event/RegistrationPeriodSelector";
import { useCreateEvent } from "@/hooks/queries/useEvents";
import { CreateEventRequestRegistrationType } from "@/api/model/models";
import { EventDateTimePicker } from "@/components/feature/event/EventDateTimePicker";
import { EventCalendarPreview } from "@/components/feature/event/EventCalendarPreview";
import { REGISTRATION_PERIOD_PRESETS } from "@/constants/event";

import { combineLocation, formatDateLocal } from "@/utils/event";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/stores";
import {
  isForbiddenError,
  isEventOperatorRequired,
  getErrorMessage,
} from "@/utils/error";
import { useImageUpload } from "@/hooks/useImageUpload";
import { useToast } from "@/hooks/useToast";
import { IMAGE_UPLOAD_CONFIG } from "@/utils/upload";

const eventSchema = z
  .object({
    title: z.string().min(1, "행사 제목을 입력하세요"),
    description: z.string().min(1, "행사 설명을 입력하세요"),
    date: z.string().min(1, "행사 시작 날짜를 선택하세요"),
    time: z.string().min(1, "행사 시작 시간을 선택하세요"),
    endDate: z.string().min(1, "행사 종료 날짜를 선택하세요"),
    endTime: z.string().min(1, "행사 종료 시간을 선택하세요"),
    locationPreset: z.string(),
    locationDetail: z.string().optional(),
    capacity: z.number().min(1, "최대 인원은 1명 이상이어야 합니다"),
    registrationType: z.enum(["AUTO_APPROVE", "MANUAL_APPROVE"]),
    registrationPreset: z.enum(["default", "short", "custom"]),
    registrationStartDate: z.string().min(1, "신청 시작일을 선택하세요"),
    registrationStartTime: z.string().min(1, "신청 시작 시간을 선택하세요"),
    registrationDeadlineDate: z.string().min(1, "신청 마감일을 선택하세요"),
    registrationDeadlineTime: z.string().min(1, "신청 마감 시간을 선택하세요"),
  })
  .refine(
    (data) => {
      if (data.locationPreset && data.locationPreset !== DIRECT_INPUT_VALUE)
        return true;
      if (
        data.locationPreset === DIRECT_INPUT_VALUE &&
        data.locationDetail &&
        data.locationDetail.trim().length > 0
      )
        return true;
      return false;
    },
    { message: "장소를 입력하세요", path: ["locationPreset"] },
  )
  .refine(
    (data) => {
      if (
        !data.registrationDeadlineDate ||
        !data.registrationDeadlineTime ||
        !data.date ||
        !data.time
      )
        return true;
      const regEnd = new Date(
        `${data.registrationDeadlineDate}T${data.registrationDeadlineTime}:00`,
      );
      const eventStart = new Date(`${data.date}T${data.time}:00`);
      return regEnd <= eventStart;
    },
    {
      message: "신청 마감은 행사 시작 이전이어야 합니다",
      path: ["registrationDeadlineDate"],
    },
  );

type EventForm = z.infer<typeof eventSchema>;

export default function EventWritePage() {
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === "dark";
  const { mutate: createEvent, isPending } = useCreateEvent();
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 이미지 업로드 (UI만 — 백엔드 CreateEventRequest에 이미지 필드 없음)
  const { files, addFiles, removeFile } = useImageUpload({
    config: IMAGE_UPLOAD_CONFIG,
    onValidationError: (errors) => {
      errors.forEach((msg) => toast.error(msg));
    },
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<EventForm>({
    resolver: zodResolver(eventSchema),
    defaultValues: {
      capacity: 30,
      registrationType: "AUTO_APPROVE",
      registrationPreset: "default",
      locationPreset: "",
      locationDetail: "",
    },
  });

  const registrationType = watch("registrationType");
  const eventDate = watch("date");
  const eventTime = watch("time");
  const endDate = watch("endDate");
  const endTime = watch("endTime");
  const registrationPreset = watch("registrationPreset");
  const locationPreset = watch("locationPreset");
  const locationDetail = watch("locationDetail") ?? "";
  const registrationStartDate = watch("registrationStartDate");
  const registrationStartTime = watch("registrationStartTime");
  const registrationDeadlineDate = watch("registrationDeadlineDate");
  const registrationDeadlineTime = watch("registrationDeadlineTime");

  // 신청 기간 자동 계산
  useEffect(() => {
    if (!eventDate || registrationPreset === "custom") return;
    const preset = REGISTRATION_PERIOD_PRESETS.find(
      (p) => p.value === registrationPreset,
    );
    if (!preset || preset.value === "custom") return;

    const dateParts = eventDate.split("-").map(Number);
    const year = dateParts[0] ?? 0;
    const month = dateParts[1] ?? 1;
    const day = dateParts[2] ?? 1;
    const startDate = new Date(year, month - 1, day - preset.startDaysBefore);
    const endDate = new Date(year, month - 1, day - preset.endDaysBefore);

    setValue("registrationStartDate", formatDateLocal(startDate));
    setValue("registrationStartTime", preset.startTime);
    setValue("registrationDeadlineDate", formatDateLocal(endDate));

    // 동적 마감 시간: endTimeOffsetHours가 있고 행사 시간이 설정된 경우
    if ("endTimeOffsetHours" in preset && eventTime) {
      const tp = eventTime.split(":").map(Number);
      const totalMinutes = Math.max(
        0,
        Math.min(
          23 * 60 + 59,
          (tp[0] ?? 0) * 60 + (tp[1] ?? 0) + preset.endTimeOffsetHours * 60,
        ),
      );
      const endH = String(Math.floor(totalMinutes / 60)).padStart(2, "0");
      const endM = String(totalMinutes % 60).padStart(2, "0");
      setValue("registrationDeadlineTime", `${endH}:${endM}`);
    } else {
      setValue("registrationDeadlineTime", preset.endTime);
    }
  }, [eventDate, eventTime, registrationPreset, setValue]);

  const onSubmit = (data: EventForm) => {
    const location = combineLocation(
      data.locationPreset === DIRECT_INPUT_VALUE ? "" : data.locationPreset,
      data.locationDetail ?? "",
    );

    const eventStartAt = new Date(`${data.date}T${data.time}:00`).toISOString();
    const eventEndAt = new Date(
      `${data.endDate}T${data.endTime}:00`,
    ).toISOString();
    const registrationStartAt = new Date(
      `${data.registrationStartDate}T${data.registrationStartTime}:00`,
    ).toISOString();
    const registrationEndAt = new Date(
      `${data.registrationDeadlineDate}T${data.registrationDeadlineTime}:00`,
    ).toISOString();

    createEvent(
      {
        data: {
          title: data.title,
          description: data.description,
          location,
          eventStartAt,
          eventEndAt,
          registrationStartAt,
          registrationEndAt,
          capacity: data.capacity,
          registrationType:
            data.registrationType as CreateEventRequestRegistrationType,
        },
      },
      {
        onSuccess: () => {
          alert("행사가 등록되었습니다.");
          navigate("/events");
        },
        onError: (error: unknown) => {
          if (isForbiddenError(error) || isEventOperatorRequired(error)) {
            alert("행사 등록 권한이 없습니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    );
  };

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      <form onSubmit={handleSubmit(onSubmit)}>
        {/* Sticky Top Bar */}
        <div className="flex justify-between items-center mb-s6 sticky top-0 z-10 py-s4 backdrop-blur-md bg-background/80">
          <button
            type="button"
            onClick={() => navigate("/events")}
            className="flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
          >
            <ArrowLeft size={18} /> 취소
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="bg-primary text-primary-foreground px-s6 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save size={16} /> {isPending ? "등록 중..." : "행사 등록"}
          </button>
        </div>

        {/* 2-Column Layout */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-s6">
          {/* Left Column - Title + MDEditor + Bottom Toolbar */}
          <div
            className={cn(
              "md:col-span-2 rounded-r4 border shadow-sm flex flex-col",
              isDark ? "bg-card border-border" : "bg-card border-border",
            )}
          >
            {/* 행사 제목 */}
            <div className="px-s6 py-s5 border-b border-border">
              <input
                type="text"
                {...register("title")}
                className={cn(
                  "w-full text-2xl font-bold bg-transparent border-none focus:outline-none focus:ring-0 placeholder:text-muted-foreground/50",
                  errors.title && "border-b-2 border-b-destructive",
                )}
                placeholder="행사 제목을 입력하세요"
              />
              {errors.title && (
                <p className="typo-c1 text-destructive mt-s2">
                  {errors.title.message}
                </p>
              )}
            </div>

            {/* WYSIWYG Editor */}
            <div className="flex-1">
              <Controller
                name="description"
                control={control}
                render={({ field }) => (
                  <WysiwygEditor
                    value={field.value ?? ""}
                    onChange={field.onChange}
                    hasError={!!errors.description}
                    className="border-0 rounded-none"
                  />
                )}
              />
              {errors.description && (
                <p className="typo-c1 text-destructive px-s6 pb-s2">
                  {errors.description.message}
                </p>
              )}
            </div>

            {/* Image Preview */}
            {files.length > 0 && (
              <div className="px-s6 pb-s4">
                <ImagePreviewList files={files} onRemove={removeFile} />
              </div>
            )}

            {/* Bottom Toolbar */}
            <div className="px-s6 py-s5 border-t border-border flex items-center">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className={cn(
                  "p-2 rounded-lg transition cursor-pointer",
                  isDark
                    ? "text-gray-400 hover:bg-white/10"
                    : "text-gray-500 hover:bg-gray-100",
                )}
              >
                <ImageIcon size={20} />
              </button>
              {files.length > 0 && (
                <span className="ml-auto text-xs text-muted-foreground">
                  이미지 {files.length}/{IMAGE_UPLOAD_CONFIG.maxFiles}
                </span>
              )}
            </div>
          </div>

          {/* Right Column - Location / Capacity / Registration Type / Registration Period / Event Period / Calendar */}
          <div className="rounded-r4 border bg-card border-border shadow-sm">
            {/* 장소 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <MapPin size={14} /> 장소
              </label>
              <LocationSelector
                selectedPreset={locationPreset}
                detail={locationDetail}
                onPresetChange={(v) => setValue("locationPreset", v)}
                onDetailChange={(v) => setValue("locationDetail", v)}
                error={errors.locationPreset?.message}
              />
            </div>

            {/* 최대 인원 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Users size={14} /> 최대 인원
              </label>
              <input
                type="number"
                {...register("capacity", { valueAsNumber: true })}
                className={cn(
                  "w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm",
                  "focus:outline-none focus:border-primary",
                  errors.capacity && "border-destructive",
                )}
              />
              {errors.capacity && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.capacity.message}
                </p>
              )}
            </div>

            {/* 신청 방식 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <ListChecks size={14} /> 신청 방식
              </label>
              <div className="space-y-s2">
                <button
                  type="button"
                  onClick={() => setValue("registrationType", "AUTO_APPROVE")}
                  className={cn(
                    "w-full rounded-r3 px-s4 py-s3 border text-left text-sm transition-colors cursor-pointer",
                    registrationType === "AUTO_APPROVE"
                      ? "border-primary bg-primary/5"
                      : "border-border bg-muted/50",
                  )}
                >
                  <div className="flex items-center gap-s3">
                    <div
                      className={cn(
                        "w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0",
                        registrationType === "AUTO_APPROVE"
                          ? "border-primary"
                          : "border-muted-foreground/40",
                      )}
                    >
                      {registrationType === "AUTO_APPROVE" && (
                        <div className="w-2 h-2 rounded-full bg-primary" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium">선착순 (자동 승인)</p>
                      <p className="typo-c1 text-muted-foreground">
                        신청 즉시 승인됩니다
                      </p>
                    </div>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => setValue("registrationType", "MANUAL_APPROVE")}
                  className={cn(
                    "w-full rounded-r3 px-s4 py-s3 border text-left text-sm transition-colors cursor-pointer",
                    registrationType === "MANUAL_APPROVE"
                      ? "border-primary bg-primary/5"
                      : "border-border bg-muted/50",
                  )}
                >
                  <div className="flex items-center gap-s3">
                    <div
                      className={cn(
                        "w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0",
                        registrationType === "MANUAL_APPROVE"
                          ? "border-primary"
                          : "border-muted-foreground/40",
                      )}
                    >
                      {registrationType === "MANUAL_APPROVE" && (
                        <div className="w-2 h-2 rounded-full bg-primary" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium">선발제 (수동 승인)</p>
                      <p className="typo-c1 text-muted-foreground">
                        관리자가 승인해야 합니다
                      </p>
                    </div>
                  </div>
                </button>
              </div>
            </div>

            {/* 신청 기간 */}
            <div className="border-b border-border">
              <RegistrationPeriodSelector
                preset={registrationPreset}
                registrationStartDate={registrationStartDate}
                registrationStartTime={registrationStartTime}
                registrationDeadlineDate={registrationDeadlineDate}
                registrationDeadlineTime={registrationDeadlineTime}
                onPresetChange={(v) => setValue("registrationPreset", v)}
                onFieldChange={(field, value) =>
                  setValue(field as keyof EventForm, value)
                }
                errors={errors}
              />
            </div>

            {/* 행사 기간 */}
            <div className="px-s5 py-s5 border-b border-border">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Calendar size={14} /> 행사 기간
              </label>
              <EventDateTimePicker
                date={eventDate}
                time={eventTime}
                endDate={endDate}
                endTime={endTime}
                onDateChange={(v) => {
                  setValue("date", v);
                  if (endDate && v > endDate) setValue("endDate", v);
                }}
                onTimeChange={(v) => setValue("time", v)}
                onEndDateChange={(v) => {
                  setValue("endDate", v);
                  if (eventDate && v < eventDate) setValue("date", v);
                }}
                onEndTimeChange={(v) => setValue("endTime", v)}
                dateError={errors.date?.message}
                timeError={errors.time?.message}
                endDateError={errors.endDate?.message}
                endTimeError={errors.endTime?.message}
              />
            </div>

            {/* 캘린더 */}
            <EventCalendarPreview
              eventStartDate={eventDate}
              eventEndDate={endDate}
              registrationStartDate={registrationStartDate}
              registrationEndDate={registrationDeadlineDate}
              onEventDateChange={(start, end) => {
                setValue("date", start);
                setValue("endDate", end);
              }}
              onRegistrationDateChange={(start, end) => {
                setValue("registrationStartDate", start);
                setValue("registrationDeadlineDate", end);
              }}
              onRegistrationPresetChange={() =>
                setValue("registrationPreset", "custom")
              }
            />
          </div>
        </div>
      </form>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0) {
            addFiles(e.target.files);
            e.target.value = "";
          }
        }}
        className="hidden"
      />
    </div>
  );
}
