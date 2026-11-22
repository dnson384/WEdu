package com.wedu.exam_creation.draft.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.draft.domain.entity.DraftEntity;
import com.wedu.exam_creation.draft.domain.repository.IDraftRepository;
import com.wedu.exam_creation.draft.dto.mapper.DraftDTOMapper;
import org.junit.jupiter.api.*;

class DraftServiceTest {
    private final IDraftRepository repo = mock(IDraftRepository.class);
    private final DraftDTOMapper mapper = mock(DraftDTOMapper.class);
    private final DraftService service = new DraftService(repo, mapper);

    @Test
    @DisplayName("trả về DTO của chủ sở hữu khi bản nháp tồn tại")
    void test_getDraft_001() {

        DraftEntity entity = new DraftEntity();
        DraftDTO dto = new DraftDTO();
        when(repo.getDraft("draft", "owner")).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);
        assertThat(service.getDraft("draft", "owner")).isSameAs(dto);
    }

    @Test
    @DisplayName("trả về NotFoundException và không map khi draft không tồn tại hoặc khác owner")
    void test_getDraft_002() {

        when(repo.getDraft("draft", "other")).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> service.getDraft("draft", "other"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi repository khi đọc dữ liệu thất bại")
    void test_getDraft_003() {

        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(repo.getDraft("draft", "owner")).thenThrow(failure);
        assertThatThrownBy(() -> service.getDraft("draft", "owner")).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi repository xóa thành công")
    void test_deleteDraft_004() {

        when(repo.deleteDraft("draft", "owner")).thenReturn(true);
        service.deleteDraft("draft", "owner");
        verify(repo).deleteDraft("draft", "owner");
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi repository báo không có bản nháp để xóa")
    void test_deleteDraft_005() {

        when(repo.deleteDraft("draft", "owner")).thenReturn(false);
        assertThatCode(() -> service.deleteDraft("draft", "owner")).doesNotThrowAnyException();
        verify(repo).deleteDraft("draft", "owner");
    }

    @Test
    @DisplayName("trả về lỗi repository khi thao tác xóa thất bại")
    void test_deleteDraft_006() {

        IllegalStateException failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(repo).deleteDraft("draft", "owner");
        assertThatThrownBy(() -> service.deleteDraft("draft", "owner")).isSameAs(failure);
    }
}
