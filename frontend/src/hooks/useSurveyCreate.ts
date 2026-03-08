import { useState } from "react";
import { useCreateSurvey } from "@/api/model/survey/survey";
import { useCreateQuestion } from "@/api/model/survey-question/survey-question";
import { useCreateOption } from "@/api/model/survey-question-option/survey-question-option";
import type { DraftQuestion } from "@/components/feature/event/SurveyQuestionBuilder";

export function useSurveyCreate() {
  const [draftQuestions, setDraftQuestions] = useState<DraftQuestion[]>([]);

  const { mutateAsync: createSurveyAsync } = useCreateSurvey();
  const { mutateAsync: createQuestionAsync } = useCreateQuestion();
  const { mutateAsync: createOptionAsync } = useCreateOption();

  // 설문 전체를 새로 생성하고 surveyId를 반환. 문항이 없으면 undefined.
  // 실패 시 throw하여 호출자가 처리.
  const submitSurvey = async (
    eventTitle: string,
  ): Promise<number | undefined> => {
    if (draftQuestions.length === 0) return undefined;

    const surveyRes = await createSurveyAsync({
      data: { title: `${eventTitle} 신청 설문`, accessLevel: "MEMBER" },
    });
    const newSurveyId =
      surveyRes.status === 201 ? (surveyRes.data.id ?? undefined) : undefined;
    if (!newSurveyId) return undefined;

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
        qRes.status === 201
          ? (qRes.data?.questions?.find(
              (question) => question.displayOrder === q.displayOrder,
            )?.id ?? undefined)
          : undefined;
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

    return newSurveyId;
  };

  return { draftQuestions, setDraftQuestions, submitSurvey };
}
