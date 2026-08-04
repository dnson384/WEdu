package com.fckedu.exam_creation.exporter.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordPayloadRequestDTO {
    private String examName;
    private ImageCacheData imageCache;
    private List<QuestionsSortedData> questionsSorted;
}
