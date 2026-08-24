package com.wedu.exam_creation.draft.usecase.util;

import com.wedu.exam_creation.common.dto.chapter.response.BankStatResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.draft.response.ChapterDraftDTO;
import com.wedu.exam_creation.common.dto.draft.response.LessonDraftDTO;
import com.wedu.exam_creation.common.dto.draft.response.MatrixDetailItemDTO;
import com.wedu.exam_creation.common.dto.draft.response.MatrixItemDTO;
import com.wedu.exam_creation.draft.usecase.util.dto.RemainderItem;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DraftUtil {
    public boolean hasMatrix(
            List<ChapterDraftDTO> chapterData,
            Integer questionsCount
    ) {
        int currentTotalQuestions = 0;

        for (ChapterDraftDTO chapter : chapterData) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                if (lesson.getMatrix() != null && !lesson.getMatrix().isEmpty()) {
                    for (MatrixItemDTO matrixItem : lesson.getMatrix()) {
                        currentTotalQuestions += matrixItem.getSelectedCount() != null
                                ? matrixItem.getSelectedCount() : 0;
                    }
                }
            }
        }

        return currentTotalQuestions >= questionsCount;
    }

    public boolean hasMatrixDetail(List<ChapterDraftDTO> chapterData, Integer questionsCount) {
        int currentTotalQuestions = 0;

        for (ChapterDraftDTO chapter : chapterData) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                if (lesson.getMatrixDetails() != null && !lesson.getMatrixDetails().isEmpty()) {
                    for (MatrixDetailItemDTO detailItem : lesson.getMatrixDetails()) {
                        currentTotalQuestions += detailItem.getSelectedCount() != null ?
                                detailItem.getSelectedCount() : 0;
                    }
                }
            }
        }

        return currentTotalQuestions >= questionsCount;
    }

    public void generateMatrix(
            List<ChapterDraftDTO> newDraftChapters,
            List<String> questionTypes,
            Integer questionsCount) {
        List<String> levels = new ArrayList<>(
                Arrays.asList(
                        "Nhận biết",
                        "Thông hiểu",
                        "Vận dụng",
                        "Vận dụng cao"
                )
        );

        // Lấy số lượng cần thiết
        Map<String, Integer> neededByType = new LinkedHashMap<>();
        createMatrixConfig(questionTypes, questionsCount, neededByType);
        Map<String, Integer> neededByLevel = createLevelConfig(questionsCount);

        List<LessonDraftDTO> allLessons = new ArrayList<>();

        for (ChapterDraftDTO chapter : newDraftChapters) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                allLessons.add(lesson);
                lesson.setMatrix(new ArrayList<>());

                // Khởi tạo các item ma trận dựa trực tiếp trên questionTypes truyền vào
                for (String type : questionTypes) {
                    if (!type.equals("Tự luận")) {
                        for (String level : levels) {
                            lesson.getMatrix().add(new MatrixItemDTO(type, level, 0));
                        }
                    }
                }
            }
        }

        if (allLessons.isEmpty() || questionsCount == null || questionsCount <= 0) {
            return;
        }

        List<String> flatTypes = new ArrayList<>();
        neededByType.forEach((type, count) -> {
            for (int i = 0; i < count; i++) flatTypes.add(type);
        });

        List<String> flatLevels = new ArrayList<>();
        neededByLevel.forEach((level, count) -> {
            for (int i = 0; i < count; i++) flatLevels.add(level);
        });

        for (int i = 0; i < questionsCount; i++) {
            String targetType = i < flatTypes.size() ? flatTypes.get(i) : "";
            String targetLevel = i < flatLevels.size() ? flatLevels.get(i) : flatLevels.get(flatLevels.size() - 1);

            LessonDraftDTO targetLesson = allLessons.get(i % allLessons.size());

            Optional<MatrixItemDTO> optItem = targetLesson.getMatrix().stream()
                    .filter(m -> m.getQuestionType().equals(targetType) && m.getDifficultyLevel().equals(targetLevel))
                    .findFirst();

            if (optItem.isPresent()) {
                MatrixItemDTO existingItem = optItem.get();
                existingItem.setSelectedCount(existingItem.getSelectedCount() + 1);

            } else {
                targetLesson.getMatrix().add(new MatrixItemDTO(targetType, targetLevel, 1));
            }
        }
    }

    public void generateMatrixDetails(
            List<ChapterResponseDTO> categories,
            List<LessonDraftDTO> allDraftLessons
    ) {
        Map<String, LessonDataResponseDTO> cateMap = new HashMap<>();
        if (categories != null) {
            for (ChapterResponseDTO cate : categories) {
                if (cate.getLessons() != null) {
                    for (LessonDataResponseDTO lesson : cate.getLessons()) {
                        cateMap.put(lesson.getId(), lesson);
                    }
                }
            }
        }

        // Phân bổ ma trận đặc tả
        for (LessonDraftDTO lessonDraft : allDraftLessons) {
            if (lessonDraft.getMatrix() == null || lessonDraft.getMatrix().isEmpty()) {
                continue;
            }

            LessonDataResponseDTO lessonData = cateMap.get(lessonDraft.getId());
            List<MatrixDetailItemDTO> matrixDetailItems = new ArrayList<>();

            Set<String> allOutcomes = new LinkedHashSet<>();
            Set<String> allExTypes = new LinkedHashSet<>();

            if (lessonData != null && lessonData.getBankStats() != null) {
                for (BankStatResponseDTO stat : lessonData.getBankStats()) {
                    if (stat.getLearningOutcomes() != null) {
                        allOutcomes.addAll(stat.getLearningOutcomes());
                    }
                    if (stat.getExerciseType() != null) {
                        allExTypes.add(stat.getExerciseType());
                    }
                }
            }

            if (allOutcomes.isEmpty()) allOutcomes.add("Kiến thức tổng hợp");
            if (allExTypes.isEmpty()) allExTypes.add("Chưa phân loại");

            // Xử lý từng ô trong ma trận tổng
            for (MatrixItemDTO matrixItem : lessonDraft.getMatrix()) {
                int neededCount = matrixItem.getSelectedCount() != null ? matrixItem.getSelectedCount() : 0;
                String targetType = matrixItem.getQuestionType();
                String targetLevel = matrixItem.getDifficultyLevel();

                List<MatrixDetailItemDTO> targetDetails = new ArrayList<>();

                for (String outcome : allOutcomes) {
                    for (String exType : allExTypes) {
                        MatrixDetailItemDTO dto = new MatrixDetailItemDTO();
                        dto.setQuestionType(targetType);
                        dto.setDifficultyLevel(targetLevel);
                        dto.setExerciseType(exType);
                        dto.setLearningOutcome(outcome);
                        dto.setSelectedCount(0); // Khởi tạo bằng 0

                        targetDetails.add(dto);
                    }
                }

                if (neededCount > 0) {
                    for (int i = 0; i < neededCount; i++) {
                        MatrixDetailItemDTO target = targetDetails.get(i % targetDetails.size());
                        target.setSelectedCount(target.getSelectedCount() + 1);
                    }
                }

                // Đẩy vào danh sách detail chung của bài học
                matrixDetailItems.addAll(targetDetails);
            }
            lessonDraft.setMatrixDetails(matrixDetailItems);

        }
    }

    private void createMatrixConfig(
            List<String> questionTypes,
            Integer questionsCount,
            Map<String, Integer> config
    ) {
        int total = 0;

        for (String type : questionTypes) {
            double percent = 0;

            if (Objects.equals(type, "Nhiều lựa chọn")) {
                percent = 0.3;
            } else if (Objects.equals(type, "Đúng sai")) {
                percent = 0.4;
            } else if (Objects.equals(type, "Trả lời ngắn")) {
                percent = 0.3;
            }

            int count = (int) Math.floor(questionsCount * percent);

            config.put(type, count);
            total += count;
        }

        int remain = questionsCount - total;

        if (remain > 0) {
            String lastType = questionTypes.get(questionTypes.size() - 1);
            config.put(lastType, config.get(lastType) + remain);
        }
    }

    private Map<String, Integer> createLevelConfig(Integer totalQuestions) {
        Map<String, Double> levelRatio = new LinkedHashMap<>();
        levelRatio.put("Nhận biết", 0.4);
        levelRatio.put("Thông hiểu", 0.3);
        levelRatio.put("Vận dụng", 0.2);
        levelRatio.put("Vận dụng cao", 0.1);

        Map<String, Integer> result = new LinkedHashMap<>();
        int assigned = 0;

        List<RemainderItem> remainders = new ArrayList<>();

        for (Map.Entry<String, Double> entry : levelRatio.entrySet()) {
            String level = entry.getKey();
            Double ratio = entry.getValue();

            double exact = totalQuestions * ratio;
            int floorValue = (int) Math.floor(exact);

            result.put(level, floorValue);
            assigned += floorValue;

            remainders.add(
                    new RemainderItem(level, exact - floorValue)
            );
        }

        int remain = totalQuestions - assigned;

        remainders.sort(
                (a, b) -> Double.compare(
                        b.getRemainder(),
                        a.getRemainder()
                )
        );

        for (int i = 0; i < remain; i++) {
            String level = remainders.get(i % remainders.size()).getLevel();
            result.put(level, result.get(level) + 1);
        }

        return result;
    }
}
