package com.wedu.exam_creation.passwordResetToken.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordResetTokenServiceTest {
    private final PasswordResetTokenUsecase usecase = mock(PasswordResetTokenUsecase.class);
    private final PasswordResetTokenService service = new PasswordResetTokenService(usecase);

    @Test
    @DisplayName("trả về không có ngoại lệ khi chuyển đúng email yêu cầu sang use case")
    void test_requestReset_001() {
        service.requestReset("teacher@example.test");

        verify(usecase).requestReset("teacher@example.test");
    }

    @Test
    @DisplayName("trả về NotFoundException gốc khi use case không tìm thấy tài khoản")
    void test_requestReset_002() {
        String email = "teacher@example.test";
        NotFoundException failure = new NotFoundException("Tài khoản không tồn tại");
        doThrow(failure).when(usecase).requestReset(email);

        assertThatThrownBy(() -> service.requestReset(email)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi truyền token và hai mật khẩu đúng thứ tự")
    void test_resetPassword_003() {
        service.resetPassword("token", "password", "confirmation");

        verify(usecase).resetPassword("token", "password", "confirmation");
    }

    @Test
    @DisplayName("trả về BadRequestException gốc khi use case từ chối token")
    void test_resetPassword_004() {
        BadRequestException failure = new BadRequestException("Token không hợp lệ");
        doThrow(failure).when(usecase).resetPassword("token", "password", "password");

        assertThatThrownBy(() -> service.resetPassword("token", "password", "password"))
                .isSameAs(failure);
    }
}
