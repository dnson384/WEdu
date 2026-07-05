import axios from "axios";
import { ICategoryRepository } from "@/domain/repositories/ICategoryRepository";
import { CategoryEntity } from "@/domain/entities/category.entity";

export class CategoryRepositoryImpl implements ICategoryRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async getAll(
    accessToken: string,
    refreshToken: string,
  ): Promise<CategoryEntity[]> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<CategoryEntity[]>(
      `${this.baseUrl}/category/all`,
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
