export interface MatrixRowDTO {
  chapterName: string;
  lessonName: string;
  questionType: string;
  difficultyLevel: string;
  selectedCount: number;
}

export interface MatrixDetailRowDTO extends MatrixRowDTO {
  exerciseType: string;
  learningOutcome: string;
}

export interface MatrixWordPayloadRequestDTO {
  examName: string;
  matrix: MatrixRowDTO[];
  matrixDetails: MatrixDetailRowDTO[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isRow(value: unknown): value is MatrixRowDTO {
  return isRecord(value)
    && ["chapterName", "lessonName", "questionType", "difficultyLevel"]
      .every((key) => typeof value[key] === "string")
    && typeof value.selectedCount === "number"
    && Number.isSafeInteger(value.selectedCount) && value.selectedCount >= 0;
}

export function isMatrixWordPayload(value: unknown): value is MatrixWordPayloadRequestDTO {
  return isRecord(value)
    && typeof value.examName === "string" && value.examName.trim().length > 0
    && Array.isArray(value.matrix) && value.matrix.length > 0 && value.matrix.every(isRow)
    && Array.isArray(value.matrixDetails) && value.matrixDetails.length > 0
    && value.matrixDetails.every((row: unknown) => isRow(row)
      && "exerciseType" in row && typeof row.exerciseType === "string"
      && "learningOutcome" in row && typeof row.learningOutcome === "string");
}
