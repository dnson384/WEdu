package com.wedu.exam_creation.common.dto.chapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewChapterDTO {
    private String subject;
    private String name;
    private List<NewLessonDataDTO> lessons;
}
