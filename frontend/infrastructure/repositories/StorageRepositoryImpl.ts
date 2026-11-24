import { IStorageRepository } from "@/domain/repositories/IStorageRepository";
import axios from "axios";

export class StorageRepositoryImpl implements IStorageRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async uploadFileToQuestionBank(
    formData: FormData,
    accessToken: string,
  ): Promise<string> {
    const { data } = await axios.post<string>(
      `${this.baseUrl}/storage/question-file`,
      formData,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  public async uploadAvatar(
    formData: FormData,
    accessToken: string,
  ): Promise<string> {
    const { data } = await axios.post<string>(
      `${this.baseUrl}/storage/avatar`,
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
