import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Calendar,
  MapPin,
  Users,
  Clock,
  CheckCircle,
  Circle,
  ArrowLeft,
  RotateCcw,
  ArrowRight,
  Check,
  ChevronDown,
} from "lucide-react";
import { FullPageSpinner } from "@/components/ui";
import { useEvent } from "@/hooks/queries/useEvents";
import { useGetSurveyDetail } from "@/api/model/survey/survey";
import { useRegisterEvent } from "@/api/model/event-registration/event-registration";
import type { QuestionResponse } from "@/api/model/models/questionResponse";
import type { SubmitAnswerRequest } from "@/api/model/models/submitAnswerRequest";
import { cn } from "@/lib/utils";
import {
  getErrorMessage,
  isEventAlreadyRegistered,
  isEventCapacityFull,
  isEventRegistrationClosed,
  isForbiddenError,
} from "@/utils/error";
import { useQueryClient } from "@tanstack/react-query";
import { eventKeys, adminEventKeys } from "@/hooks/queries/useEvents";
import { myPageKeys } from "@/hooks/queries/useMyPage";

// ─── 날짜 포맷 헬퍼 ──────────────────────────────────────────────────────────

function formatDate(isoString?: string): string {
  if (!isoString) return "-";
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString("ko-KR", {
      month: "long",
      day: "numeric",
      weekday: "short",
    });
  } catch {
    return isoString;
  }
}

function formatDateTime(isoString?: string): string {
  if (!isoString) return "-";
  try {
    const d = new Date(isoString);
    return `${d.getMonth() + 1}월 ${d.getDate()}일 ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  } catch {
    return isoString;
  }
}

// ─── 답변 상태 타입 ──────────────────────────────────────────────────────────

interface AnswerState {
  textValue?: string;
  selectedOptionIds?: number[];
  numericValue?: number;
  gridAnswers?: Record<number, number[]>; // rowId -> selectedOptionIds
}

function isAnswered(question: QuestionResponse, answer: AnswerState): boolean {
  const type = question.questionType;
  if (
    type === "SHORT_ANSWER" ||
    type === "PARAGRAPH" ||
    type === "DATE" ||
    type === "TIME"
  ) {
    return !!(answer.textValue && answer.textValue.trim().length > 0);
  }
  if (
    type === "MULTIPLE_CHOICE" ||
    type === "DROPDOWN" ||
    type === "CHECKBOX"
  ) {
    return !!(answer.selectedOptionIds && answer.selectedOptionIds.length > 0);
  }
  if (type === "LINEAR_SCALE") {
    return answer.numericValue !== undefined;
  }
  if (type === "MULTIPLE_CHOICE_GRID" || type === "CHECKBOX_GRID") {
    const rowCount = question.rows?.length ?? 0;
    if (!answer.gridAnswers || rowCount === 0) return false;
    return Object.keys(answer.gridAnswers).length === rowCount;
  }
  return false;
}

function buildSubmitAnswer(
  question: QuestionResponse,
  answer: AnswerState,
): SubmitAnswerRequest {
  const base: SubmitAnswerRequest = { questionId: question.id! };
  const type = question.questionType;

  if (
    type === "SHORT_ANSWER" ||
    type === "PARAGRAPH" ||
    type === "DATE" ||
    type === "TIME"
  ) {
    return { ...base, textValue: answer.textValue };
  }
  if (
    type === "MULTIPLE_CHOICE" ||
    type === "CHECKBOX" ||
    type === "DROPDOWN"
  ) {
    return { ...base, selectedOptionIds: answer.selectedOptionIds };
  }
  if (type === "LINEAR_SCALE") {
    return { ...base, numericValue: answer.numericValue };
  }
  if (type === "MULTIPLE_CHOICE_GRID" || type === "CHECKBOX_GRID") {
    const gridAnswers = Object.entries(answer.gridAnswers ?? {}).map(
      ([rowId, selectedOptionIds]) => ({
        rowId: Number(rowId),
        selectedOptionIds,
      }),
    );
    return { ...base, gridAnswers };
  }
  return base;
}

// ─── 질문 렌더러 ─────────────────────────────────────────────────────────────

interface QuestionRendererProps {
  question: QuestionResponse;
  answer: AnswerState;
  onChange: (answer: AnswerState) => void;
}

function ShortAnswerRenderer({
  question,
  answer,
  onChange,
}: QuestionRendererProps) {
  return (
    <input
      type={
        question.questionType === "DATE"
          ? "date"
          : question.questionType === "TIME"
            ? "time"
            : "text"
      }
      value={answer.textValue ?? ""}
      onChange={(e) => onChange({ ...answer, textValue: e.target.value })}
      placeholder={question.description ?? "답변을 입력하세요"}
      className="w-full rounded-lg px-s4 py-s3 border border-border bg-muted/50 text-sm focus:outline-none focus:border-primary transition-colors"
    />
  );
}

function ParagraphRenderer({
  question,
  answer,
  onChange,
}: QuestionRendererProps) {
  const text = answer.textValue ?? "";
  return (
    <div>
      <textarea
        value={text}
        onChange={(e) => onChange({ ...answer, textValue: e.target.value })}
        placeholder={question.description ?? "자유롭게 작성해 주세요"}
        rows={5}
        maxLength={500}
        className="w-full rounded-lg px-s4 py-s3 border border-border bg-muted/50 text-sm focus:outline-none focus:border-primary transition-colors resize-y"
      />
      <p className="text-right typo-c1 text-muted-foreground mt-s1">
        {text.length} / 500
      </p>
    </div>
  );
}

function MultipleChoiceRenderer({
  question,
  answer,
  onChange,
}: QuestionRendererProps) {
  const selected = answer.selectedOptionIds?.[0];
  const options = question.options ?? [];
  return (
    <div className="space-y-s3">
      {options.map((opt) => {
        const isSelected = selected === opt.id;
        return (
          <button
            key={opt.id}
            type="button"
            onClick={() =>
              onChange({
                ...answer,
                selectedOptionIds: isSelected ? [] : [opt.id ?? 0],
              })
            }
            className="flex items-center gap-s3 w-full text-left cursor-pointer"
          >
            <div
              className={cn(
                "w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0",
                isSelected ? "border-primary" : "border-muted-foreground/50",
              )}
            >
              {isSelected && (
                <div className="w-2.5 h-2.5 rounded-full bg-primary" />
              )}
            </div>
            <span className="text-sm text-foreground">{opt.text}</span>
          </button>
        );
      })}
    </div>
  );
}

function CheckboxRenderer({
  question,
  answer,
  onChange,
}: QuestionRendererProps) {
  const selected = new Set(answer.selectedOptionIds ?? []);
  const options = question.options ?? [];
  const toggle = (id: number) => {
    const next = new Set(selected);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    onChange({ ...answer, selectedOptionIds: Array.from(next) });
  };
  return (
    <div className="space-y-s3">
      {options.map((opt) => {
        const isSelected = selected.has(opt.id ?? 0);
        return (
          <button
            key={opt.id}
            type="button"
            onClick={() => toggle(opt.id ?? 0)}
            className="flex items-center gap-s3 w-full text-left cursor-pointer"
          >
            <div
              className={cn(
                "w-5 h-5 rounded-sm border-2 flex items-center justify-center shrink-0",
                isSelected
                  ? "border-primary bg-primary"
                  : "border-muted-foreground/50",
              )}
            >
              {isSelected && (
                <Check size={12} className="text-primary-foreground" />
              )}
            </div>
            <span className="text-sm text-foreground">{opt.text}</span>
          </button>
        );
      })}
    </div>
  );
}

function DropdownRenderer({
  question,
  answer,
  onChange,
}: QuestionRendererProps) {
  const options = question.options ?? [];
  return (
    <div className="relative inline-block min-w-40">
      <select
        value={answer.selectedOptionIds?.[0] ?? ""}
        onChange={(e) =>
          onChange({
            ...answer,
            selectedOptionIds: e.target.value ? [Number(e.target.value)] : [],
          })
        }
        className="appearance-none w-full rounded-lg px-s4 py-s3 pr-10 border border-border bg-background text-sm focus:outline-none focus:border-primary transition-colors cursor-pointer"
      >
        <option value="">선택</option>
        {options.map((opt) => (
          <option key={opt.id} value={opt.id}>
            {opt.text}
          </option>
        ))}
      </select>
      <ChevronDown
        size={16}
        className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
      />
    </div>
  );
}

function LinearScaleRenderer({
  question,
  answer,
  onChange,
}: QuestionRendererProps) {
  const min = question.scaleMin ?? 1;
  const max = question.scaleMax ?? 5;
  const steps = Array.from({ length: max - min + 1 }, (_, i) => min + i);
  return (
    <div className="flex items-center gap-s2 flex-wrap">
      {steps.map((val) => {
        const isSelected = answer.numericValue === val;
        return (
          <button
            key={val}
            type="button"
            onClick={() => onChange({ ...answer, numericValue: val })}
            className={cn(
              "w-10 h-10 rounded-full border-2 text-sm font-medium transition-all cursor-pointer",
              isSelected
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border bg-background hover:border-primary/50",
            )}
          >
            {val}
          </button>
        );
      })}
    </div>
  );
}

function GridRenderer({ question, answer, onChange }: QuestionRendererProps) {
  const rows = question.rows ?? [];
  const options = question.options ?? [];
  const isCheckboxGrid = question.questionType === "CHECKBOX_GRID";
  const gridAnswers = answer.gridAnswers ?? {};

  const toggleCell = (rowId: number, optId: number) => {
    const current = gridAnswers[rowId] ?? [];
    let next: number[];
    if (isCheckboxGrid) {
      next = current.includes(optId)
        ? current.filter((id) => id !== optId)
        : [...current, optId];
    } else {
      next = [optId];
    }
    onChange({ ...answer, gridAnswers: { ...gridAnswers, [rowId]: next } });
  };

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr>
            <th className="text-left py-s2 pr-s4 text-muted-foreground font-medium" />
            {options.map((opt) => (
              <th
                key={opt.id}
                className="py-s2 px-s3 text-center text-muted-foreground font-medium"
              >
                {opt.text}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className="border-t border-border">
              <td className="py-s3 pr-s4 text-foreground">{row.label}</td>
              {options.map((opt) => {
                const selected = (gridAnswers[row.id!] ?? []).includes(opt.id!);
                return (
                  <td key={opt.id} className="py-s3 px-s3 text-center">
                    <button
                      type="button"
                      onClick={() => toggleCell(row.id!, opt.id!)}
                      className={cn(
                        "w-5 h-5 rounded border-2 transition-all cursor-pointer mx-auto block",
                        isCheckboxGrid ? "rounded" : "rounded-full",
                        selected
                          ? "border-primary bg-primary"
                          : "border-border bg-background hover:border-primary/50",
                      )}
                    />
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function QuestionRenderer(props: QuestionRendererProps) {
  const { question } = props;
  switch (question.questionType) {
    case "SHORT_ANSWER":
    case "DATE":
    case "TIME":
      return <ShortAnswerRenderer {...props} />;
    case "PARAGRAPH":
      return <ParagraphRenderer {...props} />;
    case "MULTIPLE_CHOICE":
      return <MultipleChoiceRenderer {...props} />;
    case "CHECKBOX":
      return <CheckboxRenderer {...props} />;
    case "DROPDOWN":
      return <DropdownRenderer {...props} />;
    case "LINEAR_SCALE":
      return <LinearScaleRenderer {...props} />;
    case "MULTIPLE_CHOICE_GRID":
    case "CHECKBOX_GRID":
      return <GridRenderer {...props} />;
    default:
      return (
        <p className="text-muted-foreground text-sm">
          지원하지 않는 질문 형식입니다.
        </p>
      );
  }
}

// ─── 메인 페이지 ─────────────────────────────────────────────────────────────

export default function EventApplyPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const numericId = Number(eventId);

  const { data: eventResponse, isLoading: isEventLoading } =
    useEvent(numericId);
  const event = eventResponse?.data;

  const surveyId = event?.surveyId ?? undefined;

  const { data: surveyResponse, isLoading: isSurveyLoading } =
    useGetSurveyDetail(surveyId!, { query: { enabled: !!surveyId } });
  const survey = surveyResponse?.data;
  const questions = [...(survey?.questions ?? [])].sort(
    (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
  );

  const [answers, setAnswers] = useState<Record<number, AnswerState>>({});

  const { mutate: register, isPending } = useRegisterEvent({
    mutation: {
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: eventKeys.lists() });
        void queryClient.invalidateQueries({
          queryKey: eventKeys.detail(numericId),
        });
        void queryClient.invalidateQueries({
          queryKey: adminEventKeys.lists(),
        });
        void queryClient.invalidateQueries({
          queryKey: adminEventKeys.detail(numericId),
        });
        void queryClient.invalidateQueries({
          queryKey: myPageKeys.registrations(),
        });
        alert("행사 신청이 완료되었습니다.");
        navigate("/events");
      },
      onError: (error: unknown) => {
        if (isEventAlreadyRegistered(error)) {
          alert("이미 신청한 행사입니다.");
        } else if (isEventCapacityFull(error)) {
          alert("정원이 마감되었습니다.");
        } else if (isEventRegistrationClosed(error)) {
          alert("신청 기간이 종료되었습니다.");
        } else if (isForbiddenError(error)) {
          alert("정회원 이상만 신청 가능합니다.");
        } else {
          alert(getErrorMessage(error));
        }
      },
    },
  });

  const handleReset = () => {
    setAnswers({});
  };

  const handleSubmit = () => {
    // 필수 질문 미응답 체크
    const unansweredRequired = questions.filter(
      (q) => q.required && !isAnswered(q, answers[q.id ?? 0] ?? {}),
    );
    if (unansweredRequired.length > 0) {
      alert(
        `다음 질문에 답변해 주세요: ${unansweredRequired.map((q) => q.title).join(", ")}`,
      );
      return;
    }

    const surveyAnswers: SubmitAnswerRequest[] = questions.map((q) =>
      buildSubmitAnswer(q, answers[q.id ?? 0] ?? {}),
    );

    register({
      eventId: numericId,
      data: { surveyAnswers },
    });
  };

  if (isEventLoading || (surveyId && isSurveyLoading)) {
    return <FullPageSpinner />;
  }

  if (!event) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">행사를 찾을 수 없습니다.</p>
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="mt-s4 text-primary hover:underline cursor-pointer text-sm"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  if (!surveyId) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">
          설문이 연결되지 않은 행사입니다.
        </p>
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="mt-s4 text-primary hover:underline cursor-pointer text-sm"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300 max-w-5xl mx-auto">
      {/* 브레드크럼 */}
      <button
        type="button"
        onClick={() => navigate("/events")}
        className="flex items-center gap-s2 text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer mb-s6"
      >
        <ArrowLeft size={16} />
        행사 목록으로
      </button>

      {/* 상단: 이벤트 헤더 + 진행 상태 사이드바 */}
      <div className="flex flex-col lg:flex-row gap-s4 mb-s6">
        {/* 이벤트 헤더 카드 */}
        <div className="flex-1 rounded-xl border border-border bg-card p-s6 shadow-sm">
          <div className="flex items-center gap-s2 mb-s3">
            <span className="text-xs font-semibold px-s3 py-s1 rounded-full bg-primary/10 text-primary">
              ● 모집 중
            </span>
          </div>
          <h1 className="typo-h2 font-bold mb-s5">{event.title}</h1>
          <div className="space-y-s3 text-sm text-muted-foreground">
            <div className="flex items-start gap-s3">
              <Calendar size={16} className="mt-0.5 shrink-0 text-primary/70" />
              <div>
                <p className="text-muted-foreground text-xs mb-0.5">일정</p>
                <p className="text-foreground font-medium">
                  {formatDate(event.eventStartAt)}
                  {event.eventEndAt && event.eventEndAt !== event.eventStartAt
                    ? ` – ${formatDate(event.eventEndAt)}`
                    : ""}
                </p>
              </div>
            </div>
            {event.location && (
              <div className="flex items-start gap-s3">
                <MapPin size={16} className="mt-0.5 shrink-0 text-primary/70" />
                <div>
                  <p className="text-muted-foreground text-xs mb-0.5">장소</p>
                  <p className="text-foreground font-medium">
                    {event.location}
                  </p>
                </div>
              </div>
            )}
            {event.registrationEndAt && (
              <div className="flex items-start gap-s3">
                <Clock size={16} className="mt-0.5 shrink-0 text-primary/70" />
                <div>
                  <p className="text-muted-foreground text-xs mb-0.5">
                    신청 마감
                  </p>
                  <p className="text-foreground font-medium">
                    {formatDateTime(event.registrationEndAt)}
                  </p>
                </div>
              </div>
            )}
            {event.capacity && (
              <div className="flex items-start gap-s3">
                <Users size={16} className="mt-0.5 shrink-0 text-primary/70" />
                <div>
                  <p className="text-muted-foreground text-xs mb-0.5">
                    모집 규모
                  </p>
                  <p className="text-foreground font-medium">
                    최대 {event.capacity}명
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* 진행 상태 사이드바 */}
        <div className="lg:w-52 rounded-xl border border-border bg-card p-s5 shadow-sm">
          <p className="typo-label font-semibold mb-s4">진행 상태</p>
          <div className="space-y-s3">
            {questions.map((q) => {
              const answered = isAnswered(q, answers[q.id ?? 0] ?? {});
              return (
                <div key={q.id} className="flex items-center gap-s2">
                  {answered ? (
                    <CheckCircle size={16} className="text-primary shrink-0" />
                  ) : (
                    <Circle
                      size={16}
                      className="text-muted-foreground/40 shrink-0"
                    />
                  )}
                  <span
                    className={cn(
                      "text-xs truncate",
                      answered ? "text-foreground" : "text-muted-foreground",
                    )}
                  >
                    {q.title ?? `질문 ${q.displayOrder}`}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 설문 질문 섹션들 */}
      {questions.length === 0 ? (
        <div className="rounded-xl border border-border bg-card p-s8 text-center text-muted-foreground text-sm">
          설문 질문이 없습니다.
        </div>
      ) : (
        <div className="space-y-s4">
          {questions.map((question, index) => (
            <div
              key={question.id}
              className="rounded-xl border border-border bg-card p-s6 shadow-sm"
            >
              {/* 질문 헤더 */}
              <div className="flex items-start gap-s3 mb-s5">
                <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
                  <span className="text-primary-foreground text-sm font-bold">
                    {index + 1}
                  </span>
                </div>
                <div>
                  <h3 className="font-semibold text-foreground">
                    {question.title}
                    {question.required && (
                      <span className="text-destructive ml-s1">*</span>
                    )}
                  </h3>
                  {question.description && (
                    <p className="typo-c1 text-muted-foreground mt-s1">
                      {question.description}
                    </p>
                  )}
                </div>
              </div>

              {/* 질문 입력 UI */}
              <QuestionRenderer
                question={question}
                answer={answers[question.id ?? 0] ?? {}}
                onChange={(newAnswer) =>
                  setAnswers((prev) => ({
                    ...prev,
                    [question.id ?? 0]: newAnswer,
                  }))
                }
              />
            </div>
          ))}
        </div>
      )}

      {/* 하단 버튼 */}
      <div className="flex items-center justify-between mt-s8 pt-s6 border-t border-border">
        <button
          type="button"
          onClick={handleReset}
          className="flex items-center gap-s2 px-s5 py-s3 rounded-full border border-border text-sm text-muted-foreground hover:text-foreground hover:border-foreground/30 transition-colors cursor-pointer"
        >
          <RotateCcw size={14} />
          초기화
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={isPending}
          className="flex items-center gap-s2 px-s7 py-s3 rounded-full bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? "신청 중..." : "신청 제출"}
          {!isPending && <ArrowRight size={16} />}
        </button>
      </div>
    </div>
  );
}
