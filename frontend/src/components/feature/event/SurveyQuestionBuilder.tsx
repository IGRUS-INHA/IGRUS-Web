import { ClipboardList, Copy, Plus, Trash2 } from "lucide-react";
import { CreateQuestionRequestQuestionType } from "@/api/model/models";
import { cn } from "@/lib/utils";

export interface DraftQuestion {
  localId: string;
  serverId?: number;
  questionType: CreateQuestionRequestQuestionType;
  title: string;
  required: boolean;
  displayOrder: number;
}

const QUESTION_TYPES: {
  value: CreateQuestionRequestQuestionType;
  label: string;
}[] = [
  { value: "SHORT_ANSWER", label: "단답형" },
  { value: "PARAGRAPH", label: "장문형" },
  { value: "MULTIPLE_CHOICE", label: "객관식" },
  { value: "CHECKBOX", label: "체크박스" },
  { value: "DROPDOWN", label: "드롭다운" },
  { value: "DATE", label: "날짜" },
  { value: "TIME", label: "시간" },
];

function AnswerPreview({ type }: { type: CreateQuestionRequestQuestionType }) {
  switch (type) {
    case "SHORT_ANSWER":
      return (
        <p className="text-sm text-muted-foreground/50 border-b border-border pb-1 w-48">
          단답 답변 입력란
        </p>
      );
    case "PARAGRAPH":
      return (
        <div className="border border-border rounded-r2 px-s3 py-s2 text-sm text-muted-foreground/50 w-full h-16 flex items-start">
          장문 답변 입력란
        </div>
      );
    case "MULTIPLE_CHOICE":
      return (
        <div className="space-y-s2">
          {["옵션 1", "옵션 2"].map((o) => (
            <div
              key={o}
              className="flex items-center gap-s2 text-sm text-muted-foreground/60"
            >
              <div className="w-3 h-3 rounded-full border border-muted-foreground/40 shrink-0" />
              {o}
            </div>
          ))}
        </div>
      );
    case "CHECKBOX":
      return (
        <div className="space-y-s2">
          {["옵션 1", "옵션 2"].map((o) => (
            <div
              key={o}
              className="flex items-center gap-s2 text-sm text-muted-foreground/60"
            >
              <div className="w-3 h-3 rounded-sm border border-muted-foreground/40 shrink-0" />
              {o}
            </div>
          ))}
        </div>
      );
    case "DROPDOWN":
      return (
        <div className="flex items-center gap-s1 text-sm text-muted-foreground/60 border-b border-border pb-1 w-36">
          옵션 선택
          <span className="ml-auto text-xs">▾</span>
        </div>
      );
    case "DATE":
      return (
        <p className="text-sm text-muted-foreground/50 border-b border-border pb-1 w-36">
          날짜 선택
        </p>
      );
    case "TIME":
      return (
        <p className="text-sm text-muted-foreground/50 border-b border-border pb-1 w-36">
          시간 선택
        </p>
      );
    default:
      return <p className="text-sm text-muted-foreground/50">답변 미리보기</p>;
  }
}

interface SurveyQuestionBuilderProps {
  questions: DraftQuestion[];
  onChange: (questions: DraftQuestion[]) => void;
}

export function SurveyQuestionBuilder({
  questions,
  onChange,
}: SurveyQuestionBuilderProps) {
  const addQuestion = () => {
    const localId = `local-${Date.now()}-${Math.random()}`;
    onChange([
      ...questions,
      {
        localId,
        questionType: "SHORT_ANSWER",
        title: "",
        required: false,
        displayOrder: questions.length + 1,
      },
    ]);
  };

  const updateQuestion = (localId: string, updates: Partial<DraftQuestion>) => {
    onChange(
      questions.map((q) => (q.localId === localId ? { ...q, ...updates } : q)),
    );
  };

  const copyQuestion = (localId: string) => {
    const q = questions.find((item) => item.localId === localId);
    if (!q) return;
    const newId = `local-${Date.now()}-${Math.random()}`;
    const idx = questions.findIndex((item) => item.localId === localId);
    const newQuestions = [...questions];
    newQuestions.splice(idx + 1, 0, {
      ...q,
      localId: newId,
      serverId: undefined,
      displayOrder: idx + 2,
    });
    onChange(newQuestions.map((item, i) => ({ ...item, displayOrder: i + 1 })));
  };

  const deleteQuestion = (localId: string) => {
    onChange(
      questions
        .filter((q) => q.localId !== localId)
        .map((q, i) => ({ ...q, displayOrder: i + 1 })),
    );
  };

  return (
    <div className="rounded-r4 border bg-card border-border shadow-sm">
      <div className="px-s5 py-s3 border-b border-border flex items-center justify-between">
        <p className="typo-label text-muted-foreground flex items-center gap-s2">
          <ClipboardList size={14} /> 신청 설문 문항
        </p>
        <button
          type="button"
          onClick={addQuestion}
          className="flex items-center gap-s1 text-sm text-primary hover:text-primary/80 transition cursor-pointer font-medium"
        >
          <Plus size={14} /> 문항 추가
        </button>
      </div>
      <div className="px-s5 py-s4 space-y-s3">
        {questions.length === 0 && (
          <p className="typo-c1 text-muted-foreground text-center py-s4">
            문항을 추가하면 신청자에게 설문을 받을 수 있습니다
          </p>
        )}
        {questions.map((q) => (
          <div
            key={q.localId}
            className="rounded-r3 border border-border bg-muted/30 overflow-hidden"
          >
            {/* 질문 헤더: 제목 입력 + 타입 선택 */}
            <div className="px-s4 pt-s3 pb-s2 flex items-center gap-s3">
              <input
                type="text"
                value={q.title}
                onChange={(e) =>
                  updateQuestion(q.localId, { title: e.target.value })
                }
                placeholder="질문을 입력하세요"
                className="flex-1 text-sm font-medium bg-transparent border-none focus:outline-none focus:ring-0 placeholder:text-muted-foreground/40"
              />
              <select
                value={q.questionType}
                onChange={(e) =>
                  updateQuestion(q.localId, {
                    questionType: e.target
                      .value as CreateQuestionRequestQuestionType,
                  })
                }
                className="text-xs border border-border rounded-r2 px-s2 py-1 bg-card text-foreground focus:outline-none focus:border-primary cursor-pointer shrink-0"
              >
                {QUESTION_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            {/* 답변 미리보기 */}
            <div className="px-s4 pb-s3">
              <AnswerPreview type={q.questionType} />
            </div>

            {/* 하단 액션 버튼 */}
            <div className="px-s4 py-s2 border-t border-border/50 flex items-center justify-end gap-s3">
              <button
                type="button"
                onClick={() => copyQuestion(q.localId)}
                className="text-muted-foreground hover:text-foreground transition cursor-pointer"
                title="복사"
              >
                <Copy size={14} />
              </button>
              <button
                type="button"
                onClick={() => deleteQuestion(q.localId)}
                className="text-muted-foreground hover:text-destructive transition cursor-pointer"
                title="삭제"
              >
                <Trash2 size={14} />
              </button>
              <div className="w-px h-4 bg-border" />
              <button
                type="button"
                onClick={() =>
                  updateQuestion(q.localId, { required: !q.required })
                }
                className={cn(
                  "text-xs font-medium px-s2 py-0.5 rounded-full transition cursor-pointer",
                  q.required
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                필수
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
