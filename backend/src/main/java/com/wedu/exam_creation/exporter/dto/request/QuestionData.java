package com.wedu.exam_creation.exporter.dto.request;

import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionData {
    private String id;
    private ContentDTO question;
    private List<ContentDTO> options;
}
