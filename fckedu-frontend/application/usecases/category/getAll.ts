import { CategoryEntity } from "@/domain/entities/category.entity";
import { ICategoryRepository } from "@/domain/repositories/ICategoryRepository";

export class GetAllCategoriesUsecase {
  constructor(private readonly categoryRepository: ICategoryRepository) {}

  async execute(
    accessToken: string,
    refreshToken: string,
  ): Promise<CategoryEntity[]> {
    return await this.categoryRepository.getAll(accessToken, refreshToken);
  }
}
