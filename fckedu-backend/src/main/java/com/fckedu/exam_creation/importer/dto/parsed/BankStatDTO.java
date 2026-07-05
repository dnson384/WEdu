package com.fckedu.exam_creation.importer.dto.parsed;

import java.util.ArrayList;
import java.util.List;

public class BankStatDTO {
    private String exerciseType;
    private List<String> difficultyLevels;
    private List<String> learningOutcomes;
    private String questionType;
    private int count;

    public BankStatDTO() {
        this.exerciseType = "";
        this.difficultyLevels = new ArrayList<String>();
        this.learningOutcomes = new ArrayList<String>();
        this.questionType = "";
        this.count = 1;
    }

    public BankStatDTO(String exerciseType,
                       List<String> difficultyLevels,
                       List<String> learningOutcomes,
                       String questionType,
                       int count) {
        this.exerciseType = exerciseType;
        this.difficultyLevels = difficultyLevels;
        this.learningOutcomes = learningOutcomes;
        this.questionType = questionType;
        this.count = count;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public List<String> getDifficultyLevels() {
        return difficultyLevels;
    }

    public void setDifficultyLevels(List<String> difficultyLevels) {
        this.difficultyLevels = difficultyLevels;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

