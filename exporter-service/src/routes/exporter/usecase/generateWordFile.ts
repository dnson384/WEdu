import { Document, Packer, Paragraph, HeadingLevel, AlignmentType } from "docx";
import JSZip from "jszip";

import type {
  MathContext,
  MathOptionContext,
} from "../dto/request/MathContext.js";
import type { WordPayloadRequestDTO } from "../dto/request/WordPayloadRequestDTO.js";
import { QuestionBuilder } from "../infrastructure/builder/QuestionBuilder.js";
import { OptionBuilder } from "../infrastructure/builder/OptionBuilder.js";

export class GenerateWordFileUsecase {
  constructor() {}

  async execute(data: WordPayloadRequestDTO): Promise<Buffer> {
    const children: any[] = [];

    const regex = /(<(?:math|img)_\d+>)/g;

    const mathContext: MathContext = {
      map: {},
      counter: 0,
    };

    const imageQuestionCache: Record<string, string> =
      data.imageCache.questions;
    const imageOptionsCache: Record<string, string> = data.imageCache.options;

    // Tiêu đề
    children.push(
      new Paragraph({
        text: data.examName,
        heading: HeadingLevel.HEADING_1,
        alignment: AlignmentType.CENTER,
      }),
    );

    const mathOptionContext: MathOptionContext = {};

    for (const [index, section] of data.questionsSorted.entries()) {
      // Phần (Nhiều lựa chọn, Đúng sai, Trả lời ngắn)
      children.push(
        new Paragraph({
          text: `Phần ${index + 1}. ${section.questionType}`,
          heading: HeadingLevel.HEADING_2,
        }),
      );

      // Câu hỏi
      for (const [index, q] of section.questionsData.entries()) {
        const currentQuestion = QuestionBuilder(
          index,
          q.question,
          regex,
          imageQuestionCache,
          mathContext,
        );

        if (q.options.length === 0) {
          children.push(...currentQuestion);
          continue;
        }

        q.options.forEach((opt, index) => {
          const currentOption = OptionBuilder(
            q.id,
            index,
            section.questionType,
            opt,
            regex,
            imageOptionsCache,
            mathOptionContext,
          );

          currentQuestion.push(...currentOption);
        });

        children.push(...currentQuestion);
      }
    }

    const doc = new Document({
      styles: {
        default: {
          document: {
            run: {
              size: 26,
              font: "Times New Roman",
              color: "000000",
            },
            paragraph: {
              alignment: AlignmentType.JUSTIFIED,
              spacing: {
                before: 120,
                after: 120,
                line: 312,
                lineRule: "auto",
              },
              indent: { left: 0, right: 0 },
            },
          },
          heading1: {
            run: {
              size: 34,
              font: "Times New Roman",
              color: "000000",
              allCaps: true,
              bold: true,
            },
            paragraph: {
              alignment: AlignmentType.JUSTIFIED,
              spacing: {
                before: 0,
                after: 360,
                line: 312,
                lineRule: "auto",
              },
              indent: { left: 0, right: 0 },
            },
          },
          heading2: {
            run: {
              size: 30,
              font: "Times New Roman",
              color: "000000",
              bold: true,
            },
            paragraph: {
              alignment: AlignmentType.JUSTIFIED,
              spacing: {
                before: 120,
                after: 120,
                line: 312,
                lineRule: "auto",
              },
              indent: { left: 0, right: 0 },
            },
          },
        },
      },
      sections: [
        {
          properties: {
            page: {
              size: {
                width: 11907,
                height: 16840,
              },
              margin: {
                top: 1134,
                bottom: 1134,
                left: 1701,
                right: 851,
              },
            },
          },
          children: children,
        },
      ],
    });

    const rawBuffer = await Packer.toBuffer(doc);
    const zip = await JSZip.loadAsync(rawBuffer);
    let documentXml = await zip.file("word/document.xml")?.async("string");

    if (documentXml) {
      Object.entries(mathContext.map).forEach(([placeholderId, omml]) => {
        const runRegex = new RegExp(
          `<w:r\\b[^>]*>(?:(?!<\\/w:r>)[\\s\\S])*?${placeholderId}(?:(?!<\\/w:r>)[\\s\\S])*?<\\/w:r>`,
          "g",
        );

        documentXml = documentXml!.replace(runRegex, omml);
      });

      Object.entries(mathOptionContext).forEach(([questionId, context]) => {
        Object.entries(context.map).forEach(([placeholderId, omml]) => {
          const runRegex = new RegExp(
            `<w:r\\b[^>]*>(?:(?!<\\/w:r>)[\\s\\S])*?${placeholderId}(?:(?!<\\/w:r>)[\\s\\S])*?<\\/w:r>`,
            "g",
          );
          documentXml = documentXml!.replace(runRegex, omml);
        });
      });

      zip.file("word/document.xml", documentXml);
    }

    return await zip.generateAsync({
      type: "nodebuffer",
    });
  }
}
