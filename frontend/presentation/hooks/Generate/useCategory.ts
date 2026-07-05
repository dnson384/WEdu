import { CategoryEntity } from "@/domain/entities/category.entity";
import { getAllCategoriesService } from "@/presentation/services/category.service";
import { useEffect, useState } from "react";

export default function useCategory() {
  const [categories, setCategories] = useState<CategoryEntity[]>([]);

  useEffect(() => {
    const fetchCategories = async () => {
      const categories = await getAllCategoriesService();
      if (categories) setCategories(categories);
    };

    fetchCategories();
  }, []);

  return {
    categories,
  };
}
