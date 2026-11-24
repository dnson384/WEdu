package com.wedu.exam_creation.chapter.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.wedu.exam_creation.chapter.domain.repository.IChapterRepository;
import com.wedu.exam_creation.chapter.dto.mapper.ChapterDTOMapper;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChapterUsecaseTest {
    private final IChapterRepository repository = mock(IChapterRepository.class);
    private final ChapterDTOMapper mapper = mock(ChapterDTOMapper.class);
    private final ChapterUsecase usecase = new ChapterUsecase(repository, mapper);

    @Test
    @DisplayName("trả về DTO đúng thứ tự khi repository có nhiều chương")
    void test_getAll_001() {
        ChapterEntity first = new ChapterEntity();
        ChapterEntity second = new ChapterEntity();
        first.setId("c1");
        second.setId("c2");
        ChapterResponseDTO firstDTO = new ChapterResponseDTO("c1", "Vật lý", "Chương 1", List.of());
        ChapterResponseDTO secondDTO =
                new ChapterResponseDTO("c2", "Vật lý", "Chương 2", List.of());
        when(repository.getAll()).thenReturn(List.of(first, second));
        when(mapper.entityToDTO(first)).thenReturn(firstDTO);
        when(mapper.entityToDTO(second)).thenReturn(secondDTO);

        assertThat(usecase.getAll()).containsExactly(firstDTO, secondDTO);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi chưa có chương")
    void test_getAll_002() {
        when(repository.getAll()).thenReturn(List.of());
        assertThat(usecase.getAll()).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi repository khi đọc danh sách thất bại")
    void test_getAll_003() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(repository.getAll()).thenThrow(failure);
        assertThatThrownBy(usecase::getAll).isSameAs(failure);
        verifyNoInteractions(mapper);
    }
}
