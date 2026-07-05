package com.fckedu.exam_creation.importer.dto.parsed;

import java.util.List;

public class NewCategoryImporterDTO {

    private String chapter;
    private List<LessonDataImporterDTO> lessons;

    public NewCategoryImporterDTO() {
    }

    public NewCategoryImporterDTO(String chapter,
                                  List<LessonDataImporterDTO> lessons) {
        this.chapter = chapter;
        this.lessons = lessons;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public List<LessonDataImporterDTO> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonDataImporterDTO> lessons) {
        this.lessons = lessons;
    }
}
