import axios from "axios";
import { CategoryEntity } from "@/domain/entities/category.entity";

export async function getAllCategoriesService(): Promise<CategoryEntity[]> {
  const response = await axios.get<CategoryEntity[]>("/api/category/all");
  return response.data;
}

export async function getCategoryById(cateId: string) {}
