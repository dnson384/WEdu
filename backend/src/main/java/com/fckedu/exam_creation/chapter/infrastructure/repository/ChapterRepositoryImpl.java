package com.fckedu.exam_creation.chapter.infrastructure.repository;

import com.fckedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.fckedu.exam_creation.chapter.domain.repository.IChapterRepository;
import com.fckedu.exam_creation.chapter.infrastructure.document.BankStatDocument;
import com.fckedu.exam_creation.chapter.infrastructure.document.ChapterDocument;
import com.fckedu.exam_creation.chapter.infrastructure.document.LessonDataDocument;
import com.fckedu.exam_creation.chapter.infrastructure.mapper.ChapterMapper;
import com.fckedu.exam_creation.common.dto.chapter.response.SavedChapterResponse;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ChapterRepositoryImpl implements IChapterRepository {
    private final MongoTemplate mongoTemplate;
    private final ChapterMapper chapterMapper;

    public ChapterRepositoryImpl(MongoTemplate mongoTemplate, ChapterMapper chapterMapper) {
        this.mongoTemplate = mongoTemplate;
        this.chapterMapper = chapterMapper;
    }

    @Override
    public SavedChapterResponse saveChapter(ChapterEntity chapter) {
        ChapterDocument chapterDocument = chapterMapper.toDocument(chapter);

        // Tìm kiếm chương đã tồn tại
        Query query = new Query(Criteria.where("name").is(chapter.getName()));
        ChapterDocument existedChapter = mongoTemplate.findOne(query, ChapterDocument.class);

        if (existedChapter == null) {
            chapterDocument.setCreateAt(LocalDateTime.now());
            chapterDocument.setUpdatedAt(LocalDateTime.now());
            chapterDocument.getLessons().get(0).setId(new ObjectId().toString());
            ChapterDocument created = mongoTemplate.save(chapterDocument);

            return new SavedChapterResponse(created.getId(), created.getLessons().get(0).getId());
        }

        // Tìm bài học trong chương đã tồn tại
        LessonDataDocument newLesson = chapterDocument.getLessons().get(0);
        int existingLessonIndex = -1;

        for (int index = 0; index < existedChapter.getLessons().size(); index++) {
            if (existedChapter.getLessons().get(index).getName().equals(chapter.getLessons().get(0).getName())) {
                existingLessonIndex = index;
                break;
            }
        }

        String targetLessonId;

        if (existingLessonIndex == -1) {
            newLesson.setId(new ObjectId().toString());
            existedChapter.getLessons().add(newLesson);
            existedChapter.setUpdatedAt(LocalDateTime.now());
            mongoTemplate.save(existedChapter);

            int lastIndex = existedChapter.getLessons().size() - 1;
            targetLessonId = existedChapter.getLessons().get(lastIndex).getId();
        } else {
            LessonDataDocument existingLesson = existedChapter.getLessons().get(existingLessonIndex);
            List<BankStatDocument> newBankStats = newLesson.getBankStats();

            if (newBankStats != null && !newBankStats.isEmpty()) {
                for (BankStatDocument newStat : newBankStats) {
                    Optional<BankStatDocument> existingStatOpt = existingLesson.getBankStats().stream()
                            .filter(oldStat ->
                                    oldStat.getExerciseType().equals(newStat.getExerciseType()) &&
                                            oldStat.getQuestionType().equals(newStat.getQuestionType()) &&
                                            oldStat.getDifficultyLevels().equals(newStat.getDifficultyLevels()) &&
                                            oldStat.getLearningOutcomes().equals(newStat.getLearningOutcomes())
                            ).findFirst();

                    if (existingStatOpt.isPresent()) {
                        BankStatDocument existingStat = existingStatOpt.get();
                        existingStat.setCount(existingStat.getCount() + newStat.getCount());
                    } else {
                        existingLesson.getBankStats().add(newStat);
                    }
                }
                existedChapter.setUpdatedAt(LocalDateTime.now());
                mongoTemplate.save(existedChapter);
            }

            targetLessonId = existingLesson.getId();
        }

        return new SavedChapterResponse(existedChapter.getId(), targetLessonId);
    }

    @Override
    public List<ChapterEntity> getAll() {
        List<ChapterDocument> categories = mongoTemplate.findAll(ChapterDocument.class);
        return categories.stream()
                .map(chapterMapper::toEntity)
                .toList();
    }

    @Override
    public ChapterEntity getById(String chapterId) {
        ChapterDocument chapter = mongoTemplate.findById(chapterId, ChapterDocument.class);
        if (chapter == null) {
            throw new NotFoundException("Không tồn tại chương này");
        }

        return chapterMapper.toEntity(chapter);
    }

    @Override
    public List<ChapterEntity> getByIds(List<String> chapterIds) {
        Query query = new Query(Criteria.where("_id").in(chapterIds));
        List<ChapterDocument> chapters = mongoTemplate.find(query, ChapterDocument.class);

        if (chapters.isEmpty()) {
            throw new NotFoundException("Không tồn tại chương này");
        }

        return chapters.stream()
                .map(chapterMapper::toEntity)
                .toList();
    }

}
