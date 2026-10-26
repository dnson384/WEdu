package com.wedu.exam_creation.questionReview.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "QuestionsReview")
public class QuestionReviewDocument {
    @Id
    private String id;

    @Builder.Default
    private List<RegeneratedQuestionDocument> regeneratedQuestions = new ArrayList<>();
}
