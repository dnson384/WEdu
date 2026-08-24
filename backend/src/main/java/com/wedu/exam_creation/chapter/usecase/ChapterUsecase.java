package com.wedu.exam_creation.chapter.usecase;

import com.wedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.wedu.exam_creation.chapter.domain.repository.IChapterRepository;
import com.wedu.exam_creation.chapter.dto.mapper.ChapterDTOMapper;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ChapterUsecase {
    private final IChapterRepository repo;
    private final ChapterDTOMapper mapper;

    public ChapterUsecase(IChapterRepository repo, ChapterDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<ChapterResponseDTO> getAll() {
        List<ChapterEntity> chapterEntitys = repo.getAll();
        return chapterEntitys.stream()
                .map(mapper::entityToDTO)
                .toList();
    }
}
