package com.wedu.exam_creation.question.domain.repository;

import com.wedu.exam_creation.question.domain.entity.QuestionEntity;

import java.util.List;

public interface IQuestionRepository {
    List<String> saveQuestions(List<QuestionEntity> questions);

    List<QuestionEntity> findByLessonIds(List<String> lessonIds);

    List<QuestionEntity> findByIds(List<String> ids);
}
