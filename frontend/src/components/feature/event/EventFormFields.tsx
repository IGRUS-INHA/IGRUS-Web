import { useEffect, useRef, useState } from "react";
import type { RefObject } from "react";
import { ImageLightbox } from "@/components/ui";
import {
  Control,
  Controller,
  FieldErrors,
  UseFormRegister,
  UseFormSetValue,
} from "react-hook-form";
import { z } from "zod";
import {
  Calendar,
  MapPin,
  Users,
  CheckSquare,
  FileText,
  Image as ImageIcon,
  Minus,
  Plus,
  UserCheck,
  Check,
  Star,
} from "lucide-react";
import type { UploadFile } from "@/types/upload";
import { WysiwygEditor } from "@/components/feature/editor";
import { RegistrationPeriodSelector } from "@/components/feature/event/RegistrationPeriodSelector";
import { EventDateTimePicker } from "@/components/feature/event/EventDateTimePicker";
import { EventCalendarPreview } from "@/components/feature/event/EventCalendarPreview";
import {
  SurveyQuestionBuilder,
  type DraftQuestion,
} from "@/components/feature/event/SurveyQuestionBuilder";
import { EVENT_LOCATIONS } from "@/constants/event";
import { cn } from "@/lib/utils";

export const eventFormSchema = z
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
    allowExternal: z.boolean(),
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

export type EventFormValues = z.infer<typeof eventFormSchema>;

interface EventFormFieldsProps {
  register: UseFormRegister<EventFormValues>;
  control: Control<EventFormValues>;
  errors: FieldErrors<EventFormValues>;
  setValue: UseFormSetValue<EventFormValues>;
  registrationType: "AUTO_APPROVE" | "MANUAL_APPROVE";
  eventDate: string;
  eventTime: string;
  endDate: string;
  endTime: string;
  registrationPreset: "default" | "short" | "custom";
  registrationStartDate: string;
  registrationStartTime: string;
  registrationDeadlineDate: string;
  registrationDeadlineTime: string;
  capacity: number;
  capacityRaw: string;
  onCapacityRawChange: (v: string) => void;
  allowExternal: boolean;
  draftQuestions: DraftQuestion[];
  onDraftQuestionsChange: (q: DraftQuestion[]) => void;
  registrationTypeMode: "editable" | "readonly";
  editorReady?: boolean;
  files: UploadFile[];
  onAddFiles: (files: FileList) => void;
  onRemoveFile: (id: string) => void;
  onReorderFiles: (fromIndex: number, toIndex: number) => void;
  fileInputRef: RefObject<HTMLInputElement | null>;
}

export function EventFormFields({
  register,
  control,
  errors,
  setValue,
  registrationType,
  eventDate,
  eventTime,
  endDate,
  endTime,
  registrationPreset,
  registrationStartDate,
  registrationStartTime,
  registrationDeadlineDate,
  registrationDeadlineTime,
  capacity,
  capacityRaw,
  onCapacityRawChange,
  allowExternal,
  draftQuestions,
  onDraftQuestionsChange,
  registrationTypeMode,
  editorReady = true,
  files,
  onAddFiles,
  onRemoveFile,
  onReorderFiles,
  fileInputRef,
}: EventFormFieldsProps) {
  const [showLocationDropdown, setShowLocationDropdown] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const locationRef = useRef<HTMLDivElement>(null);

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

  return (
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
                  onCapacityRawChange(String(newVal));
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
                  onCapacityRawChange(raw);
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
                  onCapacityRawChange(String(newVal));
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
        <div className="border-t border-border px-s5 py-s4">
          <label className="typo-c1 text-muted-foreground mb-s2 block flex items-center gap-s1">
            <UserCheck size={12} /> 외부인 신청
          </label>
          <button
            type="button"
            onClick={() => setValue("allowExternal", !allowExternal)}
            className={cn(
              "w-full rounded-r3 px-s4 py-s3 border text-left text-sm transition-colors cursor-pointer",
              allowExternal
                ? "border-brand-l4 bg-brand-l4/30"
                : "border-border bg-muted/50",
            )}
          >
            <div className="flex items-center gap-s3">
              <div
                className={cn(
                  "w-4 h-4 rounded border-2 flex items-center justify-center shrink-0",
                  allowExternal
                    ? "border-brand-l4 bg-brand-l4"
                    : "border-muted-foreground/40",
                )}
              >
                {allowExternal && (
                  <Check size={10} className="text-primary-foreground" />
                )}
              </div>
              <div>
                <p className="font-medium">비회원 신청 허용</p>
                <p className="typo-c1 text-muted-foreground">
                  아이그루스 회원이 아니어도 신청 가능합니다
                </p>
              </div>
            </div>
          </button>
        </div>
      </div>

      {/* 신청 방식 */}
      <div className="rounded-r4 border bg-card border-border shadow-sm">
        <div className="px-s5 py-s3 border-b border-border">
          <p className="typo-label text-muted-foreground flex items-center gap-s2">
            <CheckSquare size={14} /> 신청 방식
          </p>
        </div>
        {registrationTypeMode === "editable" ? (
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
        ) : (
          <>
            <div className="px-s5 py-s4 space-y-s2 pointer-events-none opacity-60">
              <div
                className={cn(
                  "w-full rounded-r3 px-s4 py-s3 border text-sm",
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
              </div>
              <div
                className={cn(
                  "w-full rounded-r3 px-s4 py-s3 border text-sm",
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
              </div>
            </div>
            <p className="typo-c1 text-muted-foreground px-s5 pb-s4">
              생성 후 변경할 수 없습니다
            </p>
          </>
        )}
      </div>

      {/* 신청 기간 + 행사 기간 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-s4 items-start">
        <div className="rounded-r4 border bg-card border-border shadow-sm">
          <RegistrationPeriodSelector
            preset={registrationPreset}
            registrationStartDate={registrationStartDate}
            registrationStartTime={registrationStartTime}
            registrationDeadlineDate={registrationDeadlineDate}
            registrationDeadlineTime={registrationDeadlineTime}
            onPresetChange={(v) => setValue("registrationPreset", v)}
            onFieldChange={(field, value) =>
              setValue(field as keyof EventFormValues, value, {
                shouldValidate: true,
              })
            }
            errors={errors}
          />
        </div>

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
            onTimeChange={(v) => setValue("time", v, { shouldValidate: true })}
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
      <div className="rounded-r4 border bg-card border-border shadow-sm">
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
        onChange={onDraftQuestionsChange}
      />

      {/* 행사 내용 (에디터) */}
      <div className="rounded-r4 border bg-card border-border shadow-sm">
        <label className="flex items-center gap-s2 typo-label text-muted-foreground px-s5 pt-s4 pb-s3">
          <FileText size={14} /> 행사 세부 내용
        </label>
        {editorReady && (
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

      {/* 이미지 업로드 */}
      <div className="space-y-s3">
        {files.length > 0 ? (
          <div className="flex flex-wrap gap-s3">
            {files.map((file, idx) => (
              <div key={file.id} className="relative group w-48 h-48 shrink-0">
                <button
                  type="button"
                  onClick={() => setLightboxIndex(idx)}
                  className="w-full h-full cursor-pointer"
                >
                  <img
                    src={file.previewUrl}
                    alt={file.file.name}
                    className="w-full h-full object-cover rounded-r3 border border-border"
                  />
                </button>
                {idx === 0 ? (
                  <div className="absolute bottom-s1 left-s1 flex items-center gap-0.5 bg-black/60 text-white rounded-r2 px-s2 py-0.5 text-xs pointer-events-none">
                    <Star
                      size={10}
                      className="fill-yellow-400 text-yellow-400"
                    />
                    대표 이미지
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => onReorderFiles(idx, 0)}
                    className="absolute bottom-s1 left-s1 flex items-center gap-0.5 bg-black/60 text-white rounded-r2 px-s2 py-0.5 text-xs opacity-0 group-hover:opacity-100 transition cursor-pointer hover:bg-black/80"
                  >
                    <Star size={10} />
                    대표로 설정
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => onRemoveFile(file.id)}
                  className="absolute top-s1 right-s1 w-5 h-5 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition cursor-pointer text-xs leading-none"
                >
                  ×
                </button>
              </div>
            ))}
            <button
              type="button"
              className="w-48 h-48 shrink-0 rounded-r3 border-2 border-dashed border-border flex items-center justify-center cursor-pointer hover:border-primary/50 hover:bg-primary/5 transition-colors"
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                if (e.dataTransfer.files.length > 0) {
                  onAddFiles(e.dataTransfer.files);
                }
              }}
            >
              <Plus size={24} className="text-muted-foreground/50" />
            </button>
          </div>
        ) : (
          <div
            className="rounded-r4 border-2 border-dashed border-border bg-card shadow-sm px-s6 py-s8 flex flex-col items-center justify-center gap-s3 cursor-pointer hover:border-primary/50 hover:bg-primary/5 transition-colors"
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              if (e.dataTransfer.files.length > 0) {
                onAddFiles(e.dataTransfer.files);
              }
            }}
          >
            <ImageIcon size={32} className="text-muted-foreground/50" />
            <p className="text-sm font-medium text-muted-foreground">
              클릭하여 이미지 업로드
            </p>
            <p className="typo-c1 text-muted-foreground/70">
              JPG, PNG, GIF, WebP · 최대 10MB
            </p>
            <p className="typo-c1 text-muted-foreground/50 flex items-center gap-s1">
              <Star size={10} />첫 번째 이미지가 목록 썸네일로 사용됩니다
            </p>
          </div>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          onChange={(e) => {
            if (e.target.files && e.target.files.length > 0) {
              onAddFiles(e.target.files);
              e.target.value = "";
            }
          }}
          className="hidden"
        />
      </div>
      {lightboxIndex !== null && (
        <ImageLightbox
          images={files.map((f) => f.previewUrl)}
          initialIndex={lightboxIndex}
          onClose={() => setLightboxIndex(null)}
        />
      )}
    </div>
  );
}
