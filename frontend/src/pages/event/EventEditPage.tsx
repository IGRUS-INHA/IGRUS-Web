import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Save } from "lucide-react";
import { FullPageSpinner } from "@/components/ui";
import {
  EventFormFields,
  eventFormSchema,
  type EventFormValues,
} from "@/components/feature/event/EventFormFields";
import { useAdminEvent, useUpdateEvent } from "@/hooks/queries/useEvents";
import { useSurveyEdit } from "@/hooks/useSurveyEdit";
import { REGISTRATION_PERIOD_PRESETS } from "@/constants/event";
import { detectRegistrationPreset, formatDateLocal } from "@/utils/event";
import {
  getErrorMessage,
  isForbiddenError,
  isEventAccessDenied,
  isEventOperatorRequired,
} from "@/utils/error";
import { useImageUpload } from "@/hooks/useImageUpload";
import { useResolvedImageUrls } from "@/hooks/useResolvedImageUrls";
import { useToast } from "@/hooks/useToast";
import { IMAGE_UPLOAD_CONFIG } from "@/utils/upload";
import { UPLOAD_PURPOSE } from "@/services/uploadService";

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
  const [capacityRaw, setCapacityRaw] = useState("30");
  const [imagesInitialized, setImagesInitialized] = useState(false);

  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    files,
    addFiles,
    removeFile,
    reorderFiles,
    uploadAll,
    setExistingItems,
  } = useImageUpload({
    config: IMAGE_UPLOAD_CONFIG,
    purpose: UPLOAD_PURPOSE.EVENT_IMAGE,
    onValidationError: (errors) => {
      errors.forEach((msg) => toast.error(msg));
    },
  });

  const existingAttachments = useMemo(
    () => (event?.attachments ?? []).filter(Boolean),
    [event?.attachments],
  );
  const existingObjectKeys = useMemo(
    () => existingAttachments.map((a) => a.objectKey ?? "").filter(Boolean),
    [existingAttachments],
  );
  const { urls: resolvedUrls } = useResolvedImageUrls(existingObjectKeys);

  const existingSurveyId = event?.surveyId ?? undefined;
  const {
    draftQuestions,
    setDraftQuestions,
    submitSurvey,
    isQuestionsFetching,
  } = useSurveyEdit(existingSurveyId);

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<EventFormValues>({
    resolver: zodResolver(eventFormSchema),
    defaultValues: {
      capacity: 30,
      registrationPreset: "custom",
      location: "",
      registrationType: "AUTO_APPROVE",
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
  const allowExternal = watch("allowExternal");

  // 권한 체크
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

      const loadedCapacity = event.capacity || 30;

      reset({
        title: event.title || "",
        description: event.description || "",
        date: eventDateTime.date ?? "",
        time: eventDateTime.time ?? "",
        endDate: eventEndDateTime.date || eventDateTime.date || "",
        endTime: eventEndDateTime.time || eventDateTime.time || "",
        location: event.location || "",
        capacity: loadedCapacity,
        registrationType:
          (event.registrationType as "AUTO_APPROVE" | "MANUAL_APPROVE") ??
          "AUTO_APPROVE",
        registrationPreset: detectedPreset,
        registrationStartDate: regStartDateTime.date ?? "",
        registrationStartTime: regStartDateTime.time ?? "",
        registrationDeadlineDate: regEndDateTime.date ?? "",
        registrationDeadlineTime: regEndDateTime.time ?? "",
        allowExternal: event.allowExternal ?? false,
      });

      setCapacityRaw(String(loadedCapacity));
      isInitialized.current = true;
      setFormReady(true);
    }
  }, [event, reset]);

  // 기존 이미지 URL이 resolve되면 setExistingItems 호출
  useEffect(() => {
    if (imagesInitialized) return;
    if (existingAttachments.length === 0) return;
    if (resolvedUrls.size < existingObjectKeys.length) return;

    const items = existingAttachments.map((att) => ({
      objectKey: att.objectKey ?? "",
      previewUrl: resolvedUrls.get(att.objectKey ?? "") ?? att.objectKey ?? "",
    }));
    setExistingItems(items);
    setImagesInitialized(true);
  }, [
    existingAttachments,
    existingObjectKeys,
    resolvedUrls,
    setExistingItems,
    imagesInitialized,
  ]);

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

  const onSubmit = async (data: EventFormValues) => {
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

    let resolvedSurveyId: number | null = null;

    try {
      resolvedSurveyId =
        (await submitSurvey(data.title, data.allowExternal)) ?? null;
    } catch {
      alert("설문 처리에 실패했습니다. 다시 시도해주세요.");
      return;
    }

    let uploadResults;
    try {
      uploadResults = await uploadAll();
    } catch (uploadError) {
      alert(
        uploadError instanceof Error
          ? uploadError.message
          : "이미지 업로드에 실패했습니다.",
      );
      return;
    }
    const attachmentObjectKeys = uploadResults.map((r) => r.objectKey);

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
          surveyId: resolvedSurveyId,
          attachmentObjectKeys,
          allowExternal: data.allowExternal,
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
            disabled={isPending || isSubmitting || isQuestionsFetching}
            className="bg-primary text-primary-foreground px-s5 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition shadow-lg shadow-primary/20 flex items-center gap-s2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save size={16} />{" "}
            {isPending || isSubmitting || isQuestionsFetching
              ? "수정 중..."
              : "수정 완료"}
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
          allowExternal={allowExternal}
          draftQuestions={draftQuestions}
          onDraftQuestionsChange={setDraftQuestions}
          registrationTypeMode="readonly"
          editorReady={formReady}
          files={files}
          onAddFiles={addFiles}
          onRemoveFile={removeFile}
          onReorderFiles={reorderFiles}
          fileInputRef={fileInputRef}
        />
      </form>
    </div>
  );
}
