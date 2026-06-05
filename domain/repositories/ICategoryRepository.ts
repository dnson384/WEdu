import { CategoryEntity } from "../entities/category.entity";

export interface ICategoryRepository {
  getAll(): Promise<CategoryEntity[]>;
}
