package com.fckedu.exam_creation.common.dto.question.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private String id;
    private String categoryId;
    private String lessonId;
    private String exerciseType;
    private String difficultyLevel;
    private List<String> learningOutcomes;
    private String questionType;

    @Builder.Default
    private ContentDTO question = new ContentDTO();

    @Builder.Default
    private List<ContentDTO> options = new ArrayList<>();
}