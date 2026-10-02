package com.wedu.exam_creation.importer.dto.pandoc;

import java.util.List;

public class HeaderContent {
    Integer level;
    List<PandocEleOutput> content;

    public HeaderContent() {
    }

    public HeaderContent(Integer level, List<PandocEleOutput> content) {
        this.level = level;
        this.content = content;
    }

    public Integer getLevel() {
        return level;
    }

    public List<PandocEleOutput> getContent() {
        return content;
    }
}

