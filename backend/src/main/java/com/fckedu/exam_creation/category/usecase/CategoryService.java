package com.fckedu.exam_creation.category.usecase;

import com.fckedu.exam_creation.category.domain.entity.CategoryEntity;
import com.fckedu.exam_creation.category.domain.repository.ICategoryRepository;
import com.fckedu.exam_creation.category.dto.mapper.CategoryDTOMapper;
import com.fckedu.exam_creation.common.dto.category.NewCategoryDTO;
import com.fckedu.exam_creation.common.dto.category.response.CategoryResponseDTO;
import com.fckedu.exam_creation.common.dto.category.response.SavedCategoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final ICategoryRepository repo;
    private final CategoryDTOMapper mapper;

    public CategoryService(ICategoryRepository repo, CategoryDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public SavedCategoryResponse insert(NewCategoryDTO category) {
        CategoryEntity newCategoryEntity = new CategoryEntity(
                null,
                category.getSubject(),
                category.getChapter(),
                category.getLessons().stream().map(mapper::newLessonDTOToEntity).toList()
        );

        return repo.saveCategory(newCategoryEntity);
    }

    public List<CategoryResponseDTO> getByIds(List<String> chapterIds) {
        List<CategoryEntity> categories = repo.getByIds(chapterIds);

        return categories.stream()
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getSubject(),
                        category.getChapter(),
                        category.getLessons().stream()
                                .map(mapper::lessonEntityToDTO)
                                .toList())
                )
                .toList();
    }
}
