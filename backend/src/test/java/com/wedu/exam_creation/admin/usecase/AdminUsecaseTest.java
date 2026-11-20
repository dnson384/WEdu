package com.wedu.exam_creation.admin.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.response.*;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.user.usecase.UserService;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class AdminUsecaseTest {
    private final UserService users = mock(UserService.class);
    private final UserCommonDTOMapper mapper = mock(UserCommonDTOMapper.class);
    private final AdminUsecase usecase = new AdminUsecase(users, mapper);
    private final CommonUserResponseAllDTO admin = user("admin", "ROLE_ADMIN", true);
    private final IllegalStateException failure = new IllegalStateException("dependency failed");

    private CommonUserResponseAllDTO user(String id, String role, boolean active) {
        CommonUserResponseAllDTO result = new CommonUserResponseAllDTO();
        result.setId(id);
        result.setRole(role);
        result.setIsActive(active);
        return result;
    }

    private CommonUserResponseDTO response(String role, boolean active) {
        CommonUserResponseDTO result = new CommonUserResponseDTO();
        result.setId("target");
        result.setEmail("teacher@example.com");
        result.setUsername("Giáo viên");
        result.setRole(role);
        result.setIsActive(active);
        result.setAccountType("FREE");
        return result;
    }

    @Test
    @DisplayName("trả về danh sách đúng thứ tự khi dịch vụ người dùng có nhiều tài khoản")
    void test_getAllUsers_001() {
        CommonUserResponseDTO first = response("ROLE_TEACHER", true);
        CommonUserResponseDTO second = response("ROLE_MODERATOR", false);
        second.setId("second");
        when(users.getAllUsers()).thenReturn(List.of(first, second));
        assertThat(usecase.getAllUsers(admin)).containsExactly(first, second);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi dịch vụ người dùng trả rỗng")
    void test_getAllUsers_002() {
        when(users.getAllUsers()).thenReturn(List.of());
        assertThat(usecase.getAllUsers(admin)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về NotFoundException khi dịch vụ báo hệ thống chưa có người dùng")
    void test_getAllUsers_003() {
        NotFoundException error = new NotFoundException("Hệ thống chưa có người dùng nào");
        when(users.getAllUsers()).thenThrow(error);
        assertThatThrownBy(() -> usecase.getAllUsers(admin)).isSameAs(error);
    }

    @Test
    @DisplayName("trả về lỗi dependency khi không đọc được danh sách người dùng")
    void test_getAllUsers_004() {
        when(users.getAllUsers()).thenThrow(failure);
        assertThatThrownBy(() -> usecase.getAllUsers(admin)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về danh sách tìm thấy khi truyền nguyên keyword cho dịch vụ người dùng")
    void test_findUsers_005() {
        CommonUserResponseDTO first = response("ROLE_TEACHER", true);
        CommonUserResponseDTO second = response("ROLE_MODERATOR", true);
        second.setId("second");
        when(users.findUserByKeyword(" Nguyễn ")).thenReturn(List.of(first, second));
        assertThat(usecase.findUsers(admin, " Nguyễn ")).containsExactly(first, second);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về BadRequestException khi dịch vụ từ chối keyword rỗng")
    void test_findUsers_006() {
        BadRequestException error = new BadRequestException("Từ khóa không được rỗng");
        when(users.findUserByKeyword(" ")).thenThrow(error);
        assertThatThrownBy(() -> usecase.findUsers(admin, " ")).isSameAs(error);
    }

    @Test
    @DisplayName("trả về NotFoundException khi dịch vụ không tìm thấy tài khoản")
    void test_findUsers_007() {
        NotFoundException error = new NotFoundException("Không tồn tại người dùng");
        when(users.findUserByKeyword("missing")).thenThrow(error);
        assertThatThrownBy(() -> usecase.findUsers(admin, "missing")).isSameAs(error);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi dịch vụ tìm kiếm trả rỗng")
    void test_findUsers_008() {
        when(users.findUserByKeyword("missing")).thenReturn(List.of());
        assertThat(usecase.findUsers(admin, "missing")).isEmpty();
    }

    @Test
    @DisplayName("trả về lỗi dependency khi tìm kiếm người dùng thất bại")
    void test_findUsers_009() {
        when(users.findUserByKeyword("Nguyễn")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.findUsers(admin, "Nguyễn")).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_TEACHER", "ROLE_MODERATOR", "ROLE_ADMIN"})
    @DisplayName(
            "trả về DTO đã cập nhật khi đổi tài khoản không phải ADMIN sang role backend chấp nhận")
    void test_updateUserRole_010(String role) {
        CommonUserResponseAllDTO current = user("target", "ROLE_TEACHER", true);
        CommonUserResponseAllDTO updated = user("target", role, true);
        CommonUserResponseDTO result = response(role, true);
        when(users.findById("target")).thenReturn(current);
        when(users.updateRole(current)).thenReturn(updated);
        when(mapper.commonAllToCommonDTO(updated)).thenReturn(result);
        assertThat(usecase.updateUserRole(admin, "target", role)).isSameAs(result);
        assertThat(current.getRole()).isEqualTo(role);
        verify(users).updateRole(current);
        verify(mapper).commonAllToCommonDTO(updated);
    }

    @Test
    @DisplayName("trả về BadRequestException và không đọc ghi khi đổi role của bản thân")
    void test_updateUserRole_011() {
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "admin", "ROLE_TEACHER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Không được phép thay đổi quyền của bản thân");
        verifyNoInteractions(users, mapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "TEACHER", "ROLE_USER", "role_teacher", " ROLE_TEACHER "})
    @DisplayName(
            "trả về BadRequestException và không đọc ghi khi role null rỗng hoặc ngoài danh sách cho phép")
    void test_updateUserRole_012(String role) {
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "target", role))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quyền không hợp lệ");
        verifyNoInteractions(users, mapper);
    }

    @Test
    @DisplayName(
            "trả về NotFoundException và không cập nhật khi tài khoản cần phân quyền không tồn tại")
    void test_updateUserRole_013() {
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "target", "ROLE_MODERATOR"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy người dùng để phân quyền");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về ForbiddenException và không cập nhật khi tài khoản đích là ADMIN khác")
    void test_updateUserRole_014() {
        CommonUserResponseAllDTO current = user("target", "ROLE_ADMIN", true);
        when(users.findById("target")).thenReturn(current);
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "target", "ROLE_TEACHER"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Không được phép thay đổi quyền của admin khác");
        assertThat(current.getRole()).isEqualTo("ROLE_ADMIN");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không cập nhật khi đọc tài khoản phân quyền thất bại")
    void test_updateUserRole_015() {
        when(users.findById("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "target", "ROLE_MODERATOR"))
                .isSameAs(failure);
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không map khi ghi role thất bại")
    void test_updateUserRole_016() {
        CommonUserResponseAllDTO current = user("target", "ROLE_TEACHER", true);
        when(users.findById("target")).thenReturn(current);
        when(users.updateRole(current)).thenThrow(failure);
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "target", "ROLE_MODERATOR"))
                .isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi mapper khi đã nhận kết quả cập nhật role từ dịch vụ")
    void test_updateUserRole_017() {
        CommonUserResponseAllDTO current = user("target", "ROLE_TEACHER", true);
        CommonUserResponseAllDTO updated = user("target", "ROLE_MODERATOR", true);
        when(users.findById("target")).thenReturn(current);
        when(users.updateRole(current)).thenReturn(updated);
        when(mapper.commonAllToCommonDTO(updated)).thenThrow(failure);
        assertThatThrownBy(() -> usecase.updateUserRole(admin, "target", "ROLE_MODERATOR"))
                .isSameAs(failure);
        verify(users).updateRole(current);
    }

    @Test
    @DisplayName("trả về DTO sau khóa khi tài khoản cần thay đổi trạng thái")
    void test_lockUser_018() {
        CommonUserResponseAllDTO current = user("target", "ROLE_TEACHER", true);
        CommonUserResponseAllDTO updated = user("target", "ROLE_TEACHER", false);
        CommonUserResponseDTO result = response("ROLE_TEACHER", false);
        when(users.findById("target")).thenReturn(current);
        when(users.lockUser("target")).thenReturn(updated);
        when(mapper.commonAllToCommonDTO(updated)).thenReturn(result);
        assertThat(usecase.lockUser(admin, "target")).isSameAs(result);
        verify(users).lockUser("target");
        verify(mapper).commonAllToCommonDTO(updated);
    }

    @Test
    @DisplayName("trả về DTO hiện có và không ghi lại khi tài khoản đã ở trạng thái khóa")
    void test_lockUser_019() {
        CommonUserResponseAllDTO current = user("target", "ROLE_MODERATOR", false);
        CommonUserResponseDTO result = response("ROLE_MODERATOR", false);
        when(users.findById("target")).thenReturn(current);
        when(mapper.commonAllToCommonDTO(current)).thenReturn(result);
        assertThat(usecase.lockUser(admin, "target")).isSameAs(result);
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
    }

    @Test
    @DisplayName("trả về BadRequestException và không đọc ghi khi khóa tài khoản bản thân")
    void test_lockUser_020() {
        assertThatThrownBy(() -> usecase.lockUser(admin, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Không được phép khóa tài khoản bản thân");
        verifyNoInteractions(users, mapper);
    }

    @Test
    @DisplayName("trả về NotFoundException và không ghi khi tài khoản cần khóa không tồn tại")
    void test_lockUser_021() {
        assertThatThrownBy(() -> usecase.lockUser(admin, "target"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy người dùng để khóa");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về ForbiddenException và không ghi khi khóa tài khoản ADMIN khác")
    void test_lockUser_022() {
        when(users.findById("target")).thenReturn(user("target", "ROLE_ADMIN", true));
        assertThatThrownBy(() -> usecase.lockUser(admin, "target"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Không được phép khóa của admin khác");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không ghi khi đọc tài khoản cần khóa thất bại")
    void test_lockUser_023() {
        when(users.findById("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.lockUser(admin, "target")).isSameAs(failure);
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không map khi ghi trạng thái khóa thất bại")
    void test_lockUser_024() {
        when(users.findById("target")).thenReturn(user("target", "ROLE_TEACHER", true));
        when(users.lockUser("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.lockUser(admin, "target")).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi mapping khi map kết quả khóa thất bại")
    void test_lockUser_025() {
        CommonUserResponseAllDTO updated = user("target", "ROLE_TEACHER", false);
        when(users.findById("target")).thenReturn(user("target", "ROLE_TEACHER", true));
        when(users.lockUser("target")).thenReturn(updated);
        when(mapper.commonAllToCommonDTO(updated)).thenThrow(failure);
        assertThatThrownBy(() -> usecase.lockUser(admin, "target")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về DTO sau mở khóa khi tài khoản cần thay đổi trạng thái")
    void test_unlockUser_026() {
        CommonUserResponseAllDTO current = user("target", "ROLE_TEACHER", false);
        CommonUserResponseAllDTO updated = user("target", "ROLE_TEACHER", true);
        CommonUserResponseDTO result = response("ROLE_TEACHER", true);
        when(users.findById("target")).thenReturn(current);
        when(users.unlockUser("target")).thenReturn(updated);
        when(mapper.commonAllToCommonDTO(updated)).thenReturn(result);
        assertThat(usecase.unlockUser(admin, "target")).isSameAs(result);
        verify(users).unlockUser("target");
        verify(mapper).commonAllToCommonDTO(updated);
    }

    @Test
    @DisplayName("trả về DTO hiện có và không ghi lại khi tài khoản đã ở trạng thái mở khóa")
    void test_unlockUser_027() {
        CommonUserResponseAllDTO current = user("target", "ROLE_MODERATOR", true);
        CommonUserResponseDTO result = response("ROLE_MODERATOR", true);
        when(users.findById("target")).thenReturn(current);
        when(mapper.commonAllToCommonDTO(current)).thenReturn(result);
        assertThat(usecase.unlockUser(admin, "target")).isSameAs(result);
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
    }

    @Test
    @DisplayName("trả về BadRequestException và không đọc ghi khi mở khóa tài khoản bản thân")
    void test_unlockUser_028() {
        assertThatThrownBy(() -> usecase.unlockUser(admin, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Không được phép mở khóa tài khoản bản thân");
        verifyNoInteractions(users, mapper);
    }

    @Test
    @DisplayName("trả về NotFoundException và không ghi khi tài khoản cần mở khóa không tồn tại")
    void test_unlockUser_029() {
        assertThatThrownBy(() -> usecase.unlockUser(admin, "target"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy người dùng để mở khóa");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về ForbiddenException và không ghi khi mở khóa tài khoản ADMIN khác")
    void test_unlockUser_030() {
        when(users.findById("target")).thenReturn(user("target", "ROLE_ADMIN", false));
        assertThatThrownBy(() -> usecase.unlockUser(admin, "target"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Không được phép mở khóa của admin khác");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không ghi khi đọc tài khoản cần mở khóa thất bại")
    void test_unlockUser_031() {
        when(users.findById("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.unlockUser(admin, "target")).isSameAs(failure);
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không map khi ghi trạng thái mở khóa thất bại")
    void test_unlockUser_032() {
        when(users.findById("target")).thenReturn(user("target", "ROLE_TEACHER", false));
        when(users.unlockUser("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.unlockUser(admin, "target")).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi mapping khi map kết quả mở khóa thất bại")
    void test_unlockUser_033() {
        CommonUserResponseAllDTO updated = user("target", "ROLE_TEACHER", true);
        when(users.findById("target")).thenReturn(user("target", "ROLE_TEACHER", false));
        when(users.unlockUser("target")).thenReturn(updated);
        when(mapper.commonAllToCommonDTO(updated)).thenThrow(failure);
        assertThatThrownBy(() -> usecase.unlockUser(admin, "target")).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về đúng kết quả xóa từ dịch vụ khi tài khoản đích không phải ADMIN")
    void test_deleteUser_034(boolean deleted) {
        when(users.findById("target")).thenReturn(user("target", "ROLE_TEACHER", true));
        when(users.deleteUser("target")).thenReturn(deleted);
        assertThat(usecase.deleteUser(admin, "target")).isEqualTo(deleted);
        verify(users).deleteUser("target");
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về BadRequestException và không đọc xóa khi xóa tài khoản bản thân")
    void test_deleteUser_035() {
        assertThatThrownBy(() -> usecase.deleteUser(admin, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Không được phép xóa tài khoản bản thân");
        verifyNoInteractions(users, mapper);
    }

    @Test
    @DisplayName("trả về NotFoundException và không xóa khi tài khoản đích không tồn tại")
    void test_deleteUser_036() {
        assertThatThrownBy(() -> usecase.deleteUser(admin, "target"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy tài khoản để xóa");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về ForbiddenException và không xóa khi tài khoản đích là ADMIN khác")
    void test_deleteUser_037() {
        when(users.findById("target")).thenReturn(user("target", "ROLE_ADMIN", true));
        assertThatThrownBy(() -> usecase.deleteUser(admin, "target"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Không được phép xóa tài khoản của admin khác");
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi dependency và không xóa khi đọc tài khoản đích thất bại")
    void test_deleteUser_038() {
        when(users.findById("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.deleteUser(admin, "target")).isSameAs(failure);
        verify(users).findById("target");
        verifyNoMoreInteractions(users);
    }

    @Test
    @DisplayName("trả về lỗi dependency khi dịch vụ xóa tài khoản thất bại")
    void test_deleteUser_039() {
        when(users.findById("target")).thenReturn(user("target", "ROLE_MODERATOR", false));
        when(users.deleteUser("target")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.deleteUser(admin, "target")).isSameAs(failure);
        verifyNoInteractions(mapper);
    }
}
