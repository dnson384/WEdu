import { MatrixItems } from "@/domain/entities/draft.entity";

export type GroupedMatrix = {
  [questionType: string]: {
    [difficultyLevel: string]: number;
  };
};

export const transformMatrixToUI = (matrix: MatrixItems[]): GroupedMatrix => {
  if (!matrix || !Array.isArray(matrix)) return {};

  return matrix.reduce((acc, item) => {
    const { questionType, difficultyLevel, selectedCount } = item;

    if (!acc[questionType]) {
      acc[questionType] = {};
    }

    acc[questionType][difficultyLevel] = selectedCount;

    return acc;
  }, {} as GroupedMatrix);
};
