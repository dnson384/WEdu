package com.wedu.exam_creation.draft.usecase.util;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.common.dto.chapter.response.*;
import com.wedu.exam_creation.common.dto.draft.response.*;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftUtilTest {
    private final DraftUtil util = new DraftUtil();
    private static final List<String> TYPES = List.of("Nhiều lựa chọn", "Đúng sai", "Trả lời ngắn");

    private LessonDraftDTO lesson(String id, Integer count) {
        return new LessonDraftDTO(
                id,
                id,
                new ArrayList<>(List.of(new MatrixItemDTO(TYPES.get(0), "Nhận biết", count))),
                new ArrayList<>());
    }

    private List<ChapterDraftDTO> chapters(LessonDraftDTO... lessons) {
        return List.of(new ChapterDraftDTO("chapter", "Chương", Arrays.asList(lessons)));
    }

    @Test
    @DisplayName("trả về true khi tổng bằng hoặc vượt số câu yêu cầu")
    void test_hasMatrix_001() {

        LessonDraftDTO lesson = lesson("one", 0);
        lesson.setMatrix(
                List.of(
                        new MatrixItemDTO("Nhiều lựa chọn", "Nhận biết", 3),
                        new MatrixItemDTO("Nhiều lựa chọn", "Nhận biết", 2)));
        assertThat(util.hasMatrix(chapters(lesson), 5)).isTrue();
        assertThat(util.hasMatrix(chapters(lesson), 4)).isTrue();
    }

    @Test
    @DisplayName("trả về false khi tổng thiếu và số lượng null được tính bằng không")
    void test_hasMatrix_002() {

        LessonDraftDTO lesson = lesson("one", 0);
        lesson.setMatrix(
                List.of(
                        new MatrixItemDTO("Nhiều lựa chọn", "Nhận biết", null),
                        new MatrixItemDTO("Nhiều lựa chọn", "Nhận biết", 2)));
        assertThat(util.hasMatrix(chapters(lesson), 3)).isFalse();
    }

    @Test
    @DisplayName("trả về false khi các bài chưa có dữ liệu và yêu cầu lớn hơn không")
    void test_hasMatrix_003() {

        LessonDraftDTO first = lesson("one", 0);
        first.setMatrix(null);
        LessonDraftDTO second = lesson("two", 0);
        second.setMatrix(List.of());
        assertThat(util.hasMatrix(chapters(first, second), 1)).isFalse();
        assertThat(util.hasMatrix(List.of(), 0)).isTrue();
    }

    @Test
    @DisplayName("trả về true khi tổng bằng hoặc vượt số câu yêu cầu")
    void test_hasMatrixDetail_004() {

        LessonDraftDTO lesson = lesson("one", 0);
        lesson.setMatrixDetails(
                List.of(
                        new MatrixDetailItemDTO(
                                "Bài tập", "Nhận biết", "Kiến thức", "Nhiều lựa chọn", 3),
                        new MatrixDetailItemDTO(
                                "Bài tập", "Nhận biết", "Kiến thức", "Nhiều lựa chọn", 2)));
        assertThat(util.hasMatrixDetail(chapters(lesson), 5)).isTrue();
        assertThat(util.hasMatrixDetail(chapters(lesson), 4)).isTrue();
    }

    @Test
    @DisplayName("trả về false khi tổng thiếu và số lượng null được tính bằng không")
    void test_hasMatrixDetail_005() {

        LessonDraftDTO lesson = lesson("one", 0);
        lesson.setMatrixDetails(
                List.of(
                        new MatrixDetailItemDTO(
                                "Bài tập", "Nhận biết", "Kiến thức", "Nhiều lựa chọn", null),
                        new MatrixDetailItemDTO(
                                "Bài tập", "Nhận biết", "Kiến thức", "Nhiều lựa chọn", 2)));
        assertThat(util.hasMatrixDetail(chapters(lesson), 3)).isFalse();
    }

    @Test
    @DisplayName("trả về false khi các bài chưa có dữ liệu và yêu cầu lớn hơn không")
    void test_hasMatrixDetail_006() {

        LessonDraftDTO first = lesson("one", 0);
        first.setMatrixDetails(null);
        LessonDraftDTO second = lesson("two", 0);
        second.setMatrixDetails(List.of());
        assertThat(util.hasMatrixDetail(chapters(first, second), 1)).isFalse();
        assertThat(util.hasMatrixDetail(List.of(), 0)).isTrue();
    }

    @Test
    @DisplayName("trả về ma trận giữ tổng và tỷ lệ loại câu cùng mức độ khi sinh mười câu")
    void test_generateMatrix_007() {

        LessonDraftDTO first = lesson("one", 99);
        LessonDraftDTO second = lesson("two", 99);
        util.generateMatrix(chapters(first, second), TYPES, 10);
        List<MatrixItemDTO> items = new ArrayList<>(first.getMatrix());
        items.addAll(second.getMatrix());
        assertThat(items).hasSize(24);
        assertThat(first.getMatrix().stream().mapToInt(MatrixItemDTO::getSelectedCount).sum())
                .isEqualTo(5);
        assertThat(second.getMatrix().stream().mapToInt(MatrixItemDTO::getSelectedCount).sum())
                .isEqualTo(5);
        assertThat(total(items, "Nhiều lựa chọn", true)).isEqualTo(3);
        assertThat(total(items, "Đúng sai", true)).isEqualTo(4);
        assertThat(total(items, "Trả lời ngắn", true)).isEqualTo(3);
        assertThat(total(items, "Nhận biết", false)).isEqualTo(4);
        assertThat(total(items, "Thông hiểu", false)).isEqualTo(3);
        assertThat(total(items, "Vận dụng", false)).isEqualTo(2);
        assertThat(total(items, "Vận dụng cao", false)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "trả về phần dư vào loại cuối và các mức có phần thập phân lớn nhất khi sinh bảy câu")
    void test_generateMatrix_008() {

        LessonDraftDTO lesson = lesson("one", 0);
        util.generateMatrix(chapters(lesson), TYPES, 7);
        assertThat(total(lesson.getMatrix(), "Nhiều lựa chọn", true)).isEqualTo(2);
        assertThat(total(lesson.getMatrix(), "Đúng sai", true)).isEqualTo(2);
        assertThat(total(lesson.getMatrix(), "Trả lời ngắn", true)).isEqualTo(3);
        assertThat(total(lesson.getMatrix(), "Nhận biết", false)).isEqualTo(3);
        assertThat(total(lesson.getMatrix(), "Thông hiểu", false)).isEqualTo(2);
        assertThat(total(lesson.getMatrix(), "Vận dụng", false)).isEqualTo(1);
        assertThat(total(lesson.getMatrix(), "Vận dụng cao", false)).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về ma trận toàn số không khi yêu cầu không câu hoặc số âm")
    void test_generateMatrix_009() {

        for (Integer count : List.of(0, -1)) {
            LessonDraftDTO lesson = lesson("one", 99);
            util.generateMatrix(chapters(lesson), TYPES, count);
            assertThat(lesson.getMatrix())
                    .hasSize(12)
                    .allSatisfy(item -> assertThat(item.getSelectedCount()).isZero());
        }
        assertThatCode(() -> util.generateMatrix(List.of(), TYPES, 5)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName(
            "trả về toàn bộ câu vào loại duy nhất và chia vòng qua nhiều chương khi chỉ chọn một loại")
    void test_generateMatrix_010() {

        LessonDraftDTO first = lesson("one", 0);
        LessonDraftDTO second = lesson("two", 0);
        List<ChapterDraftDTO> chapters =
                List.of(
                        new ChapterDraftDTO("c1", "C1", List.of(first)),
                        new ChapterDraftDTO("c2", "C2", List.of(second)));
        util.generateMatrix(chapters, List.of(TYPES.get(0)), 5);
        assertThat(first.getMatrix().stream().mapToInt(MatrixItemDTO::getSelectedCount).sum())
                .isEqualTo(3);
        assertThat(second.getMatrix().stream().mapToInt(MatrixItemDTO::getSelectedCount).sum())
                .isEqualTo(2);
        assertThat(total(first.getMatrix(), TYPES.get(0), true)).isEqualTo(3);
    }

    @Test
    @DisplayName(
            "trả về tích outcome và dạng bài không trùng cùng tổng phân bổ khi bankStats có metadata lặp")
    void test_generateMatrixDetails_011() {

        LessonDraftDTO lesson = lesson("one", 7);
        BankStatResponseDTO stat =
                new BankStatResponseDTO("Dạng A", List.of(), List.of("K1", "K2"), TYPES.get(0), 1);
        BankStatResponseDTO other =
                new BankStatResponseDTO("Dạng B", null, List.of("K1"), TYPES.get(0), 0);
        ChapterResponseDTO category =
                new ChapterResponseDTO(
                        "chapter",
                        "Vật lý",
                        "Chương",
                        List.of(
                                new LessonDataResponseDTO(
                                        "one", "Bài", List.of(stat, stat, other))));
        util.generateMatrixDetails(List.of(category), List.of(lesson));
        assertThat(lesson.getMatrixDetails()).hasSize(4);
        assertThat(lesson.getMatrixDetails())
                .extracting(MatrixDetailItemDTO::getSelectedCount)
                .containsExactly(2, 2, 2, 1);
        assertThat(lesson.getMatrixDetails())
                .extracting(MatrixDetailItemDTO::getLearningOutcome)
                .containsExactly("K1", "K1", "K2", "K2");
        assertThat(lesson.getMatrixDetails())
                .extracting(MatrixDetailItemDTO::getExerciseType)
                .containsExactly("Dạng A", "Dạng B", "Dạng A", "Dạng B");
        assertThat(lesson.getMatrixDetails())
                .allSatisfy(
                        item -> {
                            assertThat(item.getQuestionType()).isEqualTo(TYPES.get(0));
                            assertThat(item.getDifficultyLevel()).isEqualTo("Nhận biết");
                        });
    }

    @Test
    @DisplayName("trả về metadata mặc định khi không có danh mục hoặc bankStats thiếu trường")
    void test_generateMatrixDetails_012() {

        LessonDraftDTO lesson = lesson("one", 3);
        util.generateMatrixDetails(null, List.of(lesson));
        assertThat(lesson.getMatrixDetails())
                .containsExactly(
                        new MatrixDetailItemDTO(
                                "Chưa phân loại",
                                "Nhận biết",
                                "Kiến thức tổng hợp",
                                TYPES.get(0),
                                3));
        ChapterResponseDTO category =
                new ChapterResponseDTO(
                        "c",
                        "Vật lý",
                        "C",
                        List.of(
                                new LessonDataResponseDTO(
                                        "one",
                                        "Bài",
                                        List.of(
                                                new BankStatResponseDTO(
                                                        null, null, null, null, null)))));
        util.generateMatrixDetails(List.of(category), List.of(lesson));
        assertThat(lesson.getMatrixDetails())
                .containsExactly(
                        new MatrixDetailItemDTO(
                                "Chưa phân loại",
                                "Nhận biết",
                                "Kiến thức tổng hợp",
                                TYPES.get(0),
                                3));
    }

    @Test
    @DisplayName("trả về ô đặc tả bằng không khi selectedCount null hoặc bằng không")
    void test_generateMatrixDetails_013() {

        LessonDraftDTO lesson = lesson("one", null);
        lesson.getMatrix().add(new MatrixItemDTO("Đúng sai", "Thông hiểu", 0));
        util.generateMatrixDetails(List.of(), List.of(lesson));
        assertThat(lesson.getMatrixDetails())
                .hasSize(2)
                .allSatisfy(item -> assertThat(item.getSelectedCount()).isZero());
    }

    @Test
    @DisplayName("trả về bài không có ma trận được giữ nguyên khi sinh đặc tả")
    void test_generateMatrixDetails_014() {

        LessonDraftDTO first = lesson("one", 0);
        first.setMatrix(null);
        LessonDraftDTO second = lesson("two", 0);
        second.setMatrix(List.of());
        MatrixDetailItemDTO old = new MatrixDetailItemDTO("A", "B", "C", "D", 2);
        first.setMatrixDetails(List.of(old));
        util.generateMatrixDetails(
                List.of(new ChapterResponseDTO("c", null, null, null)), List.of(first, second));
        assertThat(first.getMatrixDetails()).containsExactly(old);
        assertThat(second.getMatrixDetails()).isEmpty();
    }

    private int total(List<MatrixItemDTO> items, String value, boolean byType) {
        return items.stream()
                .filter(
                        item ->
                                value.equals(
                                        byType
                                                ? item.getQuestionType()
                                                : item.getDifficultyLevel()))
                .mapToInt(MatrixItemDTO::getSelectedCount)
                .sum();
    }
}
