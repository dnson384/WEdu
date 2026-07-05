import { IUploadDocxFileRepository } from "@/domain/repositories/IUploadFileRepository";
import axios from "axios";

export class UploadDocxFileRepositoryImpl implements IUploadDocxFileRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async uploadDocxFile(
    subject: string,
    formData: FormData,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    if (!formData.has("subject")) {
      formData.append("subject", subject);
    }

    const { data } = await axios.post<boolean>(
      `${this.baseUrl}/importer/parse`,
      formData,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }
}
