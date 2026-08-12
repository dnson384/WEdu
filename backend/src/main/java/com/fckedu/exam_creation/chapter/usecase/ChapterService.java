package com.fckedu.exam_creation.chapter.usecase;

import com.fckedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.fckedu.exam_creation.chapter.domain.repository.IChapterRepository;
import com.fckedu.exam_creation.chapter.dto.mapper.ChapterDTOMapper;
import com.fckedu.exam_creation.common.dto.chapter.NewChapterDTO;
import com.fckedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.fckedu.exam_creation.common.dto.chapter.response.SavedChapterResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final IChapterRepository repo;
    private final ChapterDTOMapper mapper;

    public CategoryService(IChapterRepository repo, ChapterDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public SavedChapterResponse insert(NewChapterDTO category) {
        ChapterEntity newCategoryEntity = new ChapterEntity(
                null,
                category.getSubject(),
                category.getName(),
                category.getLessons().stream().map(mapper::newLessonDTOToEntity).toList()
        );

        return repo.saveCategory(newCategoryEntity);
    }

    public List<ChapterResponseDTO> getByIds(List<String> chapterIds) {
        List<ChapterEntity> categories = repo.getByIds(chapterIds);

        return categories.stream()
                .map(category -> new ChapterResponseDTO(
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
