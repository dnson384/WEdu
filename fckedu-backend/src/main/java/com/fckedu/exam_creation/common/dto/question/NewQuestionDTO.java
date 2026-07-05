package com.fckedu.exam_creation.common.dto.question;

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
public class NewQuestionDTO {
    @Builder.Default
    private String id = null;

    private String categoryId;
    private String lessonId;
    private String exerciseType;
    private String difficultyLevel;
    private List<String> learningOutcomes;
    private String questionType;

    @Builder.Default
    private NewQuestionContentDTO question = new NewQuestionContentDTO();

    @Builder.Default
    private List<NewOptionDataDTO> options = new ArrayList<>();
}
