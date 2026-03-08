import { useState } from "react";
import {
  Circle,
  CheckSquare,
  AlignLeft,
  Star,
  ChevronDown,
  ChevronUp,
  List,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { QuestionResponse } from "@/api/model/models/questionResponse";
import type { AnswerResponse } from "@/api/model/models/answerResponse";
import {
  aggregateChoiceAnswers,
  aggregateLinearScale,
  aggregateShortAnswers,
  collectTextResponses,
} from "@/utils/surveyStats";
import type {
  ChoiceAggregate,
  LinearScaleAggregate,
} from "@/utils/surveyStats";

const BAR_COLORS = [
  "bg-primary",
  "bg-brand-l5",
  "bg-brand-l6",
  "bg-brand-l7",
  "bg-muted-foreground/50",
];

const QUESTION_TYPE_META: Record<
  string,
  { label: string; icon: typeof Circle }
> = {
  MULTIPLE_CHOICE: { label: "객관식", icon: Circle },
  DROPDOWN: { label: "드롭다운", icon: List },
  CHECKBOX: { label: "체크박스", icon: CheckSquare },
  LINEAR_SCALE: { label: "만족도", icon: Star },
  SHORT_ANSWER: { label: "단답형", icon: AlignLeft },
  PARAGRAPH: { label: "장문형", icon: AlignLeft },
};

const INITIAL_VISIBLE_COUNT = 5;

interface SurveyQuestionCardProps {
  question: QuestionResponse;
  answers: AnswerResponse[];
}

export default function SurveyQuestionCard({
  question,
  answers,
}: SurveyQuestionCardProps) {
  const questionType = question.questionType ?? "";
  const meta = QUESTION_TYPE_META[questionType] ?? {
    label: questionType,
    icon: AlignLeft,
  };
  const Icon = meta.icon;

  return (
    <div className="bg-card border border-border rounded-r3 overflow-hidden">
      {/* Question type badge */}
      <div className="flex items-center gap-s2 px-s5 pt-s4 typo-c1 font-medium text-muted-foreground">
        <Icon size={16} />
        {meta.label}
      </div>

      {/* Question title */}
      <div className="px-s5 pt-s3">
        <h3 className="typo-b2 font-medium text-foreground leading-relaxed">
          {question.title}
        </h3>
      </div>

      {/* Response count */}
      <div className="typo-c1 text-muted-foreground mt-s2 px-s5">
        응답 {answers.length}개
      </div>

      {/* Visualization body */}
      <div className="px-s5 pt-s4 pb-s5">
        <QuestionBody question={question} answers={answers} />
        <div className="h-1 bg-muted rounded-full mt-s4" />
      </div>
    </div>
  );
}

function QuestionBody({
  question,
  answers,
}: {
  question: QuestionResponse;
  answers: AnswerResponse[];
}) {
  const type = question.questionType;

  switch (type) {
    case "MULTIPLE_CHOICE":
    case "DROPDOWN":
    case "CHECKBOX":
      return (
        <BarChartView
          data={aggregateChoiceAnswers(answers, question.options ?? [])}
        />
      );
    case "LINEAR_SCALE":
      return (
        <LikertView
          data={aggregateLinearScale(
            answers,
            question.scaleMin ?? 1,
            question.scaleMax ?? 5,
          )}
        />
      );
    case "SHORT_ANSWER":
      return <ShortAnswerView answers={answers} />;
    case "PARAGRAPH":
      return <TextResponsesView texts={collectTextResponses(answers)} />;
    default:
      return <TextResponsesView texts={collectTextResponses(answers)} />;
  }
}

/* ===== 막대 차트 ===== */
function BarChartView({ data }: { data: ChoiceAggregate[] }) {
  if (data.length === 0) return null;

  return (
    <div className="flex flex-col gap-s3">
      {data.map((item, i) => (
        <div
          key={item.label}
          className="flex items-center gap-s4 max-sm:flex-wrap max-sm:gap-s2"
        >
          <span className="typo-c1 font-medium text-foreground min-w-[140px] max-sm:min-w-full max-sm:typo-c2 shrink-0">
            {item.label}
          </span>
          <div className="flex-1 h-7 bg-muted rounded-r2 overflow-hidden relative max-sm:w-full">
            <div
              className={cn(
                "h-full rounded-r2 transition-all duration-500",
                BAR_COLORS[i % BAR_COLORS.length],
              )}
              style={{ width: `${item.percentage}%` }}
            />
          </div>
          <span className="typo-c2 font-semibold text-muted-foreground min-w-12 text-right shrink-0">
            {item.percentage}%{" "}
            <span className="typo-c2 text-muted-foreground">
              ({item.count})
            </span>
          </span>
        </div>
      ))}
    </div>
  );
}

/* ===== 리커트 척도 ===== */
function LikertView({ data }: { data: LinearScaleAggregate }) {
  return (
    <div className="flex flex-col gap-s4">
      {/* Average score */}
      <div className="flex items-baseline gap-s2 mb-s2">
        <span className="text-4xl font-bold text-primary leading-none">
          {data.average}
        </span>
        <span className="typo-b1 text-muted-foreground font-medium">
          / {data.max}.0
        </span>
      </div>

      {/* Distribution bars */}
      <div className="flex flex-col gap-s2">
        {data.distribution.map((row, i) => {
          const opacity = 1 - i * 0.15;
          return (
            <div key={row.value} className="flex items-center gap-s3">
              <span className="typo-c1 font-medium text-muted-foreground min-w-14 text-right max-sm:min-w-10 max-sm:typo-c2">
                {row.label}
              </span>
              <div className="flex-1 h-5 bg-muted rounded-r1 overflow-hidden">
                <div
                  className="h-full rounded-r1 bg-primary transition-all duration-500"
                  style={{
                    width: `${row.percentage}%`,
                    opacity: Math.max(opacity, 0.4),
                  }}
                />
              </div>
              <span className="typo-c2 text-muted-foreground min-w-8 text-right">
                {row.count}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ===== 단답형 (태그 + 텍스트) ===== */
function ShortAnswerView({ answers }: { answers: AnswerResponse[] }) {
  const tags = aggregateShortAnswers(answers);
  const texts = collectTextResponses(answers);

  return (
    <div>
      {/* Tags */}
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-s2 mb-s4">
          {tags.map((tag) => (
            <span
              key={tag.text}
              className={cn(
                "px-s3 py-s1 typo-c1 font-medium rounded-full border",
                tag.isPopular
                  ? "bg-accent border-primary text-accent-foreground font-semibold"
                  : "bg-background border-border text-foreground",
              )}
            >
              {tag.text}{" "}
              <span className="typo-c2 text-muted-foreground ml-s1">
                {tag.count}
              </span>
            </span>
          ))}
        </div>
      )}

      {/* Text responses */}
      <TextResponsesView texts={texts} />
    </div>
  );
}

/* ===== 텍스트 응답 목록 (접기/펼치기) ===== */
function TextResponsesView({ texts }: { texts: string[] }) {
  const [expanded, setExpanded] = useState(false);

  if (texts.length === 0) {
    return (
      <p className="typo-c1 text-muted-foreground">텍스트 응답이 없습니다.</p>
    );
  }

  const visible = expanded ? texts : texts.slice(0, INITIAL_VISIBLE_COUNT);
  const hiddenCount = texts.length - INITIAL_VISIBLE_COUNT;

  return (
    <div className="flex flex-col gap-s3">
      {visible.map((text, i) => (
        <div
          key={i}
          className={cn(
            "px-s4 py-s3 bg-muted rounded-r2 typo-b2 text-foreground leading-relaxed border-l-3",
            i % 2 === 0 ? "border-l-primary" : "border-l-border",
          )}
        >
          {text}
        </div>
      ))}

      {hiddenCount > 0 && (
        <button
          type="button"
          onClick={() => setExpanded((prev) => !prev)}
          className="inline-flex items-center gap-s2 mt-s3 typo-c1 font-medium text-primary cursor-pointer hover:opacity-70 transition-opacity self-start"
        >
          {expanded ? (
            <>
              <ChevronUp size={14} />
              접기
            </>
          ) : (
            <>
              <ChevronDown size={14} />
              나머지 {hiddenCount}개 응답 보기
            </>
          )}
        </button>
      )}
    </div>
  );
}
