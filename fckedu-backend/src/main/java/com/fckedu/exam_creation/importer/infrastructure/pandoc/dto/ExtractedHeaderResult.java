package com.fckedu.exam_creation.importer.infrastructure.pandoc.dto;

public class ExtractedHeaderResult {
    private Integer level;
    private String text;

    public ExtractedHeaderResult() {
    }

    public ExtractedHeaderResult(Integer level, String text) {
        this.level = level;
        this.text = text;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
