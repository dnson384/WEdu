import type { Request, Response } from "express";
import type { WordPayloadRequestDTO } from "../dto/request/WordPayloadRequestDTO.js";
import { GenerateWordFileUsecase } from "../usecase/generateWordFile.js";

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
