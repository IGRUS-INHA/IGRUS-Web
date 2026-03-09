import { useMemo } from "react";
import type { SurveyDetailResponse } from "@/api/model/models/surveyDetailResponse";
import type { AdminSurveyResponseListItem } from "@/api/model/models/adminSurveyResponseListItem";
import type { AnswerResponse } from "@/api/model/models/answerResponse";
import type { QuestionResponse } from "@/api/model/models/questionResponse";
import { formatDate } from "@/utils/date";

interface Props {
  survey: SurveyDetailResponse | undefined;
  response: AdminSurveyResponseListItem | undefined;
  userName?: string;
}

function renderAnswer(
  question: QuestionResponse,
  answer: AnswerResponse | undefined,
): string {
  if (!answer) return "미응답";

  switch (question.questionType) {
    case "SHORT_ANSWER":
    case "PARAGRAPH":
      return answer.textValue ?? "미응답";

    case "MULTIPLE_CHOICE":
    case "DROPDOWN": {
      const selectedId = answer.selectedOptionIds?.[0];
      const option = question.options?.find((o) => o.id === selectedId);
      return option?.text ?? "미응답";
    }

    case "CHECKBOX": {
      const labels = (answer.selectedOptionIds ?? [])
        .map((id) => question.options?.find((o) => o.id === id)?.text)
        .filter((t): t is string => t !== undefined);
      return labels.length > 0 ? labels.join(", ") : "미응답";
    }

    case "LINEAR_SCALE":
      return answer.numericValue !== undefined
        ? `${answer.numericValue} / ${question.scaleMax ?? 5}`
        : "미응답";

    default:
      return "표시할 수 없는 형식";
  }
}

export default function SurveyAnswerPanel({
  survey,
  response,
  userName,
}: Props) {
  const questions = useMemo(
    () =>
      [...(survey?.questions ?? [])].sort(
        (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
      ),
    [survey?.questions],
  );

  const answerByQuestionId = useMemo(() => {
    const map = new Map<number, AnswerResponse>();
    for (const answer of response?.answers ?? []) {
      if (answer.questionId !== undefined) map.set(answer.questionId, answer);
    }
    return map;
  }, [response]);

  if (!survey) {
    return (
      <div className="bg-muted/30 border-b border-border px-s6 py-s5">
        <p className="typo-b2 text-muted-foreground">
          설문 정보를 불러오는 중...
        </p>
      </div>
    );
  }

  if (!response) {
    return (
      <div className="bg-muted/30 border-b border-border px-s6 py-s5">
        <p className="typo-b2 text-muted-foreground">
          이 신청자는 설문을 제출하지 않았습니다.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-muted/30 border-b border-border px-s6 py-s5">
      <div className="space-y-s4">
        <p className="typo-c1 text-muted-foreground font-bold uppercase tracking-widest">
          {userName ?? "신청자"} 설문 응답 · {formatDate(response.submittedAt)}
        </p>
        {questions.map((q, i) => (
          <div key={q.id} className="space-y-s1">
            <p className="typo-b2 font-bold">
              {i + 1}. {q.title}
            </p>
            <p className="typo-b2 text-foreground pl-s4">
              {renderAnswer(q, answerByQuestionId.get(q.id ?? 0))}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
