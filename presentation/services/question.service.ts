import axios from "axios";
import { GeneratePayloadSchema } from "../schemas/generate";

export async function generatePracticeService(
  payload: GeneratePayloadSchema,
): Promise<string> {
  const response = await axios.post<string>(
    "/api/practice/generate",
    payload,
  );
  return response.data;
}
