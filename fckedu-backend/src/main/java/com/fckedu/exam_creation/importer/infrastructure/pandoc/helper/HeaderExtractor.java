package com.fckedu.exam_creation.importer.infrastructure.pandoc.helper;

import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.ExtractedHeaderResult;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.HeaderContent;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.PandocEleOutput;

import java.util.List;

public class HeaderExtractor {
    public static ExtractedHeaderResult execute(HeaderContent headerContent) {
        List<PandocEleOutput> content = headerContent.getContent();
        Integer level = headerContent.getLevel();

        StringBuilder rawText = new StringBuilder();

        for (PandocEleOutput ele : content) {
            if ("Str".equals(ele.getT()) && ele.getC() != null) {
                rawText.append(ele.getC());
            } else if ("Space".equals(ele.getT()) && ele.getC() == null) {
                rawText.append(" ");
            }
        }

        return new ExtractedHeaderResult(level, rawText.toString().trim());
    }
}
