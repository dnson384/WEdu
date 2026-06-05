import { CategoryEntity } from "@/domain/entities/category.entity";
import { ICategoryRepository } from "@/domain/repositories/ICategoryRepository";

export class GetAllCategoriesUsecase {
  constructor(private readonly categoryRepository: ICategoryRepository) {}

  async execute(): Promise<CategoryEntity[]> {
    return await this.categoryRepository.getAll();
  }
}
