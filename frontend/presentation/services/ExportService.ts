import axios from "axios";
import { ExportPayload } from "../schemas/export.schema";

export class ExportService {
  constructor() {}

  async exportExam(payload: ExportPayload): Promise<Blob> {
    const response = await axios.post("/api/exporter/exam", payload, {
      responseType: "blob",
    });

    return response.data;
  }
}
