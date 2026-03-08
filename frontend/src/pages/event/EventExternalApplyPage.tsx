import { useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, Check, ChevronDown } from "lucide-react";
import { useRegisterEventExternal } from "@/api/model/event-external-registration/event-external-registration";
import { useGetAnonymousSurveyForm } from "@/api/model/survey-anonymous-form/survey-anonymous-form";
import type { QuestionResponse } from "@/api/model/models/questionResponse";
import type { SubmitAnswerRequest } from "@/api/model/models/submitAnswerRequest";
import { cn } from "@/lib/utils";
import {
  getErrorMessage,
  isConflictError,
  isNotFoundError,
} from "@/utils/error";

// ─── 답변 상태 타입 ──────────────────────────────────────────────────────────

interface AnswerState {
  textValue?: string;
  selectedOptionIds?: number[];
  numericValue?: number;
  gridAnswers?: Record<number, number[]>;
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
  const base: SubmitAnswerRequest = { questionId: question.id ?? 0 };
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
  answer,
  onChange,
  question,
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
  answer,
  onChange,
  question,
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
  answer,
  onChange,
  question,
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
  answer,
  onChange,
  question,
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

function GridRenderer({ answer, onChange, question }: QuestionRendererProps) {
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
                const isSelected = (gridAnswers[row.id ?? 0] ?? []).includes(
                  opt.id ?? 0,
                );
                return (
                  <td key={opt.id} className="py-s3 px-s3 text-center">
                    <button
                      type="button"
                      onClick={() => toggleCell(row.id ?? 0, opt.id ?? 0)}
                      className={cn(
                        "w-5 h-5 rounded border-2 transition-all cursor-pointer mx-auto block",
                        isCheckboxGrid ? "rounded" : "rounded-full",
                        isSelected
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

export default function EventExternalApplyPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const numericId = Number(eventId);

  const surveyIdParam = searchParams.get("surveyId");
  const surveyId = surveyIdParam ? Number(surveyIdParam) : undefined;

  // 신청자 정보는 이전 페이지(아코디언)에서 쿼리 파라미터로 전달
  const name = searchParams.get("name") ?? "";
  const studentId = searchParams.get("studentId") ?? "";
  const phone = searchParams.get("phone") ?? "";
  const department = searchParams.get("department") ?? "";

  const { data: surveyResponse, isLoading: isSurveyLoading } =
    useGetAnonymousSurveyForm(surveyId ?? 0, {
      query: { enabled: !!surveyId },
    });
  const survey = surveyResponse?.data;
  const questions = [...(survey?.questions ?? [])].sort(
    (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
  );

  const [answers, setAnswers] = useState<Record<number, AnswerState>>({});

  const { mutate: registerExternal, isPending } = useRegisterEventExternal({
    mutation: {
      onSuccess: () => {
        alert("행사 신청이 완료되었습니다.");
        navigate("/events");
      },
      onError: (error: unknown) => {
        if (isConflictError(error)) {
          alert("이미 신청한 행사입니다. (동일 학번 또는 연락처로 중복 신청)");
        } else if (isNotFoundError(error)) {
          alert("행사를 찾을 수 없습니다.");
        } else {
          alert(getErrorMessage(error));
        }
      },
    },
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (surveyId && questions.length > 0) {
      const unansweredRequired = questions.filter(
        (q) => q.required && !isAnswered(q, answers[q.id ?? 0] ?? {}),
      );
      if (unansweredRequired.length > 0) {
        alert(
          `다음 질문에 답변해 주세요: ${unansweredRequired.map((q) => q.title).join(", ")}`,
        );
        return;
      }
    }

    const surveyAnswers: SubmitAnswerRequest[] =
      surveyId && questions.length > 0
        ? questions.map((q) => buildSubmitAnswer(q, answers[q.id ?? 0] ?? {}))
        : [];

    registerExternal({
      eventId: numericId,
      data: {
        name,
        studentId,
        phone,
        department,
        surveyAnswers: surveyAnswers.length > 0 ? surveyAnswers : undefined,
      },
    });
  };

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300 max-w-2xl mx-auto">
      {/* 브레드크럼 */}
      <button
        type="button"
        onClick={() => navigate("/events")}
        className="flex items-center gap-s2 text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer mb-s6"
      >
        <ArrowLeft size={16} />
        행사 목록으로
      </button>

      {/* 헤더 */}
      <div className="mb-s6">
        <p className="text-xs font-bold text-primary tracking-widest mb-s1">
          EXTERNAL REGISTRATION
        </p>
        <h1 className="text-2xl font-bold mb-s2">외부인 행사 신청</h1>
        <p className="text-sm text-muted-foreground">
          비회원도 신청 가능한 행사입니다. 아래 정보를 입력하여 신청해 주세요.
        </p>
      </div>

      <form onSubmit={onSubmit} className="space-y-s6">
        {/* 설문 질문 섹션 */}
        {surveyId && (
          <>
            {isSurveyLoading ? (
              <div className="rounded-xl border border-border bg-card p-s8 text-center text-sm text-muted-foreground">
                설문을 불러오는 중...
              </div>
            ) : questions.length > 0 ? (
              <div className="space-y-s4">
                {questions.map((question, index) => (
                  <div
                    key={question.id}
                    className="rounded-xl border border-border bg-card p-s6 shadow-sm"
                  >
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
            ) : null}
          </>
        )}

        {/* 제출 버튼 */}
        <div className="pt-s2">
          <button
            type="submit"
            disabled={isPending}
            className="w-full py-s3 rounded-r4 font-bold bg-primary text-primary-foreground hover:bg-primary/90 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isPending ? "신청 중..." : "신청 제출"}
          </button>
          <p className="typo-c1 text-muted-foreground text-center mt-s3">
            학번 또는 연락처 기준으로 중복 신청이 방지됩니다.
          </p>
        </div>
      </form>
    </div>
  );
}
