import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";
import axios from "axios";

export class ModeratorRepositoryImpl implements IModeratorRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async parse(formData: FormData, accessToken: string): Promise<string[]> {
    const { data } = await axios.post<string[]>(
      `${this.baseUrl}/moderator/parse`,
      formData,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "multipart/form-data",
        },
      },
    );
    return data;
  }
}
