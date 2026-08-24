package com.wedu.exam_creation.common.dto.exam.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailDTO {
    private String id;
    private String userId;
    private String name;
    private List<ExamQuestionDTO> groups;
}
