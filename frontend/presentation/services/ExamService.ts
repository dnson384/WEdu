import {
  ExamDetailReponseEntity,
  ExamGeneratedResponseEntity,
  ExamResponseEntity,
} from "@/domain/entities/exam.entity";
import axios from "axios";

export class ExamService {
  async generate(draftId: string): Promise<ExamGeneratedResponseEntity> {
    const { data } = await axios.post<ExamGeneratedResponseEntity>(
      `/api/exam/generate/${draftId}`,
    );

    return data;
  }

  async get(examId: string): Promise<ExamDetailReponseEntity> {
    const { data } = await axios.get<ExamDetailReponseEntity>(
      `/api/exam/${examId}`,
    );

    return data;
  }

  async getAllExams() {
    const { data } =
      await axios.get<ExamResponseEntity[]>(`/api/exam/user-exams`);

    return data;
  }

  async getRecent(): Promise<ExamResponseEntity[]> {
    const { data } = await axios.get<ExamResponseEntity[]>(`/api/exam/recent`);
    return data;
  }

  async delete(examId: string): Promise<boolean> {
    const { data } = await axios.delete<boolean>(`/api/exam/${examId}/delete`);
    return data;
  }
}
