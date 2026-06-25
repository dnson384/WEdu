import { IUploadDocxFileRepository } from "@/domain/repositories/IUploadFileRepository";
import axios from "axios";
import { cookies } from "next/headers";

export class UploadDocxFileRepositoryImpl implements IUploadDocxFileRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async uploadDocxFile(subject: string, formData: FormData): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    if (!formData.has("subject")) {
      formData.append("subject", subject);
    }

    const { data } = await axios.post<boolean>(
      `${this.baseUrl}/importer/parse`,
      formData,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }
}
