import type { AnswerResponse } from "@/api/model/models/answerResponse";
import type { OptionResponse } from "@/api/model/models/optionResponse";

export interface ChoiceAggregate {
  label: string;
  count: number;
  percentage: number;
}

export interface LinearScaleAggregate {
  average: number;
  max: number;
  distribution: {
    label: string;
    value: number;
    count: number;
    percentage: number;
  }[];
}

export interface ShortAnswerTag {
  text: string;
  count: number;
  isPopular: boolean;
}

/**
 * 선택형 질문(MULTIPLE_CHOICE, DROPDOWN, CHECKBOX)의 optionId별 응답 수 집계
 */
export function aggregateChoiceAnswers(
  answers: AnswerResponse[],
  options: OptionResponse[],
): ChoiceAggregate[] {
  const total = answers.length;
  if (total === 0) {
    return options.map((o) => ({
      label: o.text ?? "",
      count: 0,
      percentage: 0,
    }));
  }

  const countMap = new Map<number, number>();
  for (const opt of options) {
    if (opt.id !== undefined) countMap.set(opt.id, 0);
  }

  for (const answer of answers) {
    if (answer.selectedOptions) {
      for (const opt of answer.selectedOptions) {
        countMap.set(opt.id, (countMap.get(opt.id) ?? 0) + 1);
      }
    }
  }

  return options.map((opt) => {
    const count = opt.id !== undefined ? (countMap.get(opt.id) ?? 0) : 0;
    return {
      label: opt.text ?? "",
      count,
      percentage: total > 0 ? Math.round((count / total) * 1000) / 10 : 0,
    };
  });
}

/**
 * LINEAR_SCALE 질문의 평균 점수 + 각 점수별 분포 계산
 */
export function aggregateLinearScale(
  answers: AnswerResponse[],
  scaleMin: number,
  scaleMax: number,
): LinearScaleAggregate {
  const validAnswers = answers.filter(
    (a) => a.numericValue !== undefined && a.numericValue !== null,
  );
  const total = validAnswers.length;

  const countByValue = new Map<number, number>();
  for (let v = scaleMin; v <= scaleMax; v++) {
    countByValue.set(v, 0);
  }

  let sum = 0;
  for (const a of validAnswers) {
    const val = a.numericValue!;
    sum += val;
    countByValue.set(val, (countByValue.get(val) ?? 0) + 1);
  }

  const average = total > 0 ? Math.round((sum / total) * 10) / 10 : 0;

  const distribution: LinearScaleAggregate["distribution"] = [];
  for (let v = scaleMax; v >= scaleMin; v--) {
    const count = countByValue.get(v) ?? 0;
    distribution.push({
      label: String(v),
      value: v,
      count,
      percentage: total > 0 ? Math.round((count / total) * 1000) / 10 : 0,
    });
  }

  return { average, max: scaleMax, distribution };
}

/**
 * 단답형 질문의 동일 답변 빈도 집계 (태그 표시용)
 */
export function aggregateShortAnswers(
  answers: AnswerResponse[],
): ShortAnswerTag[] {
  const countMap = new Map<string, number>();
  for (const a of answers) {
    const text = a.textValue?.trim();
    if (!text) continue;
    countMap.set(text, (countMap.get(text) ?? 0) + 1);
  }

  const total = answers.length;
  const sorted = [...countMap.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([text, count]) => ({
      text,
      count,
      isPopular: total > 0 && count / total >= 0.15,
    }));

  return sorted;
}

/**
 * 장문형/단답형 텍스트 응답 목록 반환
 */
export function collectTextResponses(answers: AnswerResponse[]): string[] {
  return answers
    .map((a) => a.textValue?.trim())
    .filter((text): text is string => !!text);
}
