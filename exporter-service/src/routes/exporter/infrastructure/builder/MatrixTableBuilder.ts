import {
  AlignmentType, BorderStyle, Paragraph, Table, TableCell, TableLayoutType,
  TableRow, TextRun, VerticalAlign, WidthType,
} from "docx";
import type { ITableCellOptions } from "docx";
import {
  formatMatrixNumber, levelTotals, percentageForCount, pointsForCount, sumCounts,
} from "../../usecase/util/MatrixExportData.js";
import type { MatrixExportData, MatrixExportRow } from "../../usecase/util/MatrixExportData.js";

const TABLE_WIDTH = 12960;
const BORDER = { style: BorderStyle.SINGLE, size: 4, color: "000000" };

function cell(text: string, options: {
  span?: number; rowSpan?: number; bold?: boolean; left?: boolean;
} = {}): TableCell {
  const properties: ITableCellOptions = {
    children: text.split("\n").map((line) => new Paragraph({
      alignment: options.left ? AlignmentType.LEFT : AlignmentType.CENTER,
      spacing: { before: 0, after: 80, line: 240 },
      children: [new TextRun({ text: line, font: "Arial", size: 24, bold: options.bold ?? false, color: "000000" })],
    })),
    verticalAlign: VerticalAlign.CENTER,
    margins: { top: 70, bottom: 70, left: 60, right: 60 },
    ...(options.span === undefined ? {} : { columnSpan: options.span }),
    ...(options.rowSpan === undefined ? {} : { rowSpan: options.rowSpan }),
  };
  return new TableCell(properties);
}

function row(cells: TableCell[], header = false): TableRow {
  return new TableRow({ children: cells, ...(header ? { tableHeader: true } : {}) });
}

function headerRows(data: MatrixExportData, detailed: boolean): TableRow[] {
  const count = data.questionTypes.length * data.levels.length;
  const fixed = ["TT", "Chủ đề/Chương", "Nội dung/Đơn vị kiến thức",
    ...(detailed ? ["Yêu cầu cần đạt"] : [])];
  const heading = (text: string, span = 1, rowSpan = 1) => cell(text, { span, rowSpan, bold: true });
  return [
    row([
      ...fixed.map((text) => heading(text, 1, 4)),
      heading(detailed ? "Số câu hỏi ở các mức độ đánh giá" : "Mức độ đánh giá", count),
      ...(!detailed ? [heading("Tổng", data.levels.length, 3), heading("Tỷ lệ % điểm", 1, 4)] : []),
    ], true),
    row([heading(data.questionTypes.length === 3 ? "TNKQ" : "Loại câu hỏi", count)], true),
    row(data.questionTypes.map((type) => heading(type, data.levels.length)), true),
    row([
      ...data.questionTypes.flatMap(() => data.levels.map((level) => heading(level))),
      ...(!detailed ? data.levels.map((level) => heading(level)) : []),
    ], true),
  ];
}

function chapterSpan(rows: MatrixExportRow[], index: number): number {
  let span = 1;
  while (rows[index + span]?.chapterName === rows[index]?.chapterName) span++;
  return span;
}

function lessonSpan(rows: MatrixExportRow[], index: number): number {
  let span = 1;
  while (rows[index + span]?.chapterName === rows[index]?.chapterName
      && rows[index + span]?.lessonName === rows[index]?.lessonName) span++;
  return span;
}

function bodyRows(data: MatrixExportData, detailed: boolean): TableRow[] {
  const rows = detailed ? data.matrixDetails : data.matrix;
  let chapterNumber = 0;
  return rows.map((item, index) => {
    const previous = rows[index - 1];
    const newChapter = !previous || previous.chapterName !== item.chapterName;
    const newLesson = newChapter || previous?.lessonName !== item.lessonName;
    if (newChapter) chapterNumber++;
    const cells: TableCell[] = [];
    if (newChapter) {
      const span = chapterSpan(rows, index);
      cells.push(cell(String(chapterNumber), { rowSpan: span }),
        cell(item.chapterName, { rowSpan: span, left: true }));
    }
    if (newLesson || !detailed) {
      cells.push(cell(item.lessonName, { left: true, rowSpan: detailed ? lessonSpan(rows, index) : 1 }));
    }
    if (detailed) {
      cells.push(cell([item.exerciseType, item.learningOutcome].filter(Boolean).join("\n"), { left: true }));
    }
    cells.push(...item.counts.map((count) => cell(String(count))));
    if (!detailed) {
      cells.push(...levelTotals(item.counts, data.levels.length).map((count) => cell(String(count))),
        cell(formatMatrixNumber(percentageForCount(sumCounts(item.counts), data.totalQuestions))));
    }
    return row(cells);
  });
}

function summaryRows(data: MatrixExportData, detailed: boolean): TableRow[] {
  const rows = detailed ? data.matrixDetails : data.matrix;
  const counts = Array<number>(data.questionTypes.length * data.levels.length).fill(0);
  for (const item of rows) item.counts.forEach((count, index) => { counts[index] = (counts[index] ?? 0) + count; });
  const totals = levelTotals(counts, data.levels.length);
  const totalCount = sumCounts(counts);
  const fixedSpan = detailed ? 4 : 3;
  const label = (text: string) => cell(text, { span: fixedSpan, bold: true });
  const typeTotals = data.questionTypes.map((_, index) =>
    sumCounts(counts.slice(index * data.levels.length, (index + 1) * data.levels.length)));
  return [
    row([label("Tổng số câu"), ...counts.map((count) => cell(String(count), { bold: true })),
      ...(!detailed ? [...totals.map((count) => cell(String(count), { bold: true })),
        cell(formatMatrixNumber(percentageForCount(totalCount, data.totalQuestions)), { bold: true })] : [])]),
    row([label("Tổng số điểm"), ...typeTotals.map((count) => cell(
      formatMatrixNumber(pointsForCount(count, data.totalQuestions)), { span: data.levels.length, bold: true })),
      ...(!detailed ? [...totals.map((count) => cell(formatMatrixNumber(pointsForCount(count, data.totalQuestions)), { bold: true })), cell("")] : [])]),
    row([label("Tỷ lệ %"), ...typeTotals.map((count) => cell(
      formatMatrixNumber(percentageForCount(count, data.totalQuestions)), { span: data.levels.length, bold: true })),
      ...(!detailed ? [...totals.map((count) => cell(formatMatrixNumber(percentageForCount(count, data.totalQuestions)), { bold: true })),
        cell(formatMatrixNumber(percentageForCount(totalCount, data.totalQuestions)), { bold: true })] : [])]),
  ];
}

function buildTable(data: MatrixExportData, detailed: boolean): Table {
  // Keep the reference's landscape grid with space for long learning outcomes.
  const fixedWidths = detailed ? [520, 1300, 1800, 3100] : [520, 1180, 1800];
  const extraWidths = detailed ? [] : [...Array<number>(data.levels.length).fill(620), 780];
  const numericCount = data.questionTypes.length * data.levels.length;
  const numericWidth = TABLE_WIDTH - sumCounts(fixedWidths) - sumCounts(extraWidths);
  const columnWidths = [...fixedWidths,
    ...Array.from({ length: numericCount }, (_, index) =>
      Math.floor(numericWidth / numericCount) + (index < numericWidth % numericCount ? 1 : 0)), ...extraWidths];
  return new Table({
    width: { size: TABLE_WIDTH, type: WidthType.DXA },
    columnWidths,
    layout: TableLayoutType.FIXED,
    alignment: AlignmentType.CENTER,
    borders: { top: BORDER, bottom: BORDER, left: BORDER, right: BORDER, insideHorizontal: BORDER, insideVertical: BORDER },
    rows: [...headerRows(data, detailed), ...bodyRows(data, detailed), ...summaryRows(data, detailed)],
  });
}

export function buildMatrixTable(data: MatrixExportData): Table {
  return buildTable(data, false);
}

export function buildMatrixDetailsTable(data: MatrixExportData): Table {
  return buildTable(data, true);
}
