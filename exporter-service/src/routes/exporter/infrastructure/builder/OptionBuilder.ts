import { imageSize } from "image-size";

import {
  AlignmentType,
  BorderStyle,
  HeightRule,
  ImageRun,
  Paragraph,
  Table,
  TableCell,
  TableLayoutType,
  TableRow,
  TextRun,
  VerticalAlign,
  WidthType,
} from "docx";
import type { MathOptionContext } from "../../dto/request/MathContext.js";
import type { ContentDTO } from "../../dto/request/WordPayloadRequestDTO.js";
import { latexToOmml } from "../converter/latexToOmml.js";

export function OptionBuilder(
  questionId: string,
  index: number,
  questionType: string,
  option: ContentDTO,
  regex: RegExp,
  imageCache: Record<string, string>,
  mathOptionContext: MathOptionContext,
) {
  if (!mathOptionContext[questionId]) {
    mathOptionContext[questionId] = {
      map: {},
      counter: 0,
    };
  }

  const currentQuestion: any[] = [];

  const template = option.template;
  const mathVars = option.variables.math;
  const imageVars = option.variables.image;

  let questionsNode: any[] = [];

  if (questionType === "Nhiều lựa chọn") {
    questionsNode.push(
      new TextRun({
        text: `${String.fromCharCode(65 + index)}. `,
        bold: true,
      }),
    );
  } else if (questionType === "Đúng sai") {
    questionsNode.push(
      new TextRun({
        text: `${String.fromCharCode(97 + index)}. `,
        bold: true,
      }),
    );
  }

  const parts = template.split(regex);

  parts.forEach((part) => {
    // Xử lý công thức toán học
    if (part.startsWith("<math_") && part.endsWith(">")) {
      const mathKey = part.replace(/[<>]/g, "");
      const latex = mathVars[mathKey];
      const omml = latexToOmml(latex!);
      const placeholderId = `MMMMATH_${questionId}_${mathOptionContext[questionId]!.counter++}_MMMM`;
      mathOptionContext[questionId]!.map[placeholderId] = omml;
      questionsNode.push(new TextRun(placeholderId));
    }
    // Xử lý ảnh
    else if (part.startsWith("<image_") && part.endsWith(">")) {
      if (questionsNode.length > 0) {
        currentQuestion.push(new Paragraph({ children: questionsNode }));
        questionsNode = [];
      }

      const imageKey = part.replace(/[<>]/g, "");
      const imageURL = imageVars[imageKey];
      const base64 = imageCache[imageURL!];
      const buffer = Buffer.from(base64 as string, "base64");

      if (buffer) {
        const dimensions = imageSize(buffer);
        const width = Math.round(Math.min(dimensions.width, 151));
        const height = Math.round(
          (width * dimensions.height) / dimensions.width,
        );
        const imageType =
          dimensions.type === "jpg" ? "jpeg" : dimensions.type || "png";

        currentQuestion.push(
          new Paragraph({
            children: [
              new ImageRun({
                data: buffer,
                transformation: {
                  width: width,
                  height: height,
                },
                type: imageType as any,
              }),
            ],
            alignment: AlignmentType.CENTER,
          }),
        );
      }
    }
    // Text
    else {
      questionsNode.push(new TextRun({ text: part }));
    }
  });

  if (questionsNode.length > 0) {
    if (questionType === "Đúng sai") {
      const allBorders = {
        top: { style: BorderStyle.SINGLE, size: 6 },
        bottom: { style: BorderStyle.SINGLE, size: 6 },
        left: { style: BorderStyle.SINGLE, size: 6 },
        right: { style: BorderStyle.SINGLE, size: 6 },
      };

      const noBorders = {
        top: { style: BorderStyle.NONE },
        bottom: { style: BorderStyle.NONE },
        left: { style: BorderStyle.NONE },
        right: { style: BorderStyle.NONE },
      };

      const rightColumnTable = new Table({
        width: { size: 100, type: WidthType.PERCENTAGE },
        layout: TableLayoutType.FIXED,
        columnWidths: [450, 450, 450],
        borders: {
          top: { style: BorderStyle.NONE },
          bottom: { style: BorderStyle.NONE },
          left: { style: BorderStyle.NONE },
          right: { style: BorderStyle.NONE },
          insideVertical: { style: BorderStyle.NONE },
          insideHorizontal: { style: BorderStyle.NONE },
        },
        rows: [
          new TableRow({
            height: { value: 0, rule: HeightRule.AUTO },
            children: [
              new TableCell({
                width: { size: 34, type: WidthType.PERCENTAGE },
                borders: allBorders,
                children: [
                  new Paragraph({
                    text: "Đ",
                    alignment: AlignmentType.CENTER,
                  }),
                ],
                verticalAlign: VerticalAlign.CENTER,
              }),
              new TableCell({
                width: { size: 33, type: WidthType.PERCENTAGE },
                borders: allBorders,
                children: [new Paragraph({ text: "" })],
              }),
              new TableCell({
                width: { size: 33, type: WidthType.PERCENTAGE },
                borders: allBorders,
                children: [
                  new Paragraph({
                    text: "S",
                    alignment: AlignmentType.CENTER,
                  }),
                ],
                verticalAlign: VerticalAlign.CENTER,
              }),
            ],
          }),
        ],
      });

      currentQuestion.push(
        new Table({
          width: { size: 100, type: WidthType.PERCENTAGE },
          layout: TableLayoutType.FIXED,
          columnWidths: [7200, 450, 1350],
          borders: {
            top: { style: BorderStyle.NONE },
            bottom: { style: BorderStyle.NONE },
            left: { style: BorderStyle.NONE },
            right: { style: BorderStyle.NONE },
            insideVertical: { style: BorderStyle.NONE },
            insideHorizontal: { style: BorderStyle.NONE },
          },
          rows: [
            new TableRow({
              height: { value: 350, rule: HeightRule.ATLEAST },
              children: [
                // Cột 1: Nội dung
                new TableCell({
                  width: { size: 80, type: WidthType.PERCENTAGE },
                  borders: noBorders,
                  children: [
                    new Paragraph({
                      children: questionsNode,
                      alignment: AlignmentType.JUSTIFIED,
                    }),
                  ],
                  verticalAlign: VerticalAlign.CENTER,
                }),
                new TableCell({
                  width: { size: 5, type: WidthType.PERCENTAGE },
                  borders: noBorders,
                  children: [new Paragraph({ text: "" })],
                  verticalAlign: VerticalAlign.CENTER,
                }),
                // Cột 3: Ô Đ/S
                new TableCell({
                  width: { size: 15, type: WidthType.PERCENTAGE },
                  borders: noBorders,
                  children: [rightColumnTable],
                  verticalAlign: VerticalAlign.CENTER,
                }),
              ],
            }),
          ],
        }),
      );
    } else {
      currentQuestion.push(new Paragraph({ children: questionsNode }));
    }
  }

  return currentQuestion;
}
