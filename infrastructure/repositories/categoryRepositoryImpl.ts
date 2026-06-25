import axios from "axios";
import { ICategoryRepository } from "@/domain/repositories/ICategoryRepository";
import { CategoryEntity } from "@/domain/entities/category.entity";
import { cookies } from "next/headers";

export class CategoryRepositoryImpl implements ICategoryRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async getAll(): Promise<CategoryEntity[]> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<CategoryEntity[]>(
      `${this.baseUrl}/category/all`,
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
