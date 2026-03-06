import { useNavigate, useParams } from "react-router-dom";
import { FullPageSpinner } from "@/components/ui";
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
  Save,
  Minus,
  Plus,
} from "lucide-react";
import { WysiwygEditor } from "@/components/feature/editor";
import { RegistrationPeriodSelector } from "@/components/feature/event/RegistrationPeriodSelector";
import { useAdminEvent, useUpdateEvent } from "@/hooks/queries/useEvents";
import { EventDateTimePicker } from "@/components/feature/event/EventDateTimePicker";
import { EventCalendarPreview } from "@/components/feature/event/EventCalendarPreview";
import {
  SurveyQuestionBuilder,
  type DraftQuestion,
} from "@/components/feature/event/SurveyQuestionBuilder";
import {
  REGISTRATION_PERIOD_PRESETS,
  EVENT_LOCATIONS,
} from "@/constants/event";
import { detectRegistrationPreset, formatDateLocal } from "@/utils/event";
import { cn } from "@/lib/utils";
import { useEffect, useRef, useState } from "react";
import {
  getErrorMessage,
  isForbiddenError,
  isEventAccessDenied,
  isEventOperatorRequired,
} from "@/utils/error";
import { useCreateSurvey } from "@/api/model/survey/survey";
import {
  useCreateQuestion,
  useDeleteQuestion,
  useUpdateQuestion,
  useGetQuestionList,
} from "@/api/model/survey-question/survey-question";

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

export default function EventEditPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const {
    data: eventResponse,
    isLoading,
    error,
  } = useAdminEvent(Number(eventId));
  const { mutate: updateEvent, isPending } = useUpdateEvent();
  const event = eventResponse?.data;
  const isInitialized = useRef(false);
  const [formReady, setFormReady] = useState(false);
  const [showLocationDropdown, setShowLocationDropdown] = useState(false);
  const [draftQuestions, setDraftQuestions] = useState<DraftQuestion[]>([]);
  const locationRef = useRef<HTMLDivElement>(null);

  const existingSurveyId = event?.surveyId ?? undefined;
  const { data: questionListResponse } = useGetQuestionList(
    existingSurveyId ?? 0,
    { query: { enabled: !!existingSurveyId } },
  );
  const { mutateAsync: createSurveyAsync } = useCreateSurvey();
  const { mutateAsync: createQuestionAsync } = useCreateQuestion();
  const { mutateAsync: deleteQuestionAsync } = useDeleteQuestion();
  const { mutateAsync: updateQuestionAsync } = useUpdateQuestion();

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<EventForm>({
    resolver: zodResolver(eventSchema),
    defaultValues: {
      capacity: 30,
      registrationPreset: "custom",
      location: "",
    },
  });

  const eventTime = watch("time");
  const eventDate = watch("date");
  const endDate = watch("endDate");
  const endTime = watch("endTime");
  const registrationPreset = watch("registrationPreset");
  const registrationStartDate = watch("registrationStartDate");
  const registrationStartTime = watch("registrationStartTime");
  const registrationDeadlineDate = watch("registrationDeadlineDate");
  const registrationDeadlineTime = watch("registrationDeadlineTime");
  const capacity = watch("capacity");

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

  // 권한 체크 (권한 없으면 목록으로 리다이렉트)
  useEffect(() => {
    if (event && !event.canEdit) {
      navigate("/events");
    }
  }, [event, navigate]);

  // 기존 데이터로 폼 초기화
  useEffect(() => {
    if (event) {
      const parseDateTime = (isoString?: string) => {
        if (!isoString) return { date: "", time: "" };
        const d = new Date(isoString);
        if (isNaN(d.getTime())) return { date: "", time: "" };
        return {
          date: d.toISOString().split("T")[0],
          time: d.toTimeString().slice(0, 5),
        };
      };

      const eventDateTime = parseDateTime(event.eventStartAt);
      const eventEndDateTime = parseDateTime(event.eventEndAt);
      const regStartDateTime = parseDateTime(event.registrationStartAt);
      const regEndDateTime = parseDateTime(event.registrationEndAt);

      const detectedPreset = detectRegistrationPreset(
        eventDateTime.date ?? "",
        regStartDateTime.date ?? "",
        regStartDateTime.time ?? "",
        regEndDateTime.date ?? "",
        regEndDateTime.time ?? "",
        eventDateTime.time ?? "",
      );

      reset({
        title: event.title || "",
        description: event.description || "",
        date: eventDateTime.date ?? "",
        time: eventDateTime.time ?? "",
        endDate: eventEndDateTime.date || eventDateTime.date || "",
        endTime: eventEndDateTime.time || eventDateTime.time || "",
        location: event.location || "",
        capacity: event.capacity || 30,
        registrationPreset: detectedPreset,
        registrationStartDate: regStartDateTime.date ?? "",
        registrationStartTime: regStartDateTime.time ?? "",
        registrationDeadlineDate: regEndDateTime.date ?? "",
        registrationDeadlineTime: regEndDateTime.time ?? "",
      });

      isInitialized.current = true;
      setFormReady(true);
    }
  }, [event, reset]);

  // 기존 설문 문항 로드
  useEffect(() => {
    if (questionListResponse?.status === 200) {
      const existing = questionListResponse.data;
      setDraftQuestions(
        existing.map((q, i) => ({
          localId: String(q.id ?? i),
          serverId: q.id,
          questionType: q.questionType ?? "SHORT_ANSWER",
          title: q.title ?? "",
          required: q.required ?? false,
          displayOrder: q.displayOrder ?? i + 1,
        })),
      );
    }
  }, [questionListResponse]);

  // 신청 기간 자동 계산 (초기 로드 시에는 건너뜀)
  useEffect(() => {
    if (!isInitialized.current) return;
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

    setValue("registrationStartDate", formatDateLocal(startDate));
    setValue("registrationStartTime", preset.startTime);
    setValue("registrationDeadlineDate", formatDateLocal(endDateCalc));

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

  const onSubmit = async (data: EventForm) => {
    if (!eventId) return;

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

    let resolvedSurveyId: number | null = existingSurveyId ?? null;

    try {
      if (existingSurveyId) {
        // 기존 설문이 있는 경우: 삭제/수정/추가 diff 적용
        const originalIds = new Set(
          (questionListResponse?.status === 200
            ? questionListResponse.data
            : []
          ).map((q) => q.id),
        );
        const currentServerIds = new Set(
          draftQuestions.filter((q) => q.serverId).map((q) => q.serverId),
        );

        // 삭제된 문항 처리
        for (const id of originalIds) {
          if (id !== undefined && !currentServerIds.has(id)) {
            await deleteQuestionAsync({
              surveyId: existingSurveyId,
              questionId: id,
            });
          }
        }

        // 수정 및 추가 처리
        for (const q of draftQuestions) {
          if (q.serverId) {
            await updateQuestionAsync({
              surveyId: existingSurveyId,
              questionId: q.serverId,
              data: {
                questionType: q.questionType,
                title: q.title || "질문",
                required: q.required,
                displayOrder: q.displayOrder,
              },
            });
          } else {
            await createQuestionAsync({
              surveyId: existingSurveyId,
              data: {
                questionType: q.questionType,
                title: q.title || "질문",
                required: q.required,
                displayOrder: q.displayOrder,
              },
            });
          }
        }
      } else if (draftQuestions.length > 0) {
        // 새 설문 생성
        const surveyRes = await createSurveyAsync({
          data: {
            title: `${data.title} 신청 설문`,
            accessLevel: "MEMBER",
          },
        });
        const newSurveyId =
          surveyRes.status === 201 ? (surveyRes.data.id ?? null) : null;
        if (newSurveyId) {
          resolvedSurveyId = newSurveyId;
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
      }
    } catch {
      alert("설문 처리에 실패했습니다. 다시 시도해주세요.");
      return;
    }

    updateEvent(
      {
        eventId: Number(eventId),
        data: {
          title: data.title,
          description: data.description,
          location: data.location,
          eventStartAt,
          eventEndAt,
          registrationStartAt,
          registrationEndAt,
          capacity: data.capacity,
          surveyId: draftQuestions.length > 0 ? resolvedSurveyId : null,
        },
      },
      {
        onSuccess: () => {
          alert("행사가 수정되었습니다.");
          navigate("/events");
        },
        onError: (error: unknown) => {
          if (isForbiddenError(error) || isEventOperatorRequired(error)) {
            alert("행사 수정 권한이 없습니다.");
          } else {
            alert(getErrorMessage(error));
          }
        },
      },
    );
  };

  if (isLoading) {
    return <FullPageSpinner />;
  }

  const isForbidden = isForbiddenError(error) || isEventAccessDenied(error);

  if (isForbidden) {
    return (
      <div className="text-center py-12 space-y-s4">
        <p className="text-muted-foreground">
          정회원 승인 후 행사 조회가 가능합니다.
        </p>
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="text-sm text-primary hover:underline cursor-pointer"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">행사를 찾을 수 없습니다.</p>
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="mt-s4 text-primary hover:underline cursor-pointer"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

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
          <button
            type="submit"
            disabled={isPending}
            className="bg-primary text-primary-foreground px-s5 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save size={16} /> {isPending ? "수정 중..." : "수정 완료"}
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
                    onClick={() =>
                      setValue("capacity", Math.max(1, capacity - 1))
                    }
                    className="w-8 h-8 rounded-full border border-border bg-muted/50 flex items-center justify-center hover:bg-muted transition cursor-pointer"
                  >
                    <Minus size={14} />
                  </button>
                  <input
                    type="number"
                    min={1}
                    value={capacity}
                    onChange={(e) => {
                      const v = parseInt(e.target.value, 10);
                      if (!isNaN(v)) setValue("capacity", v);
                    }}
                    className="w-12 text-center text-sm font-semibold bg-transparent border-none focus:outline-none focus:ring-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                  />
                  <button
                    type="button"
                    onClick={() => setValue("capacity", capacity + 1)}
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

          {/* 신청 방식 (읽기 전용) */}
          <div className="rounded-r4 border bg-card border-border shadow-sm">
            <div className="px-s5 py-s3 border-b border-border">
              <p className="typo-label text-muted-foreground flex items-center gap-s2">
                <CheckSquare size={14} /> 신청 방식
              </p>
            </div>
            <div className="px-s5 py-s4 space-y-s2 pointer-events-none opacity-60">
              <div
                className={cn(
                  "w-full rounded-r3 px-s4 py-s3 border text-sm",
                  event.registrationType === "AUTO_APPROVE"
                    ? "border-primary bg-primary/5"
                    : "border-border bg-muted/50",
                )}
              >
                <div className="flex items-center gap-s3">
                  <div
                    className={cn(
                      "w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0",
                      event.registrationType === "AUTO_APPROVE"
                        ? "border-primary"
                        : "border-muted-foreground/40",
                    )}
                  >
                    {event.registrationType === "AUTO_APPROVE" && (
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
              </div>
              <div
                className={cn(
                  "w-full rounded-r3 px-s4 py-s3 border text-sm",
                  event.registrationType === "MANUAL_APPROVE"
                    ? "border-primary bg-primary/5"
                    : "border-border bg-muted/50",
                )}
              >
                <div className="flex items-center gap-s3">
                  <div
                    className={cn(
                      "w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0",
                      event.registrationType === "MANUAL_APPROVE"
                        ? "border-primary"
                        : "border-muted-foreground/40",
                    )}
                  >
                    {event.registrationType === "MANUAL_APPROVE" && (
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
              </div>
            </div>
            <p className="typo-c1 text-muted-foreground px-s5 pb-s4">
              생성 후 변경할 수 없습니다
            </p>
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
                  setValue(field as keyof EventForm, value)
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
            {formReady && (
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
            )}
            {errors.description && (
              <p className="typo-c1 text-destructive px-s6 pb-s2">
                {errors.description.message}
              </p>
            )}
          </div>
        </div>
      </form>
    </div>
  );
}
