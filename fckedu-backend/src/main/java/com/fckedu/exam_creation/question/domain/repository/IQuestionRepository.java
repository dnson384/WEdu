package com.fckedu.exam_creation.question.domain.repository;

import com.fckedu.exam_creation.question.domain.entity.QuestionEntity;

import java.util.List;

public interface IQuestionRepository {
    void saveQuestions(List<QuestionEntity> questions);

    List<QuestionEntity> findByLessonIds(List<String> lessonIds);

    List<QuestionEntity> findByIds(List<String> ids);
}
