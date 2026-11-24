import type { Request, Response } from "express";
import type { WordPayloadRequestDTO } from "../dto/request/WordPayloadRequestDTO.js";
import { GenerateWordFileUsecase } from "../usecase/generateWordFile.js";
import { isMatrixWordPayload } from "../dto/request/MatrixWordPayloadRequestDTO.js";
import { GenerateMatrixWordFileUsecase } from "../usecase/generateMatrixWordFile.js";

export async function exportMatrixAsWord(req: Request, res: Response) {
  const body: unknown = req.body;
  if (!isMatrixWordPayload(body)) {
    return res.status(400).json({ message: "Dữ liệu ma trận đề không hợp lệ" });
  }
  try {
    const buffer = await new GenerateMatrixWordFileUsecase().execute(body);
    const fileName = encodeURIComponent(`${body.examName}_ma trận đề.docx`);
    return res
      .set("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
      .set("Content-Disposition", `attachment; filename*=UTF-8''${fileName}`)
      .set("Cache-Control", "private, no-store")
      .send(buffer);
  } catch {
    return res.status(500).json({ message: "Không thể tạo file ma trận đề" });
  }
}

export async function exportAsWord(req: Request, res: Response) {
  const body: WordPayloadRequestDTO = req.body;

  const usecase = new GenerateWordFileUsecase();

  const buffer: Buffer = await usecase.execute(body);

  const fileName = encodeURIComponent(body.examName || "export") + ".docx";
  res.setHeader(
    "Content-Type",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  );
  res.setHeader(
    "Content-Disposition",
    `attachment; filename*=UTF-8''${fileName}`,
  );

  return res.send(buffer);
}
