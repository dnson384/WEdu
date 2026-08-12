package com.fckedu.exam_creation.chapter.infrastructure.document;

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
public class LessonDataDocument {
    private String id;
    private String name;

    @Builder.Default
    private List<BankStatDocument> bankStats = new ArrayList<>();
}
