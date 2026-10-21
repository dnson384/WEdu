import { ExamGeneratedResponseEntity } from "@/domain/entities/exam.entity";
import axios from "axios";

export class ExamService {
  async generate(draftId: string): Promise<ExamGeneratedResponseEntity> {
    const { data } = await axios.post<ExamGeneratedResponseEntity>(
      `/api/exam/generate/${draftId}`,
    );

    return data;
  }

}
