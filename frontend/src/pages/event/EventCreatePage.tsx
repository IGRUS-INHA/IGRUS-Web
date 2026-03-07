import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  ArrowLeft,
  Calendar,
  MapPin,
  Users,
  CheckSquare,
  FileText,
  Image as ImageIcon,
  Minus,
  Plus,
} from "lucide-react";
import { WysiwygEditor } from "@/components/feature/editor";
import { RegistrationPeriodSelector } from "@/components/feature/event/RegistrationPeriodSelector";
import { EventDateTimePicker } from "@/components/feature/event/EventDateTimePicker";
import { EventCalendarPreview } from "@/components/feature/event/EventCalendarPreview";
import {
  SurveyQuestionBuilder,
  type DraftQuestion,
} from "@/components/feature/event/SurveyQuestionBuilder";
import { useCreateEvent } from "@/hooks/queries/useEvents";
import { useCreateSurvey } from "@/api/model/survey/survey";
import { useCreateQuestion } from "@/api/model/survey-question/survey-question";
import { CreateEventRequestRegistrationType } from "@/api/model/models";
import {
  REGISTRATION_PERIOD_PRESETS,
  EVENT_LOCATIONS,
} from "@/constants/event";
import { formatDateLocal } from "@/utils/event";
import { cn } from "@/lib/utils";
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
    location: z.string().min(1, "장소를 입력하세요"),
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

const TODAY = new Date().toLocaleDateString("ko-KR", {
  year: "numeric",
  month: "long",
  day: "numeric",
});

export default function EventCreatePage() {
  const navigate = useNavigate();
  const { mutate: createEvent, isPending } = useCreateEvent();
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

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
      title: "",
      description: "",
      date: "",
      time: "",
      endDate: "",
      endTime: "",
      location: "",
      capacity: 30,
      registrationType: "AUTO_APPROVE",
      registrationPreset: "default",
      registrationStartDate: "",
      registrationStartTime: "",
      registrationDeadlineDate: "",
      registrationDeadlineTime: "",
    },
  });

  const registrationType = watch("registrationType");
  const eventDate = watch("date");
  const eventTime = watch("time");
  const endDate = watch("endDate");
  const endTime = watch("endTime");
  const registrationPreset = watch("registrationPreset");
  const registrationStartDate = watch("registrationStartDate");
  const registrationStartTime = watch("registrationStartTime");
  const registrationDeadlineDate = watch("registrationDeadlineDate");
  const registrationDeadlineTime = watch("registrationDeadlineTime");
  const capacity = watch("capacity");

  const [showLocationDropdown, setShowLocationDropdown] = useState(false);
  const [capacityRaw, setCapacityRaw] = useState("30");
  const [draftQuestions, setDraftQuestions] = useState<DraftQuestion[]>([]);
  const locationRef = useRef<HTMLDivElement>(null);

  const { mutateAsync: createSurveyAsync } = useCreateSurvey();
  const { mutateAsync: createQuestionAsync } = useCreateQuestion();

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        locationRef.current &&
        !locationRef.current.contains(e.target as Node)
      ) {
        setShowLocationDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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
    const endDateCalc = new Date(year, month - 1, day - preset.endDaysBefore);

    setValue("registrationStartDate", formatDateLocal(startDate), {
      shouldValidate: true,
    });
    setValue("registrationStartTime", preset.startTime, {
      shouldValidate: true,
    });
    setValue("registrationDeadlineDate", formatDateLocal(endDateCalc), {
      shouldValidate: true,
    });

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
      setValue("registrationDeadlineTime", `${endH}:${endM}`, {
        shouldValidate: true,
      });
    } else {
      setValue("registrationDeadlineTime", preset.endTime, {
        shouldValidate: true,
      });
    }
  }, [eventDate, eventTime, registrationPreset, setValue]);

  const onSubmit = async (data: EventForm) => {
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

    let surveyId: number | null = null;

    if (draftQuestions.length > 0) {
      try {
        const surveyRes = await createSurveyAsync({
          data: {
            title: `${data.title} 신청 설문`,
            accessLevel: "MEMBER",
          },
        });
        const newSurveyId =
          surveyRes.status === 201 ? (surveyRes.data.id ?? null) : null;
        if (newSurveyId) {
          surveyId = newSurveyId;
          for (const q of draftQuestions) {
            await createQuestionAsync({
              surveyId: newSurveyId,
              data: {
                questionType: q.questionType,
                title: q.title || "질문",
                required: q.required,
                displayOrder: q.displayOrder,
              },
            });
          }
        }
      } catch {
        alert("설문 생성에 실패했습니다. 다시 시도해주세요.");
        return;
      }
    }

    createEvent(
      {
        data: {
          title: data.title,
          description: data.description,
          location: data.location,
          eventStartAt,
          eventEndAt,
          registrationStartAt,
          registrationEndAt,
          capacity: data.capacity,
          registrationType:
            data.registrationType as CreateEventRequestRegistrationType,
          surveyId,
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
    <div className="animate-in slide-in-from-bottom-8 duration-300 max-w-2xl mx-auto">
      <form onSubmit={handleSubmit(onSubmit)}>
        {/* Sticky Top Bar */}
        <div className="flex justify-between items-center mb-s5 sticky top-0 z-10 py-s4 backdrop-blur-md bg-background/80">
          <button
            type="button"
            onClick={() => navigate("/events")}
            className="flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
          >
            <ArrowLeft size={18} /> 취소
          </button>
          <div className="text-center">
            <p className="typo-c1 text-muted-foreground">{TODAY}</p>
          </div>
          <button
            type="submit"
            disabled={isPending}
            className="bg-primary text-primary-foreground px-s5 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isPending ? "등록 중..." : "행사 등록"}
          </button>
        </div>

        <div className="space-y-s4">
          {/* 제목 */}
          <div className="rounded-r4 border bg-card border-border shadow-sm px-s6 py-s5">
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

          {/* 기본 정보 */}
          <div className="rounded-r4 border bg-card border-border shadow-sm">
            <div className="px-s5 py-s3 border-b border-border">
              <p className="typo-label text-muted-foreground flex items-center gap-s2">
                <MapPin size={14} /> 기본 정보
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-border">
              <div className="px-s5 py-s4">
                <label className="typo-c1 text-muted-foreground mb-s2 block flex items-center gap-s1">
                  <MapPin size={12} /> 장소
                </label>
                <div ref={locationRef} className="relative">
                  <input
                    type="text"
                    {...register("location")}
                    onFocus={() => setShowLocationDropdown(true)}
                    className={cn(
                      "w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm",
                      "focus:outline-none focus:border-primary",
                      errors.location && "border-destructive",
                    )}
                    placeholder="장소를 입력하세요"
                  />
                  {showLocationDropdown && (
                    <ul className="absolute z-50 w-full mt-1 max-h-48 overflow-y-auto rounded-r2 border border-border bg-card shadow-md">
                      {EVENT_LOCATIONS.map((location) => (
                        <li
                          key={location}
                          onMouseDown={() => {
                            setValue("location", location);
                            setShowLocationDropdown(false);
                          }}
                          className="px-s4 py-s2 text-sm cursor-pointer hover:bg-muted"
                        >
                          {location}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
                {errors.location && (
                  <p className="typo-c1 text-destructive mt-s1">
                    {errors.location.message}
                  </p>
                )}
              </div>
              <div className="px-s5 py-s4">
                <label className="typo-c1 text-muted-foreground mb-s3 block flex items-center gap-s1">
                  <Users size={12} /> 최대 인원
                </label>
                <div className="flex items-center justify-center gap-s3">
                  <button
                    type="button"
                    onClick={() => {
                      const newVal = Math.max(1, capacity - 1);
                      setValue("capacity", newVal);
                      setCapacityRaw(String(newVal));
                    }}
                    className="w-8 h-8 rounded-full border border-border bg-muted/50 flex items-center justify-center hover:bg-muted transition cursor-pointer"
                  >
                    <Minus size={14} />
                  </button>
                  <input
                    type="number"
                    min={1}
                    value={capacityRaw}
                    onChange={(e) => {
                      const raw = e.target.value;
                      setCapacityRaw(raw);
                      if (raw === "") {
                        setValue("capacity", 0);
                      } else {
                        const v = parseInt(raw, 10);
                        if (!isNaN(v)) setValue("capacity", v);
                      }
                    }}
                    className="w-12 text-center text-sm font-semibold bg-transparent border-none focus:outline-none focus:ring-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      const newVal = capacity + 1;
                      setValue("capacity", newVal);
                      setCapacityRaw(String(newVal));
                    }}
                    className="w-8 h-8 rounded-full border border-border bg-muted/50 flex items-center justify-center hover:bg-muted transition cursor-pointer"
                  >
                    <Plus size={14} />
                  </button>
                </div>
                {errors.capacity && (
                  <p className="typo-c1 text-destructive mt-s1">
                    {errors.capacity.message}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* 신청 방식 */}
          <div className="rounded-r4 border bg-card border-border shadow-sm">
            <div className="px-s5 py-s3 border-b border-border">
              <p className="typo-label text-muted-foreground flex items-center gap-s2">
                <CheckSquare size={14} /> 신청 방식
              </p>
            </div>
            <div className="px-s5 py-s4 space-y-s2">
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
                    <p className="font-medium">자동 승인</p>
                    <p className="typo-c1 text-muted-foreground">
                      선착순으로 신청 즉시 자동 승인됩니다
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
                    <p className="font-medium">수동 승인</p>
                    <p className="typo-c1 text-muted-foreground">
                      관리자가 직접 신청자를 검토하고 승인합니다
                    </p>
                  </div>
                </div>
              </button>
            </div>
          </div>

          {/* 신청 기간 + 행사 기간 */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-s4 items-start">
            {/* 신청 기간 */}
            <div className="rounded-r4 border bg-card border-border shadow-sm">
              <RegistrationPeriodSelector
                preset={registrationPreset}
                registrationStartDate={registrationStartDate}
                registrationStartTime={registrationStartTime}
                registrationDeadlineDate={registrationDeadlineDate}
                registrationDeadlineTime={registrationDeadlineTime}
                onPresetChange={(v) => setValue("registrationPreset", v)}
                onFieldChange={(field, value) =>
                  setValue(field as keyof EventForm, value, {
                    shouldValidate: true,
                  })
                }
                errors={errors}
              />
            </div>

            {/* 행사 기간 */}
            <div className="rounded-r4 border bg-card border-border shadow-sm px-s5 py-s5">
              <label className="flex items-center gap-s2 typo-label text-muted-foreground mb-s3">
                <Calendar size={14} /> 행사 기간
              </label>
              <EventDateTimePicker
                date={eventDate}
                time={eventTime}
                endDate={endDate}
                endTime={endTime}
                onDateChange={(v) => {
                  setValue("date", v, { shouldValidate: true });
                  if (endDate && v > endDate)
                    setValue("endDate", v, { shouldValidate: true });
                }}
                onTimeChange={(v) =>
                  setValue("time", v, { shouldValidate: true })
                }
                onEndDateChange={(v) => {
                  setValue("endDate", v, { shouldValidate: true });
                  if (eventDate && v < eventDate)
                    setValue("date", v, { shouldValidate: true });
                }}
                onEndTimeChange={(v) =>
                  setValue("endTime", v, { shouldValidate: true })
                }
                dateError={errors.date?.message}
                timeError={errors.time?.message}
                endDateError={errors.endDate?.message}
                endTimeError={errors.endTime?.message}
              />
            </div>
          </div>

          {/* 캘린더 */}
          <div className="rounded-r4 border bg-card border-border shadow-sm pointer-events-none select-none">
            <EventCalendarPreview
              eventStartDate={eventDate}
              eventEndDate={endDate}
              registrationStartDate={registrationStartDate}
              registrationEndDate={registrationDeadlineDate}
              showModeToggle={false}
            />
          </div>

          {/* 신청 설문 문항 */}
          <SurveyQuestionBuilder
            questions={draftQuestions}
            onChange={setDraftQuestions}
          />

          {/* 행사 내용 (에디터) */}
          <div className="rounded-r4 border bg-card border-border shadow-sm">
            <label className="flex items-center gap-s2 typo-label text-muted-foreground px-s5 pt-s4 pb-s3">
              <FileText size={14} /> 행사 세부 내용
            </label>
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

          {/* 이미지 업로드 */}
          <div
            className="rounded-r4 border-2 border-dashed border-border bg-card shadow-sm px-s6 py-s8 flex flex-col items-center justify-center gap-s3 cursor-pointer hover:border-primary/50 hover:bg-primary/5 transition-colors"
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              if (e.dataTransfer.files.length > 0) {
                addFiles(e.dataTransfer.files);
              }
            }}
          >
            {files.length > 0 ? (
              <div className="w-full space-y-s2">
                {files.map((file) => (
                  <div
                    key={file.id}
                    className="flex items-center justify-between text-sm"
                  >
                    <span className="text-foreground truncate max-w-[80%]">
                      {file.file.name}
                    </span>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        removeFile(file.id);
                      }}
                      className="text-muted-foreground hover:text-destructive transition cursor-pointer text-xs ml-s2"
                    >
                      삭제
                    </button>
                  </div>
                ))}
                <p className="text-xs text-muted-foreground text-center pt-s2">
                  클릭하여 이미지 추가 · {files.length}/
                  {IMAGE_UPLOAD_CONFIG.maxFiles}
                </p>
              </div>
            ) : (
              <>
                <ImageIcon size={32} className="text-muted-foreground/50" />
                <p className="text-sm font-medium text-muted-foreground">
                  클릭하여 이미지 업로드
                </p>
                <p className="typo-c1 text-muted-foreground/70">
                  JPG, PNG, GIF, WebP · 최대 10MB
                </p>
              </>
            )}
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
