package com.fckedu.exam_creation.exam.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExamDocument {
    private String questionType;
    private String difficultyLevel;

    @Field(targetType = FieldType.OBJECT_ID)
    private List<String> questionIds;
}
