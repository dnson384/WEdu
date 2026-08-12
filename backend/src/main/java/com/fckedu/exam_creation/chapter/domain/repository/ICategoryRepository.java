package com.fckedu.exam_creation.category.domain.repository;

import com.fckedu.exam_creation.category.domain.entity.CategoryEntity;
import com.fckedu.exam_creation.common.dto.category.response.SavedCategoryResponse;

import java.util.List;

public interface ICategoryRepository {
    SavedCategoryResponse saveCategory(CategoryEntity category);

    List<CategoryEntity> getAll();

    CategoryEntity getById(String chapterId);

    List<CategoryEntity> getByIds(List<String> chapterIds);
}