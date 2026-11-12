import type { MatrixRowDTO, MatrixWordPayloadRequestDTO } from "../../dto/request/MatrixWordPayloadRequestDTO.js";

export interface MatrixExportRow {
  chapterName: string;
  lessonName: string;
  exerciseType: string;
  learningOutcome: string;
  counts: number[];
}

export interface MatrixExportData {
  questionTypes: string[];
  levels: string[];
  matrix: MatrixExportRow[];
  matrixDetails: MatrixExportRow[];
  totalQuestions: number;
}

function exportLevel(level: string): string {
  if (level === "Nhận biết") return "Biết";
  if (level === "Thông hiểu") return "Hiểu";
  if (level === "Vận dụng cao") return "Vận dụng";
  return level;
}

// Only the presentation groups change. The request and stored snapshot stay intact.
export function prepareMatrixExport(data: MatrixWordPayloadRequestDTO): MatrixExportData {
  const allRows = [...data.matrix, ...data.matrixDetails];
  const questionTypes = [...new Set(["Nhiều lựa chọn", "Đúng sai", "Trả lời ngắn",
    ...allRows.map((row) => row.questionType)])];
  const levels = [...new Set(["Biết", "Hiểu", "Vận dụng",
    ...allRows.map((row) => exportLevel(row.difficultyLevel))])];

  function groupRows(rows: MatrixRowDTO[], detailed: boolean): MatrixExportRow[] {
    const chapters = new Map<string, Map<string, MatrixExportRow>>();
    for (const row of rows) {
      const exerciseType = detailed && "exerciseType" in row ? String(row.exerciseType) : "";
      const learningOutcome = detailed && "learningOutcome" in row ? String(row.learningOutcome) : "";
      const key = JSON.stringify([row.lessonName, exerciseType, learningOutcome]);
      let chapter = chapters.get(row.chapterName);
      if (!chapter) {
        chapter = new Map();
        chapters.set(row.chapterName, chapter);
      }
      let grouped = chapter.get(key);
      if (!grouped) {
        grouped = { chapterName: row.chapterName, lessonName: row.lessonName, exerciseType,
          learningOutcome, counts: Array<number>(questionTypes.length * levels.length).fill(0) };
        chapter.set(key, grouped);
      }
      const index = questionTypes.indexOf(row.questionType) * levels.length
        + levels.indexOf(exportLevel(row.difficultyLevel));
      grouped.counts[index] = (grouped.counts[index] ?? 0) + row.selectedCount;
    }
    return [...chapters.values()].flatMap((chapter) => [...chapter.values()]);
  }

  return {
    questionTypes, levels,
    matrix: groupRows(data.matrix, false),
    matrixDetails: groupRows(data.matrixDetails, true),
    totalQuestions: data.matrix.reduce((sum, row) => sum + row.selectedCount, 0),
  };
}

export function sumCounts(counts: number[]): number {
  return counts.reduce((sum, count) => sum + count, 0);
}

export function levelTotals(counts: number[], levelCount: number): number[] {
  return Array.from({ length: levelCount }, (_, level) =>
    counts.reduce((sum, count, index) => sum + (index % levelCount === level ? count : 0), 0));
}

export function pointsForCount(count: number, totalQuestions: number): number {
  return totalQuestions > 0 ? count * 10 / totalQuestions : 0;
}

export function percentageForCount(count: number, totalQuestions: number): number {
  return totalQuestions > 0 ? count * 100 / totalQuestions : 0;
}

export function formatMatrixNumber(value: number): string {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(value);
}
