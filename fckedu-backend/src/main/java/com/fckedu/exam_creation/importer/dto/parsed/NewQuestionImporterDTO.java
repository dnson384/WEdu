package com.fckedu.exam_creation.importer.dto.parsed;

import java.util.ArrayList;
import java.util.List;

public class NewQuestionImporterDTO {

    private String exerciseType;
    private String difficultyLevel;
    private List<String> learningOutcomes;
    private String questionType;
    private QuestionContentImporterDTO question;
    private List<OptionsDataImporterDTO> options;

    public NewQuestionImporterDTO() {
        this.exerciseType = "";
        this.difficultyLevel = "";
        this.learningOutcomes = new ArrayList<String>();
        this.questionType = "";
        this.question = new QuestionContentImporterDTO();
        this.options = new ArrayList<OptionsDataImporterDTO>();
    }

    public NewQuestionImporterDTO(String exerciseType,
                                  String difficultyLevel,
                                  List<String> learningOutcomes,
                                  String questionType,
                                  QuestionContentImporterDTO question,
                                  List<OptionsDataImporterDTO> options) {
        this.exerciseType = exerciseType;
        this.difficultyLevel = difficultyLevel;
        this.learningOutcomes = learningOutcomes;
        this.questionType = questionType;
        this.question = question;
        this.options = options;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public List<String> getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(List<String> learningOutcomes) {
        this.learningOutcomes = learningOutcomes;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public QuestionContentImporterDTO getQuestion() {
        return question;
    }

    public void setQuestion(QuestionContentImporterDTO question) {
        this.question = question;
    }

    public List<OptionsDataImporterDTO> getOptions() {
        return options;
    }

    public void setOptions(List<OptionsDataImporterDTO> options) {
        this.options = options;
    }
}
