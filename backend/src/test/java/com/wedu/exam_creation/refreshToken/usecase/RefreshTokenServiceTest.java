package com.wedu.exam_creation.refreshToken.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.wedu.exam_creation.refreshToken.dto.mapper.RefreshTokenDTOMapper;
import com.wedu.exam_creation.refreshToken.infrastructure.repository.RefreshTokenRepositoryImpl;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshTokenServiceTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-29T00:00:00Z");
    private final RefreshTokenRepositoryImpl repository = mock(RefreshTokenRepositoryImpl.class);
    private final RefreshTokenDTOMapper mapper = mock(RefreshTokenDTOMapper.class);
    private final RefreshTokenService service = new RefreshTokenService(repository, mapper);

    @Test
    @DisplayName("trả về DTO theo thứ tự của repository khi người dùng có nhiều phiên")
    void test_getUserRefreshToken_001() {
        RefreshTokenEntity first =
                new RefreshTokenEntity("id-1", "jti-1", "user", EXPIRES_AT, ISSUED_AT);
        RefreshTokenEntity second =
                new RefreshTokenEntity("id-2", "jti-2", "user", EXPIRES_AT, ISSUED_AT);
        RTResponseDTO firstDto = new RTResponseDTO("id-1", "jti-1", "user", EXPIRES_AT, ISSUED_AT);
        RTResponseDTO secondDto = new RTResponseDTO("id-2", "jti-2", "user", EXPIRES_AT, ISSUED_AT);
        when(repository.getRefreshTokenByUserId("user")).thenReturn(List.of(first, second));
        when(mapper.toResponseDTO(first)).thenReturn(firstDto);
        when(mapper.toResponseDTO(second)).thenReturn(secondDto);

        assertThat(service.getUserRefreshToken("user")).containsExactly(firstDto, secondDto);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi người dùng chưa có phiên refresh")
    void test_getUserRefreshToken_002() {
        when(repository.getRefreshTokenByUserId("user")).thenReturn(List.of());

        assertThat(service.getUserRefreshToken("user")).isEmpty();
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về kết quả lưu gốc khi chuyển metadata yêu cầu thành entity mới")
    void test_save_003(boolean saved) {
        NewRTRequestDTO request = new NewRTRequestDTO("jti", "user", EXPIRES_AT, ISSUED_AT);
        RefreshTokenEntity expected =
                new RefreshTokenEntity(null, "jti", "user", EXPIRES_AT, ISSUED_AT);
        when(repository.save(expected)).thenReturn(saved);

        assertThat(service.save(request)).isEqualTo(saved);
        verify(repository).save(expected);
    }

    @Test
    @DisplayName("trả về true khi repository xóa thành công token theo JTI")
    void test_delete_004() {
        when(repository.delete("jti")).thenReturn(true);

        assertThat(service.delete("jti")).isTrue();
        verify(repository).delete("jti");
    }

    @Test
    @DisplayName("trả về NotFoundException gốc khi repository không tìm thấy token cần xóa")
    void test_delete_005() {
        NotFoundException failure = new NotFoundException("Không tìm thấy RT");
        when(repository.delete("missing")).thenThrow(failure);

        assertThatThrownBy(() -> service.delete("missing")).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về kết quả gốc khi xóa nhiều phiên theo danh sách JTI")
    void test_deleteMany_006(boolean deleted) {
        List<String> jtis = List.of("jti-1", "jti-2");
        when(repository.deleteMany(jtis)).thenReturn(deleted);

        assertThat(service.deleteMany(jtis)).isEqualTo(deleted);
        verify(repository).deleteMany(jtis);
    }

    @Test
    @DisplayName("trả về DTO đã ánh xạ khi tìm thấy token theo JTI và user ID tương ứng")
    void test_getRefreshToken_007() {
        RefreshTokenEntity entity =
                new RefreshTokenEntity("id", "jti", "user", EXPIRES_AT, ISSUED_AT);
        RTResponseDTO dto = new RTResponseDTO("id", "jti", "user", EXPIRES_AT, ISSUED_AT);
        when(repository.getRefreshTokenByJti("jti", "user")).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(dto);

        assertThat(service.getRefreshToken("jti", "user")).isSameAs(dto);
        verify(repository).getRefreshTokenByJti("jti", "user");
    }

    @Test
    @DisplayName("trả về null khi repository không tìm thấy token của người dùng")
    void test_getRefreshToken_008() {
        when(repository.getRefreshTokenByJti("jti", "user")).thenReturn(null);
        when(mapper.toResponseDTO(null)).thenReturn(null);

        assertThat(service.getRefreshToken("jti", "user")).isNull();
    }
}
