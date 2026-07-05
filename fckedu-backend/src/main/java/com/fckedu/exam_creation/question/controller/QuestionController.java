package com.fckedu.exam_creation.question.controller;

import com.fckedu.exam_creation.question.usecase.QuestionUsecase;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/question")
public class QuestionController {
    private final QuestionUsecase questionUsecase;

    public QuestionController(QuestionUsecase questionUsecase) {
        this.questionUsecase = questionUsecase;
    }
}
