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

  async getAll(): Promise<CategoryEntity[]> {
    const { data } = await axios.get<CategoryEntity[]>(
      `${this.baseUrl}/category/all`,
    );
    return data;
  }
}
