import { CategoryEntity } from "../entities/category.entity";

export interface ICategoryRepository {
  getAll(accessToken: string, refreshToken: string): Promise<CategoryEntity[]>;
}
