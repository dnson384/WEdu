package com.fckedu.exam_creation.draft.usecase.util;

import com.fckedu.exam_creation.common.dto.category.response.BankStatResponseDTO;
import com.fckedu.exam_creation.common.dto.category.response.CategoryResponseDTO;
import com.fckedu.exam_creation.common.dto.category.response.LessonDataResponseDTO;
import com.fckedu.exam_creation.common.dto.draft.response.ChapterDraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.LessonDraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.MatrixDetailItemDTO;
import com.fckedu.exam_creation.common.dto.draft.response.MatrixItemDTO;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.draft.usecase.util.dto.BankPool;
import com.fckedu.exam_creation.draft.usecase.util.dto.Candidate;
import com.fckedu.exam_creation.draft.usecase.util.dto.ComboKey;
import com.fckedu.exam_creation.draft.usecase.util.dto.RemainderItem;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            Map<String, List<LessonDataResponseDTO>> lessonsData,
            List<ChapterDraftDTO> newDraftChapters,
            List<String> questionTypes,
            Integer questionsCount) {
        List<String> levels = new ArrayList<>();
        levels.add("Nhận biết");
        levels.add("Thông hiểu");
        levels.add("Vận dụng");
        levels.add("Vận dụng cao");

        Map<String, Integer> neededByType = new LinkedHashMap<>();
        createMatrixConfig(questionTypes, questionsCount, neededByType);
        Map<String, Integer> neededByLevel = createLevelConfig(questionsCount);

        List<BankPool> availablePools = flatLessons(lessonsData);

        Set<String> allPossibleTypes = new LinkedHashSet<>(questionTypes);
        availablePools.stream().map(BankPool::getType).forEach(allPossibleTypes::add);

        for (ChapterDraftDTO chapter : newDraftChapters) {
            for (LessonDraftDTO lesson : chapter.getLessons()) {
                lesson.setMatrix(new ArrayList<>());

                for (String type : allPossibleTypes) {
                    if (!Objects.equals(type, "Tự luận")) {
                        for (String level : levels) {
                            lesson.getMatrix().add(new MatrixItemDTO(type, level, 0));
                        }
                    }

                }
            }
        }

        List<String> distinctLessonIds = availablePools.stream()
                .map(BankPool::getLessonId)
                .distinct()
                .toList();

        int lessonIndex = 0;
        int totalAllocated = 0;
        int maxAttempts = questionsCount * 10;
        int attempts = 0;
        // Đếm số lần duyệt qua các bài liên tiếp mà không lấy được câu nào
        int consecutiveFailures = 0;

        while (totalAllocated < questionsCount && attempts < maxAttempts) {
            attempts++;

            if (distinctLessonIds.isEmpty()) break;

            // Xoay vòng qua từng bài học
            String currentLessonId = distinctLessonIds.get(lessonIndex % distinctLessonIds.size());
            lessonIndex++;

            // Lấy các pool còn câu hỏi thuộc bài học hiện tại
            List<BankPool> currentLessonPools = availablePools.stream()
                    .filter(p ->
                            p.getLessonId().equals(currentLessonId) &&
                                    p.getAvailable() > 0
                    )
                    .toList();

            BankPool bestPool = null;
            int maxScore = 0;

            // Chấm điểm để chọn ra câu hỏi tối ưu nhất dựa trên thực tế kho câu hỏi
            for (BankPool pool : currentLessonPools) {
                int score = 0;
                boolean typeNeeded = neededByType.getOrDefault(pool.getType(), 0) > 0;
                boolean levelNeeded = neededByLevel.getOrDefault(pool.getLevel(), 0) > 0;

                if (typeNeeded && levelNeeded) {
                    score = 3; // Khớp cả 2 tiêu chí tiêu chuẩn
                } else if (typeNeeded) {
                    score = 2; // Ưu tiên khớp loại câu hỏi trước
                } else if (levelNeeded) {
                    score = 1; // Hoặc khớp mức độ nhận thức
                }

                if (score > maxScore) {
                    maxScore = score;
                    bestPool = pool;
                }
            }

            if (bestPool == null) {
                consecutiveFailures++;

                /*
                Nếu đã quét qua tất cả các bài mà không tìm được câu nào khớp với cấu hình còn thiếu
                => kho thực tế bị lệch so với yêu cầu ban đầu.
                => hạ chuẩn
                */
                if (consecutiveFailures >= distinctLessonIds.size()) {
                    // Tìm 1 câu bất kỳ còn sót lại trên toàn bộ hệ thống,
                    // ưu tiên câu có ích nhất có thể
                    bestPool = availablePools.stream()
                            .filter(p -> p.getAvailable() > 0)
                            .max((p1, p2) -> {
                                int score1 = (neededByType.getOrDefault(p1.getType(), 0) > 0 ? 2 : 0)
                                        + (neededByLevel.getOrDefault(p1.getLevel(), 0) > 0 ? 1 : 0);
                                int score2 = (neededByType.getOrDefault(p2.getType(), 0) > 0 ? 2 : 0)
                                        + (neededByLevel.getOrDefault(p2.getLevel(), 0) > 0 ? 1 : 0);
                                return Integer.compare(score1, score2);
                            })
                            .orElse(null);

                    if (bestPool == null) {
                        break;
                    }
                    consecutiveFailures = 0;
                } else {
                    continue;
                }
            } else {
                consecutiveFailures = 0;
            }

            String targetType = bestPool.getType();
            String targetLevel = bestPool.getLevel();

            bestPool.setAvailable(bestPool.getAvailable() - 1);

            // Giảm nhu cầu cấu hình (chỉ giảm nếu cấu hình đó vẫn còn cần > 0)
            if (neededByType.getOrDefault(targetType, 0) > 0) {
                neededByType.put(targetType, neededByType.get(targetType) - 1);
            }
            if (neededByLevel.getOrDefault(targetLevel, 0) > 0) {
                neededByLevel.put(targetLevel, neededByLevel.get(targetLevel) - 1);
            }
            totalAllocated += 1;

            BankPool finalBestPool = bestPool;
            ChapterDraftDTO chapter = newDraftChapters.stream()
                    .filter(c -> c.getId().equals(finalBestPool.getChapterId()))
                    .findFirst()
                    .orElse(null);

            if (chapter != null) {
                LessonDraftDTO lesson = chapter.getLessons().stream()
                        .filter(l -> l.getId().equals(finalBestPool.getLessonId()))
                        .findFirst()
                        .orElse(null);

                if (lesson != null) {
                    lesson.getMatrix().stream()
                            .filter(m -> m.getQuestionType().equals(targetType) &&
                                    m.getDifficultyLevel().equals(targetLevel))
                            .findFirst()
                            .ifPresent(existingItem ->
                                    existingItem.setSelectedCount(existingItem.getSelectedCount() + 1));
                }
            }
        }

        if (totalAllocated < questionsCount) {
            throw new NotFoundException("Không thể tạo đủ ma trận câu hỏi do kho không đáp ứng.");
        }
    }

    public void generateMatrixDetails(
            List<CategoryResponseDTO> categories,
            List<LessonDraftDTO> allDraftLessons
    ) {
        Map<String, LessonDataResponseDTO> cateMap = new LinkedHashMap<>();
        for (CategoryResponseDTO cate : categories) {
            for (LessonDataResponseDTO lesson : cate.getLessons()) {
                cateMap.put(lesson.getId(), lesson);
            }
        }

        for (LessonDraftDTO lessonDraft : allDraftLessons) {
            LessonDataResponseDTO lessonData = cateMap.get(lessonDraft.getId());
            if (lessonData == null || lessonData.getBankStats().isEmpty()) {
                continue;
            }

            List<MatrixDetailItemDTO> matrixDetailItems = new ArrayList<>();

            Set<String> allQuestionTypes = lessonDraft.getMatrix().stream()
                    .map(MatrixItemDTO::getQuestionType)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<ComboKey> uniqueCombos = new LinkedHashSet<>();
            for (BankStatResponseDTO bankStat : lessonData.getBankStats()) {
                for (String difficultyLevel : bankStat.getDifficultyLevels()) {
                    for (String learningOutcome : bankStat.getLearningOutcomes()) {
                        uniqueCombos.add(new ComboKey(bankStat.getExerciseType(), difficultyLevel, learningOutcome));
                    }
                }
            }

            for (ComboKey combo : uniqueCombos) {
                for (String qType : allQuestionTypes) {
                    MatrixDetailItemDTO newItem = new MatrixDetailItemDTO();
                    newItem.setExerciseType(combo.getExerciseType());
                    newItem.setDifficultyLevel(combo.getDifficultyLevel());
                    newItem.setLearningOutcome(combo.getLearningOutcome());
                    newItem.setQuestionType(qType);
                    newItem.setSelectedCount(0);
                    matrixDetailItems.add(newItem);
                }
            }

            for (MatrixItemDTO matrixItem : lessonDraft.getMatrix()) {
                List<Candidate> candidates = lessonData.getBankStats().stream()
                        .filter(stat ->
                                stat.getQuestionType().equals(matrixItem.getQuestionType()) &&
                                        stat.getDifficultyLevels().contains(matrixItem.getDifficultyLevel()) &&
                                        stat.getCount() != null && stat.getCount() > 0)
                        .map(m -> new Candidate(m, m.getCount()))
                        .toList();

                if (candidates.isEmpty()) continue;

                int needed = matrixItem.getSelectedCount() != null ? matrixItem.getSelectedCount() : 0;
                int index = 0;

                while (needed > 0) {
                    Candidate candidate = candidates.get(index);

                    if (candidate.getRemaining() > 0) {
                        candidate.setRemaining(candidate.getRemaining() - 1);
                        needed--;

                        List<String> outcomes = candidate.getBankStat().getLearningOutcomes();
                        String randomOutcome = outcomes.get(ThreadLocalRandom.current().nextInt(outcomes.size()));

                        matrixDetailItems.stream()
                                .filter(d -> d.getExerciseType().equals(candidate.getBankStat().getExerciseType()) &&
                                        d.getDifficultyLevel().equals(matrixItem.getDifficultyLevel()) &&
                                        d.getLearningOutcome().equals(randomOutcome) &&
                                        d.getQuestionType().equals(matrixItem.getQuestionType()))
                                .findFirst()
                                .ifPresent(detailItem -> {
                                    int currentCount = detailItem.getSelectedCount() == null ? 0 : detailItem.getSelectedCount();
                                    detailItem.setSelectedCount(currentCount + 1);
                                });
                    }

                    index = (index + 1) % candidates.size();

                    boolean allZero = candidates.stream().allMatch(x -> x.getRemaining() == 0);
                    if (allZero) break;
                }
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

    private List<BankPool> flatLessons(Map<String, List<LessonDataResponseDTO>> lessonsData) {
        List<BankPool> pools = new ArrayList<>();

        for (Map.Entry<String, List<LessonDataResponseDTO>> entry : lessonsData.entrySet()) {
            String chapterId = entry.getKey();
            List<LessonDataResponseDTO> lessons = entry.getValue();

            for (LessonDataResponseDTO lesson : lessons) {
                for (BankStatResponseDTO bankStat : lesson.getBankStats()) {
                    String type = bankStat.getQuestionType();
                    int count = bankStat.getCount() != null ? bankStat.getCount() : 0;

                    if (count > 0 && !bankStat.getDifficultyLevels().isEmpty()) {
                        int countPerLevel = (int) Math.floor((double) count / bankStat.getDifficultyLevels().size());

                        for (String level : bankStat.getDifficultyLevels()) {
                            pools.add(new BankPool(
                                    chapterId,
                                    lesson.getId(),
                                    type,
                                    level,
                                    countPerLevel
                            ));
                        }
                    }
                }
            }
        }

        return pools;
    }
}
