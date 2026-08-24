package com.wedu.exam_creation.exporter.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionsSortedData {
    private String questionType;
    private List<QuestionData> questionsData;
}
