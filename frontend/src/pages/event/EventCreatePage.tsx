import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Image as ImageIcon } from "lucide-react";
import {
  EventFormFields,
  eventFormSchema,
  type EventFormValues,
} from "@/components/feature/event/EventFormFields";
import { useCreateEvent } from "@/hooks/queries/useEvents";
import { useCreateSurvey } from "@/api/model/survey/survey";
import { useCreateQuestion } from "@/api/model/survey-question/survey-question";
import { useCreateOption } from "@/api/model/survey-question-option/survey-question-option";
import { CreateEventRequestRegistrationType } from "@/api/model/models";
import { REGISTRATION_PERIOD_PRESETS } from "@/constants/event";
import { formatDateLocal } from "@/utils/event";
import {
  isForbiddenError,
  isEventOperatorRequired,
  getErrorMessage,
} from "@/utils/error";
import { useImageUpload } from "@/hooks/useImageUpload";
import { useToast } from "@/hooks/useToast";
import { IMAGE_UPLOAD_CONFIG } from "@/utils/upload";
import type { DraftQuestion } from "@/components/feature/event/SurveyQuestionBuilder";

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
  } = useForm<EventFormValues>({
    resolver: zodResolver(eventFormSchema),
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

  const [capacityRaw, setCapacityRaw] = useState("30");
  const [draftQuestions, setDraftQuestions] = useState<DraftQuestion[]>([]);

  const { mutateAsync: createSurveyAsync } = useCreateSurvey();
  const { mutateAsync: createQuestionAsync } = useCreateQuestion();
  const { mutateAsync: createOptionAsync } = useCreateOption();

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

  const onSubmit = async (data: EventFormValues) => {
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
            const qRes = await createQuestionAsync({
              surveyId: newSurveyId,
              data: {
                questionType: q.questionType,
                title: q.title || "질문",
                required: q.required,
                displayOrder: q.displayOrder,
              },
            });
            const newQuestionId =
              qRes.status === 201 ? (qRes.data?.id ?? null) : null;
            if (newQuestionId && q.options?.length) {
              for (const [i, text] of q.options.entries()) {
                if (text.trim()) {
                  await createOptionAsync({
                    surveyId: newSurveyId,
                    questionId: newQuestionId,
                    data: { text: text.trim(), displayOrder: i + 1 },
                  });
                }
              }
            }
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

        <EventFormFields
          register={register}
          control={control}
          errors={errors}
          setValue={setValue}
          registrationType={registrationType}
          eventDate={eventDate}
          eventTime={eventTime}
          endDate={endDate}
          endTime={endTime}
          registrationPreset={registrationPreset}
          registrationStartDate={registrationStartDate}
          registrationStartTime={registrationStartTime}
          registrationDeadlineDate={registrationDeadlineDate}
          registrationDeadlineTime={registrationDeadlineTime}
          capacity={capacity}
          capacityRaw={capacityRaw}
          onCapacityRawChange={setCapacityRaw}
          draftQuestions={draftQuestions}
          onDraftQuestionsChange={setDraftQuestions}
          registrationTypeMode="editable"
        />

        {/* 이미지 업로드 (생성 전용) */}
        <div className="mt-s4">
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
