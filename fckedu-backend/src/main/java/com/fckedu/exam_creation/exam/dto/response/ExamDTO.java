package com.fckedu.exam_creation.exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamDTO {
    private String id;
    private String name;
    private List<ExamChapterDTO> chapters;
    private Integer questionsCount;
}
