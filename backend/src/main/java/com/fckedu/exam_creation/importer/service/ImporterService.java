package com.fckedu.exam_creation.importer.service;

import com.fckedu.exam_creation.category.usecase.CategoryService;
import com.fckedu.exam_creation.common.dto.category.NewBankStatDTO;
import com.fckedu.exam_creation.common.dto.category.NewCategoryDTO;
import com.fckedu.exam_creation.common.dto.category.NewLessonDataDTO;
import com.fckedu.exam_creation.common.dto.category.response.SavedCategoryResponse;
import com.fckedu.exam_creation.common.dto.question.NewOptionDataDTO;
import com.fckedu.exam_creation.common.dto.question.NewQuestionContentDTO;
import com.fckedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.fckedu.exam_creation.common.dto.question.NewVariablesDTO;
import com.fckedu.exam_creation.importer.dto.parsed.NewQuestionImporterDTO;
import com.fckedu.exam_creation.importer.dto.parsed.ParsedDataOutput;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.PandocConverter;
import com.fckedu.exam_creation.question.usecase.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImporterService {
    private final PandocConverter fileParser;
    private final CategoryService categoryService;
    private final QuestionService questionService;

    public ImporterService(CategoryService categoryService, QuestionService questionService, PandocConverter fileParser) {
        this.categoryService = categoryService;
        this.questionService = questionService;
        this.fileParser = fileParser;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean execute(byte[] fileBuffer, String subject) throws Exception {
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Không tồn tại môn học");
        }
        if (fileBuffer == null || fileBuffer.length == 0) {
            throw new IllegalArgumentException("File không tồn tại");
        }

        ParsedDataOutput parsedData = fileParser.parse(fileBuffer);

        NewCategoryDTO newCategoryDTO = new NewCategoryDTO(
                subject,
                parsedData.getCategory().getChapter(),
                parsedData.getCategory().getLessons().stream()
                        .map(lesson -> {
                            NewLessonDataDTO dto = new NewLessonDataDTO();
                            dto.setName(lesson.getName());
                            List<NewBankStatDTO> bankStatsDTO = lesson.getBankStats().stream()
                                    .map(bankStat ->
                                            new NewBankStatDTO(
                                                    bankStat.getExerciseType(),
                                                    bankStat.getDifficultyLevels(),
                                                    bankStat.getLearningOutcomes(),
                                                    bankStat.getQuestionType(),
                                                    bankStat.getCount())).toList();

                            dto.setBankStats(bankStatsDTO);
                            return dto;
                        })
                        .toList());

        SavedCategoryResponse categoryResponse = categoryService.insert(newCategoryDTO);

        List<NewQuestionDTO> newQuestionDTOS = parsedData.getQuestions().stream()
                .map(question -> mapQuestionToDTO(question, subject, categoryResponse)).toList();

        questionService.insert(newQuestionDTOS);

        return true;
    }

    private NewQuestionDTO mapQuestionToDTO(NewQuestionImporterDTO question, String subject, SavedCategoryResponse categoryResponse) {
        NewQuestionDTO newQuestionDTO = new NewQuestionDTO();
        newQuestionDTO.setCategoryId(categoryResponse.getChapterId());
        newQuestionDTO.setLessonId(categoryResponse.getLessonId());
        newQuestionDTO.setExerciseType(question.getExerciseType());
        newQuestionDTO.setDifficultyLevel(question.getDifficultyLevel());
        newQuestionDTO.setLearningOutcomes(question.getLearningOutcomes());
        newQuestionDTO.setQuestionType(question.getQuestionType());

        NewQuestionContentDTO questionContentDTO = new NewQuestionContentDTO();
        questionContentDTO.setTemplate(question.getQuestion().getTemplate());
        questionContentDTO.setVariables(new NewVariablesDTO(
                question.getQuestion().getVariables().getMath(),
                question.getQuestion().getVariables().getImage()));

        newQuestionDTO.setQuestion(questionContentDTO);

        List<NewOptionDataDTO> newOptionDataDTOS = question.getOptions().stream()
                .map(option -> {
                    NewOptionDataDTO newOptionDataDTO = new NewOptionDataDTO();

                    newOptionDataDTO.setTemplate(option.getTemplate());
                    newOptionDataDTO.setVariables(new NewVariablesDTO(
                            option.getVariables().getMath(),
                            option.getVariables().getImage()));

                    return newOptionDataDTO;
                }).toList();

        newQuestionDTO.setOptions(newOptionDataDTOS);
        return newQuestionDTO;
    }
}
