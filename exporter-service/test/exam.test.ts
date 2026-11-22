import assert from "node:assert/strict";
import test from "node:test";
import JSZip from "jszip";
import type { WordPayloadRequestDTO } from "../src/routes/exporter/dto/request/WordPayloadRequestDTO.js";
import { GenerateWordFileUsecase } from "../src/routes/exporter/usecase/generateWordFile.js";

test("tạo DOCX có tiêu đề, câu hỏi, phương án, ảnh nhúng và công thức OMML", async () => {
  const imageUrl = "https://media.example.test/figure.png";
  const png =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1sAAAAASUVORK5CYII=";
  const payload: WordPayloadRequestDTO = {
    examName: "Đề Vật lý",
    imageCache: { questions: { [imageUrl]: png }, options: {} },
    questionsSorted: [
      {
        questionType: "Nhiều lựa chọn",
        questionsData: [
          {
            id: "question-1",
            question: {
              template: "Quan sát <img_1> và tính <math_1>",
              variables: { math: { math_1: "x^2" }, image: { img_1: imageUrl } },
            },
            options: [
              {
                template: "Kết quả <math_1>",
                variables: { math: { math_1: "y+1" }, image: {} },
              },
            ],
          },
        ],
      },
      {
        questionType: "Trả lời ngắn",
        questionsData: [
          {
            id: "question-2",
            question: {
              template: "Viết công thức vận tốc",
              variables: { math: {}, image: {} },
            },
            options: [],
          },
        ],
      },
    ],
  };

  const buffer = await new GenerateWordFileUsecase().execute(payload);

  const zip = await JSZip.loadAsync(buffer);
  const document = zip.file("word/document.xml");
  assert.ok(document);
  const xml = await document.async("string");
  assert.match(xml, /Đề Vật lý/u);
  assert.match(xml, /Phần 1\. Nhiều lựa chọn/u);
  assert.match(xml, /Phần 2\. Trả lời ngắn/u);
  assert.match(xml, /Kết quả/u);
  assert.match(xml, /Viết công thức vận tốc/u);
  assert.equal([...xml.matchAll(/<m:oMath(?:\s[^>]*)?>/gu)].length, 2);
  assert.doesNotMatch(xml, /MMMMATH_/u);
  const [image] = zip.file(/^word\/media\/[^/]+\.png$/u);
  assert.ok(image);
  assert.deepEqual(await image.async("nodebuffer"), Buffer.from(png, "base64"));
});
