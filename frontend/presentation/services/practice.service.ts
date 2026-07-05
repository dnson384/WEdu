import {
  PracticeDetailEntity,
  PracticeEntity,
} from "@/domain/entities/practice.entity";
import axios, { isAxiosError } from "axios";
import { LessonExportPayload } from "../schemas/export.schema";

export async function getAllPracticesService(): Promise<PracticeEntity[]> {
  const response =
    await axios.get<Promise<PracticeEntity[]>>("/api/practice/all");
  return response.data;
}

export async function getPracticeDetailByIdService(
  id: string,
): Promise<PracticeDetailEntity> {
  const response = await axios.get<PracticeDetailEntity>("/api/practice", {
    params: { id: id },
  });
  return response.data;
}

export async function exportWordFile(
  title: string,
  questionsSorted: LessonExportPayload,
) {
  const response = await axios.post(
    "/api/export/word",
    { title: title, questionsSorted: questionsSorted },
    {
      responseType: "blob",
    },
  );

  return response.data;
}
