import { AlignmentType, Document, Packer, PageOrientation, Paragraph, TextRun } from "docx";
import type { MatrixWordPayloadRequestDTO } from "../dto/request/MatrixWordPayloadRequestDTO.js";
import { buildMatrixDetailsTable, buildMatrixTable } from "../infrastructure/builder/MatrixTableBuilder.js";
import { prepareMatrixExport } from "./util/MatrixExportData.js";

export class GenerateMatrixWordFileUsecase {
  async execute(data: MatrixWordPayloadRequestDTO): Promise<Buffer> {
    const report = prepareMatrixExport(data);
    const title = (text: string, pageBreakBefore = false) => new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 120 },
      keepNext: true,
      pageBreakBefore,
      children: [new TextRun({ text, bold: true })],
    });
    const document = new Document({
      styles: {
        default: {
          document: {
            run: { font: "Arial", size: 24, color: "000000" },
            paragraph: { spacing: { before: 0, after: 160, line: 278 } },
          },
        },
      },
      sections: [{
        properties: {
          page: {
            size: { width: 12240, height: 15840, orientation: PageOrientation.LANDSCAPE },
            margin: { top: 1440, right: 1440, bottom: 1440, left: 1440, header: 720, footer: 720 },
          },
        },
        children: [
          title("1. MA TRẬN ĐỀ KIỂM TRA ĐỊNH KỲ"),
          buildMatrixTable(report),
          title("2. BẢN ĐẶC TẢ ĐỀ KIỂM TRA ĐỊNH KỲ", true),
          buildMatrixDetailsTable(report),
        ],
      }],
    });
    return Packer.toBuffer(document);
  }
}
