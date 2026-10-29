package com.wedu.exam_creation.question.domain.repository;

import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import java.time.Instant;

import java.util.List;

public interface IQuestionRepository {
    QuestionEntity findById(String id);

    boolean updatePendingContent(String id, ContentEntity question, List<ContentEntity> options, Instant expectedUpdatedAt);

    List<QuestionEntity> findPending(
            int page, String search, String questionType, List<String> chapterIds, List<String> lessonIds);

    long countPending(String search, String questionType, List<String> chapterIds, List<String> lessonIds);

    List<String> saveQuestions(List<QuestionEntity> questions);

    List<QuestionEntity> findByLessonIds(List<String> lessonIds);

    List<QuestionEntity> findByIds(List<String> ids);
}
