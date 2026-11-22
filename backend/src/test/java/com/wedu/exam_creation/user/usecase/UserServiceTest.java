package com.wedu.exam_creation.user.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceTest {
    private static final String ID = "teacher-1";
    private static final String EMAIL = "teacher@example.com";
    private final UserUsecase usecase = mock(UserUsecase.class);
    private final UserService service = new UserService(usecase);

    @Test
    @DisplayName("trả về kết quả xác nhận liên kết khi đối chiếu email và subject Google")
    void test_hasGoogleLink_001() {
        when(usecase.hasGoogleLink(EMAIL, "subject")).thenReturn(true);

        assertThat(service.hasGoogleLink(EMAIL, "subject")).isTrue();
    }

    @Test
    @DisplayName("trả về tài khoản đã liên kết khi use case gắn thành công subject Google")
    void test_linkGoogle_002() {
        CommonUserResponseAllDTO linked = new CommonUserResponseAllDTO();
        linked.setLoginMethod("MIXED");
        when(usecase.linkGoogle(ID, "subject")).thenReturn(linked);

        assertThat(service.linkGoogle(ID, "subject")).isSameAs(linked);
    }

    @Test
    @DisplayName("trả về ConflictException gốc khi use case từ chối liên kết Google")
    void test_linkGoogle_003() {
        ConflictException failure = new ConflictException("Không thể liên kết tài khoản Google");
        when(usecase.linkGoogle(ID, "subject")).thenThrow(failure);

        assertThatThrownBy(() -> service.linkGoogle(ID, "subject")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về tài khoản tương ứng khi tìm được người dùng theo email")
    void test_findByEmail_004() {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setEmail(EMAIL);
        when(usecase.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThat(service.findByEmail(EMAIL)).containsSame(user);
    }

    @Test
    @DisplayName("trả về tài khoản tương ứng khi tìm được người dùng theo ID")
    void test_findById_005() {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId(ID);
        when(usecase.findById(ID)).thenReturn(user);

        assertThat(service.findById(ID)).isSameAs(user);
    }

    @Test
    @DisplayName("trả về danh sách hồ sơ khi use case tìm được người dùng trong hệ thống")
    void test_getAllUsers_006() {
        CommonUserResponseDTO user = new CommonUserResponseDTO();
        user.setId(ID);
        when(usecase.getAllUsers()).thenReturn(List.of(user));

        assertThat(service.getAllUsers()).containsExactly(user);
    }

    @Test
    @DisplayName("trả về danh sách hồ sơ khớp khi truy vấn bằng từ khóa")
    void test_findUserByKeyword_007() {
        CommonUserResponseDTO user = new CommonUserResponseDTO();
        user.setEmail(EMAIL);
        when(usecase.findUserByKeyword("teacher")).thenReturn(List.of(user));

        assertThat(service.findUserByKeyword("teacher")).containsExactly(user);
    }

    @Test
    @DisplayName("trả về NotFoundException gốc khi use case không tìm thấy hồ sơ theo từ khóa")
    void test_findUserByKeyword_008() {
        NotFoundException failure = new NotFoundException("Không tồn tại người dùng");
        when(usecase.findUserByKeyword("missing")).thenThrow(failure);

        assertThatThrownBy(() -> service.findUserByKeyword("missing")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về tài khoản đã tạo khi chuyển yêu cầu cùng mật khẩu băm vào use case")
    void test_createNewUser_009() {
        NewUserRequestDTO request = new NewUserRequestDTO();
        request.setEmail(EMAIL);
        CommonUserResponseAllDTO created = new CommonUserResponseAllDTO();
        created.setId(ID);
        when(usecase.createNewUser(request, "hash")).thenReturn(created);

        assertThat(service.createNewUser(request, "hash")).isSameAs(created);
    }

    @Test
    @DisplayName("trả về tài khoản có vai trò mới khi use case cập nhật thành công")
    void test_updateRole_010() {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId(ID);
        user.setRole("ROLE_MODERATOR");
        when(usecase.updateRole(user)).thenReturn(user);

        assertThat(service.updateRole(user)).isSameAs(user);
    }

    @Test
    @DisplayName("trả về tài khoản có mật khẩu băm mới khi use case cập nhật thành công")
    void test_updatePassword_011() {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId(ID);
        user.setHashedPassword("hash");
        when(usecase.updatePassword(user)).thenReturn(user);

        assertThat(service.updatePassword(user)).isSameAs(user);
    }

    @Test
    @DisplayName("trả về tài khoản đang hoạt động khi use case mở khóa thành công")
    void test_unlockUser_012() {
        CommonUserResponseAllDTO unlocked = new CommonUserResponseAllDTO();
        unlocked.setIsActive(true);
        when(usecase.unlockUser(ID)).thenReturn(unlocked);

        assertThat(service.unlockUser(ID)).isSameAs(unlocked);
    }

    @Test
    @DisplayName("trả về tài khoản đã khóa khi use case đổi trạng thái thành công")
    void test_lockUser_013() {
        CommonUserResponseAllDTO locked = new CommonUserResponseAllDTO();
        locked.setIsActive(false);
        when(usecase.lockUser(ID)).thenReturn(locked);

        assertThat(service.lockUser(ID)).isSameAs(locked);
    }

    @Test
    @DisplayName(
            "trả về ngày hết hạn do use case tính khi cấp PLUS theo thời điểm và số ngày yêu cầu")
    void test_grantPaidPlus_014() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiration = Instant.parse("2026-01-31T00:00:00Z");
        when(usecase.grantPaidPlus(ID, now, 30)).thenReturn(expiration);

        assertThat(service.grantPaidPlus(ID, now, 30)).isEqualTo(expiration);
    }

    @Test
    @DisplayName("trả về true khi use case hoàn tất xóa tài khoản")
    void test_deleteUser_015() {
        when(usecase.deleteUser(ID)).thenReturn(true);

        assertThat(service.deleteUser(ID)).isTrue();
    }
}
