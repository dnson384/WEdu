package com.fckedu.exam_creation.category.usecase;

import com.fckedu.exam_creation.category.domain.entity.CategoryEntity;
import com.fckedu.exam_creation.category.domain.repository.ICategoryRepository;
import com.fckedu.exam_creation.category.dto.mapper.CategoryDTOMapper;
import com.fckedu.exam_creation.common.dto.category.response.ChapterResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChapterUsecase {
    private final ICategoryRepository repo;
    private final CategoryDTOMapper mapper;

    public ChapterUsecase(ICategoryRepository repo, CategoryDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<ChapterResponseDTO> getAll() {
        List<CategoryEntity> categoryEntities = repo.getAll();
        return categoryEntities.stream()
                .map(mapper::entityToDTO)
                .toList();
    }
}
