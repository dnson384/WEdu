package com.wedu.exam_creation.exam.dto.mapper;

import com.wedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamQuestionDTO;
import com.wedu.exam_creation.common.dto.exam.response.QuestionDetailResponseDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.exam.domain.entity.ChapterExamEntity;
import com.wedu.exam_creation.exam.domain.entity.ExamEntity;
import com.wedu.exam_creation.exam.dto.response.ExamChapterDTO;
import com.wedu.exam_creation.exam.dto.response.ExamDTO;
import com.wedu.exam_creation.storage.service.S3Service;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExamDTOMapper {
    public ExamDetailDTO convertToExamDetailResponse(
            ExamEntity exam,
            List<QuestionDTO> questions,
            S3Service s3Service) {
        ExamDetailDTO dto = new ExamDetailDTO();
        dto.setId(exam.getId());
        dto.setUserId(exam.getUserId());
        dto.setName(exam.getName());

        List<QuestionDTO> validQuestions = new ArrayList<>();
        for (ChapterExamEntity chapter : exam.getChapters()) {
            List<QuestionDTO> matchedQuestions = questions.stream()
                    .filter(q -> q.getChapterId().equals(chapter.getId()) &&
                            chapter.getLessonIds().contains(q.getLessonId()))
                    .toList();
            validQuestions.addAll(matchedQuestions);
        }

        List<ExamQuestionDTO> examQuestionDTOS = validQuestions.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getQuestionType() + "-" + q.getDifficultyLevel()
                ))
                .values().stream()
                .map(groupedList -> {
                    QuestionDTO sampleQuestion = groupedList.get(0);

                    List<QuestionDetailResponseDTO> detailDTOs = groupedList.stream()
                            .map(q -> {
                                QuestionDetailResponseDTO questionDetailDTO = new QuestionDetailResponseDTO();
                                questionDetailDTO.setId(q.getId());

                                ContentDTO question = q.getQuestion();

                                Map<String, String> questionImages = question.getVariables().getImage();
                                Map<String, String> newQuestionImages = new HashMap<>(questionImages);

                                if (!questionImages.isEmpty()) {
                                    newQuestionImages.replaceAll(
                                            (key, value) -> s3Service.generatePresignedUrl(value)
                                    );
                                }
                                ContentDTO newQuestion = new ContentDTO(
                                        question.getTemplate(),
                                        new VariablesDTO(
                                                question.getVariables().getMath(),
                                                newQuestionImages
                                        )
                                );

                                List<ContentDTO> newOptions = q.getOptions().stream().map(option -> {
                                            Map<String, String> optionImages = option.getVariables().getImage();
                                            Map<String, String> newOptionImages = new HashMap<>();

                                            if (!optionImages.isEmpty()) {
                                                for (Map.Entry<String, String> entry : optionImages.entrySet()) {
                                                    String key = entry.getKey();
                                                    String value = entry.getValue();

                                                    newOptionImages.put(key, s3Service.generatePresignedUrl(value));
                                                }
                                            }

                                            VariablesDTO newVariablesDTO = new VariablesDTO(
                                                    option.getVariables().getMath(),
                                                    newOptionImages
                                            );

                                            return new ContentDTO(
                                                    option.getTemplate(),
                                                    newVariablesDTO
                                            );
                                        })
                                        .toList();
                                return new QuestionDetailResponseDTO(
                                        q.getId(),
                                        newQuestion,
                                        newOptions);
                            })
                            .toList();

                    return new ExamQuestionDTO(
                            sampleQuestion.getQuestionType(),
                            sampleQuestion.getDifficultyLevel(),
                            detailDTOs
                    );
                })
                .toList();

        dto.setGroups(examQuestionDTOS);

        return dto;
    }

    public ExamDTO convertToExamResponse(ExamEntity entity) {
        List<ExamChapterDTO> chapterDTOS = entity.getChapters().stream()
                .map(chapter -> new ExamChapterDTO(
                        chapter.getId(),
                        chapter.getLessonIds()
                ))
                .toList();

        Integer questionCount = entity.getQuestions().stream()
                .mapToInt(q -> q.getQuestionIds().size())
                .sum();

        return new ExamDTO(entity.getId(),
                entity.getName(),
                chapterDTOS,
                questionCount);
    }
}
