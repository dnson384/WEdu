package com.fckedu.exam_creation.exam.domain.repository;

import com.fckedu.exam_creation.exam.domain.entity.ExamEntity;

import java.util.List;

public interface IExamRepository {
    String saveExam(ExamEntity payload);

    ExamEntity getExamById(String examId);

    List<ExamEntity> getAllUserExams(String userId);

    List<ExamEntity> getRecentExams(String userId);
}
