package com.wedu.exam_creation.draft.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.draft.response.*;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.draft.domain.entity.*;
import com.wedu.exam_creation.draft.domain.payload.*;
import com.wedu.exam_creation.draft.domain.repository.IDraftRepository;
import com.wedu.exam_creation.draft.dto.mapper.DraftDTOMapper;
import com.wedu.exam_creation.draft.dto.request.*;
import com.wedu.exam_creation.draft.usecase.util.DraftUtil;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class DraftUsecaseTest {
    private final IDraftRepository repo = mock(IDraftRepository.class);
    private final ChapterService chapters = mock(ChapterService.class);
    private final DraftDTOMapper mapper = mock(DraftDTOMapper.class);
    private final DraftUtil util = mock(DraftUtil.class);
    private final DraftUsecase usecase = new DraftUsecase(repo, chapters, mapper, util);
    private final DraftEntity entity = new DraftEntity();

    private DraftDTO draft() {
        LessonDraftDTO lesson =
                new LessonDraftDTO(
                        "lesson",
                        "Bài",
                        List.of(new MatrixItemDTO("Đúng sai", "Thông hiểu", 2)),
                        new ArrayList<>());
        DraftDTO dto =
                new DraftDTO(
                        "draft",
                        "owner",
                        "Đề",
                        2,
                        List.of("Đúng sai"),
                        List.of(new ChapterDraftDTO("chapter", "Chương", List.of(lesson))),
                        null,
                        null);
        when(repo.getDraft("draft", "owner")).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);
        return dto;
    }

    @Test
    @DisplayName("trả về ID cùng entity ban đầu đúng owner và thời gian cố định khi tạo nháp")
    void test_createDraft_001() {

        Instant now = Instant.parse("2026-11-20T00:00:00Z");
        when(repo.createDraft(any())).thenReturn("created");
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(now);
            assertThat(
                            usecase.createDraft(
                                    new CreateDraftDTO(12, "Đề vật lý", List.of("Đúng sai")),
                                    "owner"))
                    .isEqualTo("created");
        }
        ArgumentCaptor<DraftEntity> captor = ArgumentCaptor.forClass(DraftEntity.class);
        verify(repo).createDraft(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(
                        new DraftEntity(
                                null,
                                "owner",
                                "Đề vật lý",
                                12,
                                List.of("Đúng sai"),
                                List.of(),
                                now,
                                now));
    }

    @Test
    @DisplayName("trả về lỗi repository khi không lưu được bản nháp")
    void test_createDraft_002() {

        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(repo.createDraft(any())).thenThrow(failure);
        assertThatThrownBy(
                        () ->
                                usecase.createDraft(
                                        new CreateDraftDTO(1, "Đề", List.of("Đúng sai")), "owner"))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("trả về DTO đã map khi owner có bản nháp")
    void test_getDraft_003() {

        DraftDTO dto = draft();
        assertThat(usecase.getDraft("draft", "owner")).isSameAs(dto);
    }

    @Test
    @DisplayName("trả về NotFoundException khi draft không tồn tại hoặc không thuộc owner")
    void test_getDraft_004() {

        when(repo.getDraft("draft", "other")).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.getDraft("draft", "other"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName(
            "trả về true cùng payload thêm xóa và owner chính xác khi repository cập nhật thành công")
    void test_updateChapters_005() {

        UpdateDraftDTO request =
                new UpdateDraftDTO(List.of(new UpdateParamDTO("added", "Tên")), List.of("removed"));
        when(repo.updateChapters(
                        new UpdateChaptersPayload(
                                "draft",
                                "owner",
                                List.of(new UpdateParam("added", "Tên")),
                                List.of("removed"))))
                .thenReturn(true);
        assertThat(usecase.updateChapters("draft", request, "owner")).isTrue();
        verify(repo)
                .updateChapters(
                        new UpdateChaptersPayload(
                                "draft",
                                "owner",
                                List.of(new UpdateParam("added", "Tên")),
                                List.of("removed")));
    }

    @Test
    @DisplayName("trả về false khi repository báo không có thay đổi")
    void test_updateChapters_006() {

        UpdateDraftDTO request = new UpdateDraftDTO(List.of(), List.of());
        assertThat(usecase.updateChapters("draft", request, "owner")).isFalse();
        verify(repo)
                .updateChapters(new UpdateChaptersPayload("draft", "owner", List.of(), List.of()));
    }

    @Test
    @DisplayName("trả về NotFoundException khi không có draft thuộc owner")
    void test_updateChapters_007() {

        UpdateDraftDTO request = new UpdateDraftDTO(List.of(), List.of());
        when(repo.updateChapters(any())).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.updateChapters("draft", request, "owner"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName(
            "trả về true cùng payload thêm xóa và owner chính xác khi repository cập nhật thành công")
    void test_updateLessons_008() {

        UpdateDraftDTO request =
                new UpdateDraftDTO(List.of(new UpdateParamDTO("added", "Tên")), List.of("removed"));
        when(repo.updateLessons(
                        new UpdateLessonsPayload(
                                "draft",
                                "owner",
                                "chapter",
                                List.of(new UpdateParam("added", "Tên")),
                                List.of("removed"))))
                .thenReturn(true);
        assertThat(usecase.updateLessons("draft", "chapter", request, "owner")).isTrue();
        verify(repo)
                .updateLessons(
                        new UpdateLessonsPayload(
                                "draft",
                                "owner",
                                "chapter",
                                List.of(new UpdateParam("added", "Tên")),
                                List.of("removed")));
    }

    @Test
    @DisplayName("trả về false khi repository báo không có thay đổi")
    void test_updateLessons_009() {

        UpdateDraftDTO request = new UpdateDraftDTO(List.of(), List.of());
        assertThat(usecase.updateLessons("draft", "chapter", request, "owner")).isFalse();
        verify(repo)
                .updateLessons(
                        new UpdateLessonsPayload(
                                "draft", "owner", "chapter", List.of(), List.of()));
    }

    @Test
    @DisplayName("trả về NotFoundException khi không có draft thuộc owner")
    void test_updateLessons_010() {

        UpdateDraftDTO request = new UpdateDraftDTO(List.of(), List.of());
        when(repo.updateLessons(any())).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.updateLessons("draft", "chapter", request, "owner"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("trả về kết quả lưu với đúng chapter lesson owner và nội dung khi sinh ma trận")
    void test_generateMatrix_011() {

        DraftDTO dto = draft();
        doAnswer(
                        call -> {
                            List<ChapterDraftDTO> data = call.getArgument(0);
                            data.get(0)
                                    .getLessons()
                                    .get(0)
                                    .setMatrix(
                                            List.of(new MatrixItemDTO("Đúng sai", "Nhận biết", 2)));
                            return null;
                        })
                .when(util)
                .generateMatrix(anyList(), eq(dto.getQuestionTypes()), eq(2));
        List<UpdateMatrixPayload> expected =
                List.of(
                        new UpdateMatrixPayload(
                                "draft",
                                "owner",
                                "chapter",
                                "lesson",
                                List.of(new MatrixItemEntity("Đúng sai", "Nhận biết", 2))));
        when(repo.updateMatrix(expected)).thenReturn(true);
        assertThat(usecase.generateMatrix("draft", "owner")).isTrue();
        verify(repo).updateMatrix(expected);
    }

    @Test
    @DisplayName("trả về false khi repository không lưu được ma trận")
    void test_generateMatrix_012() {

        draft();
        when(repo.updateMatrix(anyList())).thenReturn(false);
        assertThat(usecase.generateMatrix("draft", "owner")).isFalse();
    }

    @Test
    @DisplayName("trả về NotFoundException và không sinh hoặc ghi khi khác owner")
    void test_generateMatrix_013() {

        when(repo.getDraft("draft", "other")).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.generateMatrix("draft", "other"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(util);
        verify(repo, never()).updateMatrix(anyList());
    }

    @Test
    @DisplayName("trả về lỗi sinh và không ghi khi utility thất bại")
    void test_generateMatrix_014() {

        draft();
        doThrow(new IllegalStateException("generation failed"))
                .when(util)
                .generateMatrix(anyList(), anyList(), anyInt());
        assertThatThrownBy(() -> usecase.generateMatrix("draft", "owner"))
                .isInstanceOf(IllegalStateException.class);
        verify(repo, never()).updateMatrix(anyList());
    }

    @Test
    @DisplayName("trả về kết quả lưu danh sách rỗng khi draft chưa chọn chương")
    void test_generateMatrix_015() {

        DraftDTO dto = draft();
        dto.setChapters(List.of());
        when(repo.updateMatrix(List.of())).thenReturn(true);
        assertThat(usecase.generateMatrix("draft", "owner")).isTrue();
        verify(repo).updateMatrix(List.of());
    }

    @Test
    @DisplayName("trả về lỗi repository khi ghi ma trận thất bại")
    void test_generateMatrix_016() {

        draft();
        IllegalStateException failure = new IllegalStateException("write failed");
        when(repo.updateMatrix(anyList())).thenThrow(failure);
        assertThatThrownBy(() -> usecase.generateMatrix("draft", "owner")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về payload đặc tả đã map với đúng owner khi danh mục và dữ liệu hợp lệ")
    void test_generateMatrixDetails_017() {

        DraftDTO dto = draft();
        List<ChapterResponseDTO> categories =
                List.of(new ChapterResponseDTO("chapter", "Vật lý", "Chương", List.of()));
        when(chapters.getByIds(List.of("chapter"))).thenReturn(categories);
        MatrixDetailItemDTO detail =
                new MatrixDetailItemDTO("Dạng", "Thông hiểu", "Kiến thức", "Đúng sai", 2);
        MatrixDetailItemEntity mapped =
                new MatrixDetailItemEntity("Dạng", "Thông hiểu", "Kiến thức", "Đúng sai", 2);
        doAnswer(
                        call -> {
                            List<LessonDraftDTO> lessons = call.getArgument(1);
                            lessons.get(0).setMatrixDetails(List.of(detail));
                            return null;
                        })
                .when(util)
                .generateMatrixDetails(eq(categories), anyList());
        when(mapper.detailDTOToEntity(detail)).thenReturn(mapped);
        List<UpdateMatrixDetailsPayload> expected =
                List.of(
                        new UpdateMatrixDetailsPayload(
                                "draft", "owner", "chapter", "lesson", List.of(mapped)));
        when(repo.updateMatrixDetails(expected)).thenReturn(true);
        assertThat(usecase.generateMatrixDetails("draft", "owner")).isTrue();
        verify(repo).updateMatrixDetails(expected);
        assertThat(dto.getChapters().get(0).getLessons().get(0).getMatrixDetails())
                .containsExactly(detail);
    }

    @Test
    @DisplayName("trả về danh sách ghi rỗng khi bài không có đặc tả")
    void test_generateMatrixDetails_018() {

        draft();
        when(chapters.getByIds(List.of("chapter"))).thenReturn(List.of());
        when(repo.updateMatrixDetails(List.of())).thenReturn(true);
        assertThat(usecase.generateMatrixDetails("draft", "owner")).isTrue();
        verify(repo).updateMatrixDetails(List.of());
    }

    @Test
    @DisplayName("trả về NotFoundException và không đọc danh mục hay ghi khi khác owner")
    void test_generateMatrixDetails_019() {

        when(repo.getDraft("draft", "other")).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.generateMatrixDetails("draft", "other"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(chapters, util);
        verify(repo, never()).updateMatrixDetails(anyList());
    }

    @Test
    @DisplayName("trả về lỗi dependency và không ghi khi đọc danh mục thất bại")
    void test_generateMatrixDetails_020() {

        draft();
        when(chapters.getByIds(anyList()))
                .thenThrow(new IllegalStateException("catalog unavailable"));
        assertThatThrownBy(() -> usecase.generateMatrixDetails("draft", "owner"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(util);
        verify(repo, never()).updateMatrixDetails(anyList());
    }

    @Test
    @DisplayName("trả về false khi repository không lưu được đặc tả")
    void test_generateMatrixDetails_021() {

        draft();
        when(chapters.getByIds(anyList())).thenReturn(List.of());
        assertThat(usecase.generateMatrixDetails("draft", "owner")).isFalse();
    }

    @Test
    @DisplayName("trả về lỗi dependency và không ghi khi utility sinh đặc tả thất bại")
    void test_generateMatrixDetails_022() {

        draft();
        when(chapters.getByIds(anyList())).thenReturn(List.of());
        doThrow(new IllegalStateException("generation failed"))
                .when(util)
                .generateMatrixDetails(anyList(), anyList());
        assertThatThrownBy(() -> usecase.generateMatrixDetails("draft", "owner"))
                .isInstanceOf(IllegalStateException.class);
        verify(repo, never()).updateMatrixDetails(anyList());
    }

    @Test
    @DisplayName("trả về lỗi repository khi ghi đặc tả thất bại")
    void test_generateMatrixDetails_023() {

        draft();
        when(chapters.getByIds(anyList())).thenReturn(List.of());
        when(repo.updateMatrixDetails(anyList()))
                .thenThrow(new IllegalStateException("write failed"));
        assertThatThrownBy(() -> usecase.generateMatrixDetails("draft", "owner"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về danh sách DTO theo đúng thứ tự khi owner có nhiều nháp")
    void test_getRecentDraft_024() {

        DraftEntity first = new DraftEntity();
        DraftEntity second = new DraftEntity();
        second.setId("second");
        DraftDTO firstDTO = new DraftDTO();
        DraftDTO secondDTO = new DraftDTO();
        secondDTO.setId("second");
        when(repo.getRecentDraft("owner")).thenReturn(List.of(first, second));
        when(mapper.toDTO(first)).thenReturn(firstDTO);
        when(mapper.toDTO(second)).thenReturn(secondDTO);
        assertThat(usecase.getRecentDraft("owner")).containsExactly(firstDTO, secondDTO);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi owner chưa có nháp")
    void test_getRecentDraft_025() {

        when(repo.getRecentDraft("owner")).thenReturn(List.of());
        assertThat(usecase.getRecentDraft("owner")).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi repository khi truy vấn danh sách thất bại")
    void test_getRecentDraft_026() {

        when(repo.getRecentDraft("owner"))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> usecase.getRecentDraft("owner"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về danh sách DTO theo đúng thứ tự khi owner có nhiều nháp")
    void test_getAllUserDrafts_027() {

        DraftEntity first = new DraftEntity();
        DraftEntity second = new DraftEntity();
        second.setId("second");
        DraftDTO firstDTO = new DraftDTO();
        DraftDTO secondDTO = new DraftDTO();
        secondDTO.setId("second");
        when(repo.getAllUserDrafts("owner")).thenReturn(List.of(first, second));
        when(mapper.toDTO(first)).thenReturn(firstDTO);
        when(mapper.toDTO(second)).thenReturn(secondDTO);
        assertThat(usecase.getAllUserDrafts("owner")).containsExactly(firstDTO, secondDTO);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi owner chưa có nháp")
    void test_getAllUserDrafts_028() {

        when(repo.getAllUserDrafts("owner")).thenReturn(List.of());
        assertThat(usecase.getAllUserDrafts("owner")).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi repository khi truy vấn danh sách thất bại")
    void test_getAllUserDrafts_029() {

        when(repo.getAllUserDrafts("owner"))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> usecase.getAllUserDrafts("owner"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về true khi repository xóa đúng draft của owner")
    void test_deleteDraft_030() {

        when(repo.deleteDraft("draft", "owner")).thenReturn(true);
        assertThat(usecase.deleteDraft("draft", "owner")).isTrue();
        verify(repo).deleteDraft("draft", "owner");
    }

    @Test
    @DisplayName("trả về NotFoundException khi draft không tồn tại hoặc không thuộc owner")
    void test_deleteDraft_031() {

        when(repo.deleteDraft("draft", "owner")).thenReturn(false);
        assertThatThrownBy(() -> usecase.deleteDraft("draft", "owner"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy đề kiểm tra để xóa");
    }

    @Test
    @DisplayName("trả về lỗi repository khi xóa thất bại")
    void test_deleteDraft_032() {

        when(repo.deleteDraft("draft", "owner"))
                .thenThrow(new IllegalStateException("write failed"));
        assertThatThrownBy(() -> usecase.deleteDraft("draft", "owner"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về true và ghi đúng chương bài khi chỉ một số bài trong bản nháp có đặc tả")
    void test_generateMatrixDetails_033() {
        DraftDTO dto = draft();
        LessonDraftDTO first = dto.getChapters().get(0).getLessons().get(0);
        LessonDraftDTO skipped = new LessonDraftDTO("empty", "Bài bỏ qua", List.of(), List.of());
        LessonDraftDTO last =
                new LessonDraftDTO("other-lesson", "Bài 2", first.getMatrix(), List.of());
        dto.setChapters(
                List.of(
                        new ChapterDraftDTO("chapter", "Chương 1", List.of(first, skipped)),
                        new ChapterDraftDTO("other-chapter", "Chương 2", List.of(last))));
        List<ChapterResponseDTO> context =
                List.of(
                        new ChapterResponseDTO("chapter", "Vật lý", "Chương 1", List.of()),
                        new ChapterResponseDTO("other-chapter", "Vật lý", "Chương 2", List.of()));
        when(chapters.getByIds(List.of("chapter", "other-chapter"))).thenReturn(context);
        MatrixDetailItemDTO firstDetail =
                new MatrixDetailItemDTO("Dạng 1", "Thông hiểu", "Kiến thức 1", "Đúng sai", 1);
        MatrixDetailItemDTO lastDetail =
                new MatrixDetailItemDTO("Dạng 2", "Thông hiểu", "Kiến thức 2", "Đúng sai", 1);
        MatrixDetailItemEntity firstMapped =
                new MatrixDetailItemEntity("Dạng 1", "Thông hiểu", "Kiến thức 1", "Đúng sai", 1);
        MatrixDetailItemEntity lastMapped =
                new MatrixDetailItemEntity("Dạng 2", "Thông hiểu", "Kiến thức 2", "Đúng sai", 1);
        Map<String, List<MatrixDetailItemDTO>> detailsByLesson =
                Map.of("lesson", List.of(firstDetail), "other-lesson", List.of(lastDetail));
        doAnswer(
                        call -> {
                            List<LessonDraftDTO> lessons = call.getArgument(1);
                            lessons.forEach(
                                    lesson ->
                                            lesson.setMatrixDetails(
                                                    detailsByLesson.getOrDefault(
                                                            lesson.getId(), List.of())));
                            return null;
                        })
                .when(util)
                .generateMatrixDetails(eq(context), anyList());
        when(mapper.detailDTOToEntity(firstDetail)).thenReturn(firstMapped);
        when(mapper.detailDTOToEntity(lastDetail)).thenReturn(lastMapped);
        List<UpdateMatrixDetailsPayload> expected =
                List.of(
                        new UpdateMatrixDetailsPayload(
                                "draft", "owner", "chapter", "lesson", List.of(firstMapped)),
                        new UpdateMatrixDetailsPayload(
                                "draft",
                                "owner",
                                "other-chapter",
                                "other-lesson",
                                List.of(lastMapped)));
        when(repo.updateMatrixDetails(expected)).thenReturn(true);

        assertThat(usecase.generateMatrixDetails("draft", "owner")).isTrue();

        verify(repo).updateMatrixDetails(expected);
        assertThat(first.getMatrixDetails()).containsExactly(firstDetail);
        assertThat(skipped.getMatrixDetails()).isEmpty();
        assertThat(last.getMatrixDetails()).containsExactly(lastDetail);
    }
}
