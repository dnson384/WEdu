package com.fckedu.exam_creation.chapter.domain.repository;

import com.fckedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.fckedu.exam_creation.common.dto.chapter.response.SavedChapterResponse;

import java.util.List;

public interface IChapterRepository {
    SavedChapterResponse saveChapter(ChapterEntity chapter);

    List<ChapterEntity> getAll();

    ChapterEntity getById(String chapterId);

    List<ChapterEntity> getByIds(List<String> chapterIds);
}
