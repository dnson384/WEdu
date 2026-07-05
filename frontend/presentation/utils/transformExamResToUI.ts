import {
  ExamDetailReponseEntity,
  QuestionDetail,
} from "@/domain/entities/exam.entity";

const QUESTION_ORDER = ["Nhiều lựa chọn", "Đúng sai", "Trả lời ngắn"];

export type TransformedExamUI = {
  [questionType: string]: QuestionDetail[];
};

export function transformExamResToUI(rawData: ExamDetailReponseEntity) {
  const grouped = rawData.groups.reduce((acc, group) => {
    const type = group.questionType;

    if (!acc[type]) {
      acc[type] = [];
    }
    acc[type].push(...group.questions);

    return acc;
  }, {} as TransformedExamUI);

  const sorted: TransformedExamUI = {};

  QUESTION_ORDER.forEach((type) => {
    if (grouped[type]) {
      sorted[type] = grouped[type];
    }
  });

  return sorted;
}
