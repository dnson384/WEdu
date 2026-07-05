import {
  ExamDetailReponseEntity,
  ExamResponseEntity,
} from "@/domain/entities/exam.entity";
import axios from "axios";
import { ExamExportPayload } from "../schemas/export.schema";

export async function GenerateExamService(draftId: string): Promise<boolean> {
  const response = await axios.post<boolean>(`/api/exam/generate`, {
    draftId: draftId,
  });

  return response.data;
}

export async function GetExamService(
  examId: string,
): Promise<ExamDetailReponseEntity> {
  const response = await axios.get<ExamDetailReponseEntity>(`/api/exam`, {
    params: {
      examId: examId,
    },
  });

  return response.data;
}

export async function getAllExamsService() {
  const response = await axios.get<ExamResponseEntity[]>(`/api/exam/all`);
  return response.data;
}

export async function exportWordFileService(payload: ExamExportPayload) {
  const response = await axios.post("/api/exam/export", payload, {
    responseType: "blob",
  });

  return response.data;
}

export async function getRecentExamService(): Promise<ExamResponseEntity[]> {
  const { data } = await axios.get<ExamResponseEntity[]>(`/api/exam/recent`);
  return data;
}
