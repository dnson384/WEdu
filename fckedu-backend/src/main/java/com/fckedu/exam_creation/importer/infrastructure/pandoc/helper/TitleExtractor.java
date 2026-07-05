package com.fckedu.exam_creation.importer.infrastructure.pandoc.helper;

import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.PandocEleOutput;

import java.util.List;


public class TitleExtractor {
    public static String execute(List<PandocEleOutput> content) {
        StringBuilder rawText = new StringBuilder();

        for (PandocEleOutput ele : content) {
            if ("Str".equals(ele.getT()) && ele.getC() != null) {
                rawText.append(ele.getC());
            } else if ("Space".equals(ele.getT()) && ele.getC() == null) {
                rawText.append(" ");
            }
        }

        return rawText.toString().trim();
    }
}
