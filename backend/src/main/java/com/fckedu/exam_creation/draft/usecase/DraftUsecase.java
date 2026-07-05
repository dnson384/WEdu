package com.fckedu.exam_creation.draft.usecase;

import com.fckedu.exam_creation.category.usecase.CategoryService;
import com.fckedu.exam_creation.common.dto.category.response.CategoryResponseDTO;
import com.fckedu.exam_creation.common.dto.category.response.LessonDataResponseDTO;
import com.fckedu.exam_creation.common.dto.draft.response.ChapterDraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.LessonDraftDTO;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.draft.domain.entity.DraftEntity;
import com.fckedu.exam_creation.draft.domain.entity.MatrixItemEntity;
import com.fckedu.exam_creation.draft.domain.payload.*;
import com.fckedu.exam_creation.draft.domain.repository.IDraftRepository;
import com.fckedu.exam_creation.draft.dto.mapper.DraftDTOMapper;
import com.fckedu.exam_creation.draft.dto.request.CreateDraftDTO;
import com.fckedu.exam_creation.draft.dto.request.UpdateChaptersDraftDTO;
import com.fckedu.exam_creation.draft.dto.request.UpdateLessonsDraftDTO;
import com.fckedu.exam_creation.draft.usecase.util.DraftUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DraftUsecase {
    private final IDraftRepository repo;
    private final CategoryService categoryService;
    private final DraftDTOMapper mapper;
    private final DraftUtil util;

    public DraftUsecase(IDraftRepository repo, CategoryService categoryService, DraftDTOMapper mapper, DraftUtil util) {
        this.repo = repo;
        this.categoryService = categoryService;
        this.mapper = mapper;
        this.util = util;
    }

    public String createDraft(CreateDraftDTO payload, String userId) {
        DraftEntity payloadDomain = new DraftEntity(
                null,
                userId,
                payload.getExamName(),
                payload.getQuestionsCount(),
                payload.getQuestionTypes(),
                new ArrayList<>(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        return repo.createDraft(payloadDomain);
    }

    public DraftDTO getDraft(String draftId, String userId) {
        DraftEntity draft = repo.getDraft(draftId, userId);
        return mapper.toDTO(draft);
    }

    public boolean updateChapters(UpdateChaptersDraftDTO payload, String userId) {
        UpdateChaptersPayload payloadDomain = new UpdateChaptersPayload(
                payload.getDraftId(),
                userId,
                payload.getAdd().stream()
                        .map(item -> new UpdateParam(
                                item.getId(),
                                item.getName()
                        ))
                        .toList(),
                payload.getDel()
        );

        return repo.updateChapters(payloadDomain);
    }

    public boolean updateLessons(UpdateLessonsDraftDTO payload, String userId) {
        UpdateLessonsPayload payloadDomain = new UpdateLessonsPayload(
                payload.getDraftId(),
                userId,
                payload.getChapterId(),
                payload.getAdd().stream()
                        .map(item -> new UpdateParam(
                                item.getId(),
                                item.getName()
                        ))
                        .toList(),
                payload.getDel()
        );

        return repo.updateLessons(payloadDomain);
    }

    public boolean generateMatrix(String draftId, String userId) {
        DraftDTO draft = getDraft(draftId, userId);

        if (util.hasMatrix(draft.getChapters(), draft.getQuestionsCount())) {
            return true;
        }

        List<String> chapterIds = draft.getChapters().stream()
                .map(ChapterDraftDTO::getId).toList();
        List<CategoryResponseDTO> categories = categoryService.getByIds(chapterIds);
        Map<String, CategoryResponseDTO> cateMap = categories.stream()
                .collect(Collectors.toMap(CategoryResponseDTO::getId, category -> category));

        Map<String, List<LessonDataResponseDTO>> lessonsData = new HashMap<>();

        for (ChapterDraftDTO chapter : draft.getChapters()) {
            List<String> lessonIds = chapter.getLessons().stream()
                    .map(LessonDraftDTO::getId)
                    .toList();
            CategoryResponseDTO curChapter = cateMap.get(chapter.getId());

            if (curChapter == null) {
                throw new NotFoundException("Chương không tồn tại");
            }

            List<LessonDataResponseDTO> curLessons = curChapter.getLessons().stream()
                    .filter(lesson -> lessonIds.contains(lesson.getId()))
                    .toList();

            lessonsData.put(chapter.getId(), curLessons);
        }

        if (lessonsData.isEmpty()) {
            throw new NotFoundException("Nội dung không tồn tại");
        }

        List<ChapterDraftDTO> newDraftChapters = new ArrayList<>(draft.getChapters());
        util.generateMatrix(
                lessonsData,
                newDraftChapters,
                draft.getQuestionTypes(),
                draft.getQuestionsCount()
        );

        List<UpdateMatrixPayload> payload = new ArrayList<>();
        for (ChapterDraftDTO chapter : newDraftChapters) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                payload.add(new UpdateMatrixPayload(
                        draftId,
                        userId,
                        chapter.getId(),
                        lesson.getId(),
                        lesson.getMatrix().stream()
                                .map(m -> new MatrixItemEntity(
                                        m.getQuestionType(),
                                        m.getDifficultyLevel(),
                                        m.getSelectedCount()
                                ))
                                .toList()
                ));
            }
        }

        return repo.updateMatrix(payload);
    }

    public boolean generateMatrixDetails(String draftId, String userId) {
        DraftDTO draft = getDraft(draftId, userId);

        if (util.hasMatrixDetail(draft.getChapters(), draft.getQuestionsCount())) {
            return true;
        }

        List<String> chapterIds = draft.getChapters().stream()
                .map(ChapterDraftDTO::getId).toList();
        List<CategoryResponseDTO> categories = categoryService.getByIds(chapterIds);

        List<LessonDraftDTO> allDraftLessons = new ArrayList<>();
        for (ChapterDraftDTO chapter : draft.getChapters()) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                allDraftLessons.add(new LessonDraftDTO(
                        lesson.getId(),
                        lesson.getName(),
                        lesson.getMatrix(),
                        lesson.getMatrixDetails()
                ));
            }
        }

        util.generateMatrixDetails(categories, allDraftLessons);

        List<UpdateMatrixDetailsPayload> payload = new ArrayList<>();

        for (ChapterDraftDTO chapter : draft.getChapters()) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                allDraftLessons.stream()
                        .filter(l -> l.getId().equals(lesson.getId()))
                        .findFirst()
                        .ifPresent(l -> lesson.setMatrixDetails(l.getMatrixDetails()));

                if (lesson.getMatrixDetails() != null && !lesson.getMatrixDetails().isEmpty()) {
                    payload.add(new UpdateMatrixDetailsPayload(
                            draft.getId(),
                            userId,
                            chapter.getId(),
                            lesson.getId(),
                            lesson.getMatrixDetails().stream()
                                    .map(mapper::detailDTOToEntity)
                                    .toList()
                    ));
                }
            }
        }
        return repo.updateMatrixDetails(payload);
    }

    public List<DraftDTO> getRecentDraft(String userId) {
        List<DraftEntity> draftEntities = repo.getRecentDraft(userId);
        return draftEntities.stream().map(mapper::toDTO).toList();
    }

    public List<DraftDTO> getAllUserDrafts(String userId) {
        List<DraftEntity> draftEntities = repo.getAllUserDrafts(userId);
        return draftEntities.stream().map(mapper::toDTO).toList();
    }
}
