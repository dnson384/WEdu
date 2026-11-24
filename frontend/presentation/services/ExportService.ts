import axios from "axios";
import { ExportPayload, ExportMatrixPayload } from "../schemas/export.schema";

export class ExportService {
  constructor() {}

  async exportExam(payload: ExportPayload): Promise<Blob> {
    const response = await axios.post("/api/exporter/exam", payload, {
      responseType: "blob",
    });

    return response.data;
  }

  async exportMatrix(payload: ExportMatrixPayload): Promise<Blob> {
    const response = await axios.post<Blob>("/api/exporter/matrix", payload, { responseType: "blob" });
    return response.data;
  }
}
