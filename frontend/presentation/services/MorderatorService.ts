import axios from "axios";

export class ModeratorService {
  constructor() {}

  async parseDocument(subject: string, formData: FormData): Promise<string[]> {
    formData.append("subject", subject);

    const { data } = await axios.post<string[]>(
      "/api/moderator/parse",
      formData,
    );
    return data;
  }
}
