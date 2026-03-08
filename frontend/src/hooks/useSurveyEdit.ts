import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useCreateSurvey } from "@/api/model/survey/survey";
import {
  useCreateQuestion,
  useDeleteQuestion,
  useUpdateQuestion,
  useGetQuestionList,
  getGetQuestionListQueryKey,
  getQuestionList,
} from "@/api/model/survey-question/survey-question";
import {
  useCreateOption,
  useDeleteOption,
} from "@/api/model/survey-question-option/survey-question-option";
import type { DraftQuestion } from "@/components/feature/event/SurveyQuestionBuilder";

export function useSurveyEdit(existingSurveyId?: number) {
  const queryClient = useQueryClient();
  const [draftQuestions, setDraftQuestions] = useState<DraftQuestion[]>([]);

  const { data: questionListResponse, isFetching: isQuestionsFetching } =
    useGetQuestionList(existingSurveyId ?? 0, {
      query: { enabled: !!existingSurveyId },
    });

  const { mutateAsync: createSurveyAsync } = useCreateSurvey();
  const { mutateAsync: createQuestionAsync } = useCreateQuestion();
  const { mutateAsync: deleteQuestionAsync } = useDeleteQuestion();
  const { mutateAsync: updateQuestionAsync } = useUpdateQuestion();
  const { mutateAsync: createOptionAsync } = useCreateOption();
  const { mutateAsync: deleteOptionAsync } = useDeleteOption();

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
          options:
            q.options && q.options.length > 0
              ? q.options.map((o) => o.text ?? "")
              : undefined,
        })),
      );
    }
  }, [questionListResponse]);

  // 기존 설문을 diff 처리하여 저장하고 surveyId를 반환.
  // 문항이 전부 삭제된 경우 undefined (연결 해제).
  // 실패 시 캐시 무효화 후 throw하여 호출자가 처리.
  const submitSurvey = async (
    eventTitle: string,
    allowExternal?: boolean,
  ): Promise<number | undefined> => {
    try {
      if (existingSurveyId) {
        // 항상 서버에서 최신 데이터를 가져와 stale 캐시로 인한 오류 방지
        const freshResponse = await getQuestionList(existingSurveyId);
        const freshQuestions =
          freshResponse.status === 200 ? freshResponse.data : [];

        const originalIds = new Set(freshQuestions.map((q) => q.id));
        const currentServerIds = new Set(
          draftQuestions.filter((q) => q.serverId).map((q) => q.serverId),
        );

        for (const id of originalIds) {
          if (id !== undefined && !currentServerIds.has(id)) {
            await deleteQuestionAsync({
              surveyId: existingSurveyId,
              questionId: id,
            });
          }
        }

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
            if (q.options !== undefined) {
              const originalQ = freshQuestions.find(
                (oq) => oq.id === q.serverId,
              );
              for (const opt of originalQ?.options ?? []) {
                if (opt.id) {
                  await deleteOptionAsync({
                    surveyId: existingSurveyId,
                    questionId: q.serverId,
                    optionId: opt.id,
                  });
                }
              }
              for (const [i, text] of q.options.entries()) {
                if (text.trim()) {
                  await createOptionAsync({
                    surveyId: existingSurveyId,
                    questionId: q.serverId,
                    data: { text: text.trim(), displayOrder: i + 1 },
                  });
                }
              }
            }
          } else {
            const qRes = await createQuestionAsync({
              surveyId: existingSurveyId,
              data: {
                questionType: q.questionType,
                title: q.title || "질문",
                required: q.required,
                displayOrder: q.displayOrder,
              },
            });
            const newQuestionId =
              qRes.status === 201 ? (qRes.data?.id ?? undefined) : undefined;
            if (newQuestionId && q.options?.length) {
              for (const [i, text] of q.options.entries()) {
                if (text.trim()) {
                  await createOptionAsync({
                    surveyId: existingSurveyId,
                    questionId: newQuestionId,
                    data: { text: text.trim(), displayOrder: i + 1 },
                  });
                }
              }
            }
          }
        }

        return draftQuestions.length > 0 ? existingSurveyId : undefined;
      } else if (draftQuestions.length > 0) {
        // 기존 설문 없음 & 새 문항 있음 → 신규 생성
        const surveyRes = await createSurveyAsync({
          data: {
            title: `${eventTitle} 신청 설문`,
            accessLevel: allowExternal ? "PUBLIC" : "MEMBER",
          },
        });
        const newSurveyId =
          surveyRes.status === 201
            ? (surveyRes.data.id ?? undefined)
            : undefined;
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
            qRes.status === 201 ? (qRes.data?.id ?? undefined) : undefined;
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
      }

      return undefined;
    } catch (err) {
      // 부분 변경 후 실패 시 캐시 무효화
      if (existingSurveyId) {
        void queryClient.invalidateQueries({
          queryKey: getGetQuestionListQueryKey(existingSurveyId),
        });
      }
      throw err;
    }
  };

  return {
    draftQuestions,
    setDraftQuestions,
    submitSurvey,
    isQuestionsFetching,
  };
}
