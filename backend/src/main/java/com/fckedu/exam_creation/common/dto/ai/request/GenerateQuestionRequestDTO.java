package com.fckedu.exam_creation.common.dto.ai.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionRequestDTO {
    @NotNull
    @Min(1)
    @Max(50)
    private Integer numberOfQuestions;

    @NotBlank(message = "Id chương không được để trống")
    private String chapterId;

    @NotBlank(message = "Chương không được để trống")
    private String chapter;

    @NotBlank(message = "Id bài không được để trống")
    private String lessonId;

    @NotBlank(message = "Bài không được để trống")
    private String lesson;

    @NotBlank(message = "Dạng bài không được để trống")
    private String exerciseType;

    @NotBlank(message = "Loại câu hỏi không được để trống")
    @Pattern(
            regexp = "Nhiều lựa chọn|Đúng sai|Trả lời ngắn",
            message = "Loại câu hỏi phải là Nhiều lựa chọn, Đúng sai hoặc Trả lời ngắn"
    )
    private String questionType;

    @NotBlank(message = "Độ khó không được để trống")
    @Pattern(
            regexp = "Nhận biết|Thông hiểu|Vận dụng|Vận dụng cao",
            message = "Độ khó phải là Nhận biết, Thông hiểu, Vận dụng, Vận dụng cao"
    )
    private String difficultyLevel;

    @NotBlank(message = "Yêu cầu cần đạt không được để trống")
    private String learningOutcome;
}
