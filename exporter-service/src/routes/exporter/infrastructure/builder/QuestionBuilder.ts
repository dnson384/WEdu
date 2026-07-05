import { imageSize } from "image-size";
import { AlignmentType, ImageRun, Paragraph, TextRun } from "docx";

import type { MathContext } from "../../dto/request/MathContext.js";
import type { ContentDTO } from "../../dto/request/WordPayloadRequestDTO.js";
import { latexToOmml } from "../converter/latexToOmml.js";

export function QuestionBuilder(
  index: number,
  question: ContentDTO,
  regex: RegExp,
  imageCache: Record<string, string>,
  mathContext: MathContext,
): any[] {
  const currentQuestion: any[] = [];

  const template = question.template;
  const mathVars = question.variables.math;
  const imageVars = question.variables.image;

  let questionsNode: any[] = [];
  questionsNode.push(
    new TextRun({
      text: `Câu ${index + 1}. `,
      bold: true,
    }),
  );

  const parts = template.split(regex);

  parts.forEach((part) => {
    // Xử lý công thức toán học
    if (part.startsWith("<math_") && part.endsWith(">")) {
      const mathKey = part.replace(/[<>]/g, "");
      const latex = mathVars[mathKey];
      const omml = latexToOmml(latex!);
      const placeholderId = `MMMMATH_${mathContext.counter++}MMMM`;
      mathContext.map[placeholderId] = omml;
      questionsNode.push(new TextRun(placeholderId));
    }
    // Xử lý ảnh
    else if (part.startsWith("<img_") && part.endsWith(">")) {
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
    currentQuestion.push(new Paragraph({ children: questionsNode }));
  }

  return currentQuestion;
}
