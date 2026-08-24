package com.wedu.exam_creation.importer.service;

import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.chapter.NewBankStatDTO;
import com.wedu.exam_creation.common.dto.chapter.NewChapterDTO;
import com.wedu.exam_creation.common.dto.chapter.NewLessonDataDTO;
import com.wedu.exam_creation.common.dto.chapter.response.SavedChapterResponse;
import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.importer.dto.parsed.NewQuestionImporterDTO;
import com.wedu.exam_creation.importer.dto.parsed.ParsedDataOutput;
import com.wedu.exam_creation.importer.infrastructure.pandoc.PandocConverter;
import com.wedu.exam_creation.question.usecase.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImporterService {
    private final PandocConverter fileParser;
    private final ChapterService chapterService;
    private final QuestionService questionService;

    public ImporterService(ChapterService chapterService, QuestionService questionService, PandocConverter fileParser) {
        this.chapterService = chapterService;
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

        NewChapterDTO newChapterDTO = new NewChapterDTO(
                subject,
                parsedData.getChapter().getName(),
                parsedData.getChapter().getLessons().stream()
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

        SavedChapterResponse categoryResponse = chapterService.insert(newChapterDTO);

        List<NewQuestionDTO> newQuestionDTOS = parsedData.getQuestions().stream()
                .map(question -> mapQuestionToDTO(question, subject, categoryResponse)).toList();

        questionService.insert(newQuestionDTOS);

        return true;
    }

    private NewQuestionDTO mapQuestionToDTO(NewQuestionImporterDTO question, String subject, SavedChapterResponse categoryResponse) {
        NewQuestionDTO newQuestionDTO = new NewQuestionDTO();
        newQuestionDTO.setChapterId(categoryResponse.getChapterId());
        newQuestionDTO.setLessonId(categoryResponse.getLessonId());
        newQuestionDTO.setExerciseType(question.getExerciseType());
        newQuestionDTO.setDifficultyLevel(question.getDifficultyLevel());
        newQuestionDTO.setLearningOutcomes(question.getLearningOutcomes());
        newQuestionDTO.setQuestionType(question.getQuestionType());

        ContentDTO questionContentDTO = new ContentDTO();
        questionContentDTO.setTemplate(question.getQuestion().getTemplate());
        questionContentDTO.setVariables(new VariablesDTO(
                question.getQuestion().getVariables().getMath(),
                question.getQuestion().getVariables().getImage()));

        newQuestionDTO.setQuestion(questionContentDTO);

        List<ContentDTO> newOptionDataDTOS = question.getOptions().stream()
                .map(option -> {
                    ContentDTO newOptionDataDTO = new ContentDTO();

                    newOptionDataDTO.setTemplate(option.getTemplate());
                    newOptionDataDTO.setVariables(new VariablesDTO(
                            option.getVariables().getMath(),
                            option.getVariables().getImage()));

                    return newOptionDataDTO;
                }).toList();

        newQuestionDTO.setOptions(newOptionDataDTOS);
        return newQuestionDTO;
    }
}
