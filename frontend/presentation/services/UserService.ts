import axios from "axios";

export class UserService {
  constructor() {}

  async getMe() {
    const { data } = await axios.get("/api/user/me");
    return data;
  }

  async updateAvatar(s3Key: string): Promise<boolean> {
    const { data } = await axios.put<boolean>(`/api/user/update/avatar`, {
      s3Key,
    });

    return data;
  }
}
