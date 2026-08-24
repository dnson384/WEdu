package com.wedu.exam_creation.common.dto.chapter.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponseDTO {
    private String id;
    private String subject;
    private String name;
    private List<LessonDataResponseDTO> lessons;
}
