package com.fckedu.exam_creation.importer.dto.parsed;

import java.util.List;

public class ParsedDataOutput {

    private List<NewQuestionImporterDTO> questions;
    private NewCategoryImporterDTO category;

    public ParsedDataOutput() {
    }

    public ParsedDataOutput(List<NewQuestionImporterDTO> questions,
                            NewCategoryImporterDTO category) {
        this.questions = questions;
        this.category = category;
    }

    public List<NewQuestionImporterDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<NewQuestionImporterDTO> questions) {
        this.questions = questions;
    }

    public NewCategoryImporterDTO getCategory() {
        return category;
    }

    public void setCategory(NewCategoryImporterDTO category) {
        this.category = category;
    }
}