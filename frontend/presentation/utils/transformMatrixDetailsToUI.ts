import { MatrixDetailsItems } from "@/domain/entities/draft.entity";

export type GroupedMatrixDetails = {
  [difficultyLevel: string]: {
    [learningOutcome: string]: {
      [questionType: string]: number;
    };
  };
};

export const transformMatrixDetailsToUI = (
  matrixDetails: MatrixDetailsItems[],
): GroupedMatrixDetails => {
  if (!matrixDetails || !Array.isArray(matrixDetails)) return {};

  return matrixDetails.reduce((acc, item) => {
    const { difficultyLevel, learningOutcome, questionType, selectedCount } =
      item;

    if (!acc[difficultyLevel]) {
      acc[difficultyLevel] = {};
    }

    if (!acc[difficultyLevel][learningOutcome]) {
      acc[difficultyLevel][learningOutcome] = {};
    }

    if (!acc[difficultyLevel][learningOutcome][questionType]) {
      acc[difficultyLevel][learningOutcome][questionType] = 0;
    }
    acc[difficultyLevel][learningOutcome][questionType] += selectedCount;

    return acc;
  }, {} as GroupedMatrixDetails);
};
