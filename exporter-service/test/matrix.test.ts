import assert from "node:assert/strict";
import test from "node:test";
import JSZip from "jszip";
import {
  isMatrixWordPayload,
  type MatrixWordPayloadRequestDTO,
} from "../src/routes/exporter/dto/request/MatrixWordPayloadRequestDTO.js";
import { GenerateMatrixWordFileUsecase } from "../src/routes/exporter/usecase/generateMatrixWordFile.js";
import {
  formatMatrixNumber,
  percentageForCount,
  pointsForCount,
  prepareMatrixExport,
} from "../src/routes/exporter/usecase/util/MatrixExportData.js";

function matrixPayload(): MatrixWordPayloadRequestDTO {
  const row = {
    chapterName: "Chương Sóng",
    lessonName: "Bài giao thoa",
    questionType: "Đúng sai",
    difficultyLevel: "Vận dụng",
    selectedCount: 2,
  };
  return {
    examName: "Tên đề chỉ dùng làm tên tệp",
    matrix: [
      row,
      { ...row, difficultyLevel: "Vận dụng cao", selectedCount: 1 },
      { ...row, lessonName: "Bài chưa chọn câu", selectedCount: 0 },
    ],
    matrixDetails: [
      { ...row, exerciseType: "Tính toán", learningOutcome: "Xác định bước sóng" },
      { ...row, exerciseType: "Giải thích", learningOutcome: "Mô tả hiện tượng", selectedCount: 0 },
    ],
  };
}

test("chấp nhận snapshot hợp lệ khi có dòng chưa chọn câu", () => {
  assert.equal(isMatrixWordPayload(matrixPayload()), true);
});

test("từ chối payload khi thiếu ma trận, tên đề hoặc metadata đặc tả", () => {
  const payload = matrixPayload();
  const invalid: unknown[] = [
    null,
    { ...payload, examName: " " },
    { ...payload, matrix: [] },
    { ...payload, matrixDetails: [] },
    { ...payload, matrixDetails: [{ ...payload.matrixDetails[0], learningOutcome: 42 }] },
    { ...payload, matrix: [{ ...payload.matrix[0], selectedCount: -1 }] },
    { ...payload, matrix: [{ ...payload.matrix[0], selectedCount: 1.5 }] },
  ];

  for (const body of invalid) {
    assert.equal(isMatrixWordPayload(body), false);
  }
});

test("gộp mức vận dụng khi trình bày và giữ nguyên các dòng snapshot", () => {
  const payload = matrixPayload();
  const original = structuredClone(payload);

  const report = prepareMatrixExport(payload);

  assert.deepEqual(report.levels, ["Biết", "Hiểu", "Vận dụng"]);
  assert.equal(report.totalQuestions, 3);
  const countsForTrueFalseApplication =
    report.questionTypes.indexOf("Đúng sai") * report.levels.length
    + report.levels.indexOf("Vận dụng");
  assert.equal(report.matrix[0]?.counts[countsForTrueFalseApplication], 3);
  assert.equal(report.matrix[1]?.lessonName, "Bài chưa chọn câu");
  assert.ok(report.matrix[1]?.counts.every((count) => count === 0));
  assert.deepEqual(report.matrixDetails.map((row) => row.exerciseType), ["Tính toán", "Giải thích"]);
  assert.deepEqual(payload, original);
});

test("tính điểm, tỷ lệ và định dạng tiếng Việt, kể cả khi tổng số câu bằng không", () => {
  assert.equal(formatMatrixNumber(pointsForCount(1, 3)), "3,33");
  assert.equal(formatMatrixNumber(percentageForCount(1, 3)), "33,33");
  assert.equal(pointsForCount(0, 0), 0);
  assert.equal(percentageForCount(0, 0), 0);
});

test("tạo DOCX với hai bảng ngang và giữ bài chưa chọn câu khi xuất snapshot", async () => {
  const payload = matrixPayload();

  const buffer = await new GenerateMatrixWordFileUsecase().execute(payload);

  const zip = await JSZip.loadAsync(buffer);
  const document = zip.file("word/document.xml");
  assert.ok(document);
  const xml = await document.async("string");
  assert.equal([...xml.matchAll(/<w:tbl>/gu)].length, 2);
  assert.match(xml, /w:orient="landscape"/u);
  assert.match(xml, /<w:pageBreakBefore\b/u);
  assert.match(xml, /Bài chưa chọn câu/u);
  assert.match(xml, /Xác định bước sóng/u);
  assert.ok(!xml.includes(payload.examName));
});
