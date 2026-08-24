package com.wedu.exam_creation.common.dto.question;

import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
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
    private String chapterId;
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
