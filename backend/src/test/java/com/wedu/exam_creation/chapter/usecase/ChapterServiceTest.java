package com.wedu.exam_creation.chapter.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.domain.entity.*;
import com.wedu.exam_creation.chapter.domain.repository.IChapterRepository;
import com.wedu.exam_creation.chapter.dto.mapper.ChapterDTOMapper;
import com.wedu.exam_creation.common.dto.chapter.*;
import com.wedu.exam_creation.common.dto.chapter.response.*;
import com.wedu.exam_creation.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class ChapterServiceTest {
    private final IChapterRepository repository = mock(IChapterRepository.class);
    private final ChapterDTOMapper mapper = mock(ChapterDTOMapper.class);
    private final ChapterService service = new ChapterService(repository, mapper);

    @Test
    @DisplayName("trả về ID đã lưu khi insert truyền đúng chương bài và timestamp")
    void test_insert_001() {
        Instant timestamp = Instant.parse("2026-11-20T00:00:00Z");
        NewLessonDataDTO lesson = new NewLessonDataDTO(List.of(), "Bài 1");
        LessonDataEntity mapped = new LessonDataEntity(null, "Bài 1", List.of());
        SavedChapterResponse saved = new SavedChapterResponse("c1", "l1");
        when(mapper.newLessonDTOToEntity(lesson)).thenReturn(mapped);
        when(repository.saveChapter(any(ChapterEntity.class))).thenReturn(saved);
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(timestamp);
            assertThat(service.insert(new NewChapterDTO("Vật lý", "Chương 1", List.of(lesson))))
                    .isSameAs(saved);
        }
        ArgumentCaptor<ChapterEntity> captured = ArgumentCaptor.forClass(ChapterEntity.class);
        verify(repository).saveChapter(captured.capture());
        assertThat(captured.getValue())
                .isEqualTo(
                        new ChapterEntity(
                                null, "Vật lý", "Chương 1", List.of(mapped), timestamp, timestamp));
    }

    @Test
    @DisplayName("trả về lỗi mapping và không lưu khi chuyển bài học thất bại")
    void test_insert_002() {
        NewLessonDataDTO lesson = new NewLessonDataDTO(List.of(), "Bài 1");
        IllegalStateException failure = new IllegalStateException("mapping failed");
        when(mapper.newLessonDTOToEntity(lesson)).thenThrow(failure);
        assertThatThrownBy(
                        () ->
                                service.insert(
                                        new NewChapterDTO("Vật lý", "Chương 1", List.of(lesson))))
                .isSameAs(failure);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("trả về lỗi persistence khi lưu chương thất bại")
    void test_insert_003() {
        NewLessonDataDTO lesson = new NewLessonDataDTO(List.of(), "Bài 1");
        NewChapterDTO input = new NewChapterDTO("Vật lý", "Chương 1", List.of(lesson));
        when(mapper.newLessonDTOToEntity(lesson))
                .thenReturn(new LessonDataEntity(null, "Bài 1", List.of()));
        IllegalStateException failure = new IllegalStateException("save failed");
        when(repository.saveChapter(any(ChapterEntity.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.insert(input)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về DTO đã map khi có chương trong repository")
    void test_getAll_004() {
        ChapterEntity entity = new ChapterEntity();
        ChapterResponseDTO dto = new ChapterResponseDTO("c1", "Vật lý", "Chương 1", List.of());
        when(repository.getAll()).thenReturn(List.of(entity));
        when(mapper.entityToDTO(entity)).thenReturn(dto);
        assertThat(service.getAll()).containsExactly(dto);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi repository chưa có chương")
    void test_getAll_005() {
        when(repository.getAll()).thenReturn(List.of());
        assertThat(service.getAll()).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi repository khi không đọc được chương")
    void test_getAll_006() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(repository.getAll()).thenThrow(failure);
        assertThatThrownBy(service::getAll).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về chương và bài đúng thứ tự repository khi đọc theo danh sách ID")
    void test_getByIds_007() {
        LessonDataEntity lesson = new LessonDataEntity("l1", "Bài 1", List.of());
        LessonDataResponseDTO lessonDTO = new LessonDataResponseDTO("l1", "Bài 1", List.of());
        ChapterEntity first =
                new ChapterEntity("c1", "Vật lý", "Chương 1", List.of(lesson), null, null);
        ChapterEntity second = new ChapterEntity("c2", "Vật lý", "Chương 2", List.of(), null, null);
        when(repository.getByIds(List.of("c2", "c1"))).thenReturn(List.of(first, second));
        when(mapper.lessonEntityToDTO(lesson)).thenReturn(lessonDTO);
        assertThat(service.getByIds(List.of("c2", "c1")))
                .containsExactly(
                        new ChapterResponseDTO("c1", "Vật lý", "Chương 1", List.of(lessonDTO)),
                        new ChapterResponseDTO("c2", "Vật lý", "Chương 2", List.of()));
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi repository trả về rỗng")
    void test_getByIds_008() {
        when(repository.getByIds(List.of())).thenReturn(List.of());
        assertThat(service.getByIds(List.of())).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về NotFoundException khi repository không tìm thấy chương")
    void test_getByIds_009() {
        NotFoundException failure = new NotFoundException("Không tồn tại chương này");
        when(repository.getByIds(List.of("missing"))).thenThrow(failure);
        assertThatThrownBy(() -> service.getByIds(List.of("missing"))).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi repository khi truy vấn danh sách ID thất bại")
    void test_getByIds_010() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(repository.getByIds(List.of("c1"))).thenThrow(failure);
        assertThatThrownBy(() -> service.getByIds(List.of("c1"))).isSameAs(failure);
    }
}
