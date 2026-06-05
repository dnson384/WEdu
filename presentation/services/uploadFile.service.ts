import axios from "axios";

export async function uploadDocxFile(
  subject: string,
  formData: FormData,
): Promise<boolean> {
  formData.append("subject", subject);
  const response = await axios.post<boolean>("/api/uploadFile", formData);
  return response.data;
}
