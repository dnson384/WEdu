import axios from "axios";

export class StorageService {
  constructor() {}

  async uploadAvatar(file: File): Promise<string> {
    const formData = new FormData();
    formData.append("file", file);

    const { data } = await axios.post<string>("/api/storage/avatar", formData);

    return data;
  }

  async uploadFileToQuestionBank(
    subject: string,
    formData: FormData,
  ): Promise<boolean> {
    formData.append("subject", subject);
    
    const { data } = await axios.post<boolean>(
      "/api/storage/question-bank",
      formData,
    );
    return data;
  }
}
