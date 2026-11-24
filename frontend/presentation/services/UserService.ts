import axios from "axios";
import { UserEntity } from "@/domain/entities/user.entity";

export class UserService {
  constructor() {}

  async getMe(): Promise<UserEntity> {
    const { data } = await axios.get<UserEntity>("/api/user/me");
    return data;
  }

  async updateUsername(username: string): Promise<boolean> {
    const { data } = await axios.put<boolean>("/api/user/update/username", {
      username,
    });
    return data;
  }

  async updateAvatar(s3Key: string): Promise<boolean> {
    const { data } = await axios.put<boolean>(`/api/user/update/avatar`, {
      s3Key,
    });

    return data;
  }
}
