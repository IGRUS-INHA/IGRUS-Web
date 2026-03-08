import { useMemo } from "react";
import { useGetSurveyDetail } from "@/api/model/survey/survey";
import { useGetAdminSurveyResponses } from "@/api/model/admin-survey-response/admin-survey-response";
import type { SurveyDetailResponse } from "@/api/model/models/surveyDetailResponse";
import type { AdminSurveyResponseListItem } from "@/api/model/models/adminSurveyResponseListItem";
import type { AnswerResponse } from "@/api/model/models/answerResponse";
import SurveyQuestionCard from "./SurveyQuestionCard";

interface SurveyResultsTabProps {
  surveyId: number;
}

export default function SurveyResultsTab({ surveyId }: SurveyResultsTabProps) {
  const { data: surveyResponse, isLoading: surveyLoading } =
    useGetSurveyDetail(surveyId);
  const { data: responsesResponse, isLoading: responsesLoading } =
    useGetAdminSurveyResponses(surveyId);

  const survey =
    surveyResponse?.status === 200
      ? (surveyResponse.data as SurveyDetailResponse)
      : undefined;
  const responses =
    responsesResponse?.status === 200
      ? (responsesResponse.data as AdminSurveyResponseListItem[])
      : [];

  // 질문별 응답 매핑: questionId → AnswerResponse[]
  const answersByQuestion = useMemo(() => {
    const map = new Map<number, AnswerResponse[]>();
    for (const response of responses) {
      for (const answer of response.answers ?? []) {
        if (answer.questionId === undefined) continue;
        const existing = map.get(answer.questionId) ?? [];
        existing.push(answer);
        map.set(answer.questionId, existing);
      }
    }
    return map;
  }, [responses]);

  const questions = useMemo(() => {
    return [...(survey?.questions ?? [])].sort(
      (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
    );
  }, [survey?.questions]);

  const isLoading = surveyLoading || responsesLoading;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-muted-foreground">설문 결과를 불러오는 중...</p>
      </div>
    );
  }

  if (!survey) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        설문 정보를 불러올 수 없습니다.
      </div>
    );
  }

  if (responses.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        아직 제출된 응답이 없습니다.
      </div>
    );
  }

  return (
    <div className="space-y-s5">
      {/* Response summary */}
      <h2 className="typo-h3 font-bold text-foreground">
        응답 {responses.length}개
      </h2>

      {/* Question cards */}
      {questions.map((question) => (
        <SurveyQuestionCard
          key={question.id}
          question={question}
          answers={answersByQuestion.get(question.id ?? 0) ?? []}
        />
      ))}
    </div>
  );
}
