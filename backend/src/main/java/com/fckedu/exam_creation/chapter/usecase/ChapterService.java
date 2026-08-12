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
public class ChapterService {
    private final IChapterRepository repo;
    private final ChapterDTOMapper mapper;

    public ChapterService(IChapterRepository repo, ChapterDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public SavedChapterResponse insert(NewChapterDTO chapter) {
        ChapterEntity newChapterEntity = new ChapterEntity(
                null,
                chapter.getSubject(),
                chapter.getName(),
                chapter.getLessons().stream().map(mapper::newLessonDTOToEntity).toList()
        );

        return repo.saveChapter(newChapterEntity);
    }

    public List<ChapterResponseDTO> getByIds(List<String> chapterIds) {
        List<ChapterEntity> chapterEntities = repo.getByIds(chapterIds);

        return chapterEntities.stream()
                .map(chapter -> new ChapterResponseDTO(
                        chapter.getId(),
                        chapter.getSubject(),
                        chapter.getName(),
                        chapter.getLessons().stream()
                                .map(mapper::lessonEntityToDTO)
                                .toList())
                )
                .toList();
    }
}
