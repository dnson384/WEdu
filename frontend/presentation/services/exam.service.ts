import {
  ExamDetailReponseEntity,
  ExamGeneratedResponseEntity,
  ExamResponseEntity,
} from "@/domain/entities/exam.entity";
import axios from "axios";
import { ExportPayload } from "../schemas/export.schema";

export async function GenerateExamService(
  draftId: string,
): Promise<ExamGeneratedResponseEntity> {
  const response = await axios.post<ExamGeneratedResponseEntity>(
    `/api/exam/generate`,
    {
      draftId: draftId,
    },
  );

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

export async function exportWordFileService(
  payload: ExportPayload,
): Promise<Blob> {
  const response = await axios.post("/api/exam/export", payload, {
    responseType: "blob",
  });

  return response.data;
}

export async function getRecentExamService(): Promise<ExamResponseEntity[]> {
  const { data } = await axios.get<ExamResponseEntity[]>(`/api/exam/recent`);
  return data;
}

export async function deleteExamService(examId: string): Promise<boolean> {
  const { data } = await axios.delete<boolean>(`/api/exam/delete/${examId}`);
  return data;
}
