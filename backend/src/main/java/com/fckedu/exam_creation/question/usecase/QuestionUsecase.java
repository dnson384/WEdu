package com.fckedu.exam_creation.question.usecase;

import com.fckedu.exam_creation.question.domain.repository.IQuestionRepository;
import org.springframework.stereotype.Service;

@Service
public class QuestionUsecase {
    private final IQuestionRepository repo;

    public QuestionUsecase(IQuestionRepository repo) {
        this.repo = repo;
    }
}
