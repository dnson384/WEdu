package com.wedu.exam_creation.user.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

class UserUsecaseTest {
    private static final String ID = "teacher-1";
    private static final String EMAIL = "teacher@example.com";
    private static final String DEFAULT_AVATAR = "avatars/default-avatar-user.png";
    private static final String OLD_AVATAR = "avatars/old.png";
    private static final String NEW_AVATAR = "avatars/new.png";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final UserRepositoryImpl repo = mock(UserRepositoryImpl.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final S3Service storage = mock(S3Service.class);
    private final TelegramNotificationService notifications =
            mock(TelegramNotificationService.class);
    private final UserDTOMapper mapper = mock(UserDTOMapper.class);
    private final UserCommonDTOMapper commonMapper = mock(UserCommonDTOMapper.class);
    private final UserUsecase usecase =
            new UserUsecase(repo, mapper, commonMapper, refreshTokens, storage, notifications);

    @Test
    @DisplayName(
            "trả về tài khoản giáo viên FREE với avatar mặc định khi tạo mới bằng mật khẩu đã băm")
    void test_createNewUser_001() {
        NewUserRequestDTO request =
                new NewUserRequestDTO(EMAIL, "plain-fixture", "plain-fixture", "Tên", "LOCAL");
        UserEntity saved = user(true, DEFAULT_AVATAR);
        CommonUserResponseAllDTO expected = stubAccount(saved);
        when(repo.save(any(UserEntity.class))).thenReturn(saved);

        CommonUserResponseAllDTO result;
        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(NOW);
            result = usecase.createNewUser(request, "new-hash");
        }

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<UserEntity> pending = ArgumentCaptor.forClass(UserEntity.class);
        verify(repo).save(pending.capture());
        assertThat(pending.getValue())
                .isEqualTo(
                        new UserEntity(
                                null,
                                EMAIL,
                                "new-hash",
                                "Tên",
                                "ROLE_TEACHER",
                                "LOCAL",
                                DEFAULT_AVATAR,
                                true,
                                "FREE",
                                NOW,
                                NOW));
    }

    @Test
    @DisplayName("trả về lỗi lưu trữ gốc khi repository không thể tạo tài khoản")
    void test_createNewUser_002() {
        NewUserRequestDTO request =
                new NewUserRequestDTO(EMAIL, "plain-fixture", "plain-fixture", "Tên", "LOCAL");
        ConflictException failure = new ConflictException("Email đã tồn tại");
        when(repo.save(any(UserEntity.class))).thenThrow(failure);

        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(NOW);
            assertThatThrownBy(() -> usecase.createNewUser(request, "new-hash")).isSameAs(failure);
        }
    }

    @Test
    @DisplayName("trả về hồ sơ có URL xem ảnh khi S3 ký được khóa avatar trong hồ sơ")
    void test_getMe_003() {
        CommonUserResponseAllDTO account = new CommonUserResponseAllDTO();
        account.setAvatarUrl(OLD_AVATAR);
        CommonUserResponseDTO profile = new CommonUserResponseDTO();
        profile.setId(ID);
        profile.setEmail(EMAIL);
        profile.setAvatarUrl(OLD_AVATAR);
        when(commonMapper.commonAllToCommonDTO(account)).thenReturn(profile);
        String url = "https://example.test/avatar";
        when(storage.generatePresignedUrl(OLD_AVATAR)).thenReturn(url);

        CommonUserResponseDTO result = usecase.getMe(account);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getAvatarUrl()).isEqualTo(url);
        assertThat(account.getAvatarUrl()).isEqualTo(OLD_AVATAR);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("trả về lỗi ký URL gốc khi S3 không tạo được URL avatar")
    void test_getMe_004() {
        CommonUserResponseAllDTO account = new CommonUserResponseAllDTO();
        account.setAvatarUrl(OLD_AVATAR);
        CommonUserResponseDTO profile = new CommonUserResponseDTO();
        profile.setId(ID);
        profile.setEmail(EMAIL);
        profile.setAvatarUrl(OLD_AVATAR);
        when(commonMapper.commonAllToCommonDTO(account)).thenReturn(profile);
        IllegalStateException failure = new IllegalStateException("signing failed");
        when(storage.generatePresignedUrl(OLD_AVATAR)).thenThrow(failure);

        assertThatThrownBy(() -> usecase.getMe(account)).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về kết quả so khớp subject Google khi email tìm thấy tài khoản")
    void test_hasGoogleLink_005(boolean matching) {
        UserEntity account = user(true, DEFAULT_AVATAR);
        account.setGoogleSubject("linked-subject");
        when(repo.findByEmail(EMAIL)).thenReturn(Optional.of(account));
        String subject = matching ? "linked-subject" : "different-subject";

        assertThat(usecase.hasGoogleLink(EMAIL, subject)).isEqualTo(matching);
        verify(repo).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("trả về false khi email chưa có người dùng để xác nhận liên kết Google")
    void test_hasGoogleLink_006() {
        when(repo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(usecase.hasGoogleLink(EMAIL, "subject")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOCAL", "MIXED", "GOOGLE"})
    @DisplayName(
            "trả về phương thức đăng nhập tương ứng khi liên kết Google cho tài khoản có thông tin hợp lệ")
    void test_linkGoogle_007(String method) {
        UserEntity account = user(true, DEFAULT_AVATAR);
        account.setLoginMethod(method);
        String expected = method.equals("GOOGLE") ? "GOOGLE" : "MIXED";
        String password = method.equals("GOOGLE") ? null : "stored-hash";
        account.setHashedPassword(password);
        UserEntity linked = user(true, DEFAULT_AVATAR);
        linked.setLoginMethod(expected);
        linked.setHashedPassword(password);
        CommonUserResponseAllDTO response = stubAccount(linked);
        when(repo.findById(ID)).thenReturn(account);
        when(repo.linkGoogle(ID, "subject", expected)).thenReturn(linked);

        CommonUserResponseAllDTO result = usecase.linkGoogle(ID, "subject");

        assertThat(result).isSameAs(response);
        verify(repo).linkGoogle(ID, "subject", expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName(
            "trả về BadRequestException khi gắn liên kết Google cho tài khoản LOCAL thiếu mật khẩu")
    void test_linkGoogle_008(String password) {
        UserEntity account = user(true, DEFAULT_AVATAR);
        account.setHashedPassword(password);
        when(repo.findById(ID)).thenReturn(account);

        assertThatThrownBy(() -> usecase.linkGoogle(ID, "subject"))
                .isInstanceOf(BadRequestException.class);
        verify(repo, never()).linkGoogle(any(), any(), any());
    }

    @Test
    @DisplayName("trả về NotFoundException khi không tìm thấy tài khoản để liên kết Google")
    void test_linkGoogle_009() {
        assertThatThrownBy(() -> usecase.linkGoogle(ID, "subject"))
                .isInstanceOf(NotFoundException.class);
        verify(repo, never()).linkGoogle(any(), any(), any());
    }

    @Test
    @DisplayName("trả về ConflictException khi repository từ chối thay đổi liên kết Google")
    void test_linkGoogle_010() {
        when(repo.findById(ID)).thenReturn(user(true, DEFAULT_AVATAR));
        ConflictException failure = new ConflictException("Không thể liên kết tài khoản Google");
        when(repo.linkGoogle(ID, "subject", "MIXED")).thenThrow(failure);

        assertThatThrownBy(() -> usecase.linkGoogle(ID, "subject")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về thông tin đăng nhập đã ánh xạ khi tìm thấy người dùng theo email")
    void test_findByEmail_011() {
        UserEntity account = user(true, DEFAULT_AVATAR);
        CommonUserResponseAllDTO response = stubAccount(account);
        when(repo.findByEmail(EMAIL)).thenReturn(Optional.of(account));

        assertThat(usecase.findByEmail(EMAIL)).containsSame(response);
    }

    @Test
    @DisplayName("trả về Optional rỗng khi không tìm thấy người dùng theo email")
    void test_findByEmail_012() {
        when(repo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(usecase.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("trả về đúng tài khoản đã ánh xạ khi ID tồn tại")
    void test_findById_013() {
        UserEntity account = user(true, DEFAULT_AVATAR);
        CommonUserResponseAllDTO response = stubAccount(account);
        when(repo.findById(ID)).thenReturn(account);

        assertThat(usecase.findById(ID)).isSameAs(response);
    }

    @Test
    @DisplayName("trả về null khi repository không tìm thấy người dùng theo ID")
    void test_findById_014() {
        assertThat(usecase.findById(ID)).isNull();
    }

    @Test
    @DisplayName("trả về danh sách thông tin công khai khi hệ thống có người dùng")
    void test_getAllUsers_015() {
        UserEntity account = user(true, OLD_AVATAR);
        CommonUserResponseDTO response = new CommonUserResponseDTO();
        when(repo.all()).thenReturn(List.of(account));
        when(mapper.toCommonDTO(account)).thenReturn(response);

        assertThat(usecase.getAllUsers()).containsExactly(response);
    }

    @Test
    @DisplayName("trả về NotFoundException khi hệ thống chưa có người dùng")
    void test_getAllUsers_016() {
        when(repo.all()).thenReturn(List.of());

        assertThatThrownBy(() -> usecase.getAllUsers()).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("trả về người dùng khớp khi tìm bằng từ khóa có khoảng trắng ở hai đầu")
    void test_findUserByKeyword_017() {
        UserEntity account = user(true, DEFAULT_AVATAR);
        CommonUserResponseDTO response = new CommonUserResponseDTO();
        when(repo.findByKeyword("teacher")).thenReturn(List.of(account));
        when(mapper.toCommonDTO(account)).thenReturn(response);

        assertThat(usecase.findUserByKeyword("  teacher  ")).containsExactly(response);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("trả về BadRequestException khi từ khóa tìm người dùng bị bỏ trống")
    void test_findUserByKeyword_018(String keyword) {
        assertThatThrownBy(() -> usecase.findUserByKeyword(keyword))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("trả về NotFoundException khi không có người dùng khớp từ khóa")
    void test_findUserByKeyword_019() {
        when(repo.findByKeyword("missing")).thenReturn(List.of());

        assertThatThrownBy(() -> usecase.findUserByKeyword("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("trả về true và xóa ảnh cũ sau khi lưu thành công khóa avatar mới")
    void test_updateAvatar_020() {
        when(repo.findById(ID)).thenReturn(user(true, OLD_AVATAR));
        UserUpdateFields fields = new UserUpdateFields();
        fields.setS3Key(NEW_AVATAR);
        when(repo.updateField(ID, fields)).thenReturn(user(true, NEW_AVATAR));

        assertThat(usecase.updateAvatar(ID, NEW_AVATAR)).isTrue();

        InOrder order = inOrder(repo, storage);
        order.verify(repo).updateField(ID, fields);
        order.verify(storage).deleteFile(OLD_AVATAR);
        verifyNoInteractions(notifications);
    }

    @Test
    @DisplayName("trả về true và giữ ảnh mặc định khi lưu thành công avatar riêng")
    void test_updateAvatar_021() {
        when(repo.findById(ID)).thenReturn(user(true, DEFAULT_AVATAR));
        when(repo.updateField(eq(ID), any(UserUpdateFields.class)))
                .thenReturn(user(true, NEW_AVATAR));

        assertThat(usecase.updateAvatar(ID, NEW_AVATAR)).isTrue();
        verifyNoInteractions(storage, notifications);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về InternalServerException và thử dọn ảnh mới khi lưu avatar thất bại")
    void test_updateAvatar_022(boolean cleanupFails) {
        when(repo.findById(ID)).thenReturn(user(true, OLD_AVATAR));
        if (cleanupFails) {
            doThrow(new IllegalStateException("S3 unavailable"))
                    .when(storage)
                    .deleteFile(NEW_AVATAR);
        }

        assertThatThrownBy(() -> usecase.updateAvatar(ID, NEW_AVATAR))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Có lỗi trong quá trình cập nhật avatar");
        verify(storage).deleteFile(NEW_AVATAR);
        verifyNoMoreInteractions(storage);
        verifyNoInteractions(notifications);
    }

    @Test
    @DisplayName(
            "trả về true và gửi thông báo dọn ảnh khi lưu được avatar mới nhưng S3 không xóa được ảnh cũ")
    void test_updateAvatar_023() {
        when(repo.findById(ID)).thenReturn(user(true, OLD_AVATAR));
        when(repo.updateField(eq(ID), any(UserUpdateFields.class)))
                .thenReturn(user(true, NEW_AVATAR));
        doThrow(new IllegalStateException("S3 unavailable")).when(storage).deleteFile(OLD_AVATAR);

        assertThat(usecase.updateAvatar(ID, NEW_AVATAR)).isTrue();
        verify(notifications)
                .notifyFailedDeleteImage(
                        "Không xóa được ảnh cũ " + OLD_AVATAR + ": S3 unavailable");
    }

    @Test
    @DisplayName("trả về BadRequestException khi khóa avatar mới rỗng")
    void test_updateAvatar_024() {
        assertThatThrownBy(() -> usecase.updateAvatar(ID, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("s3Key rỗng");
        verifyNoInteractions(repo, storage, notifications);
    }

    @Test
    @DisplayName("trả về ForbiddenException khi tài khoản bị khóa yêu cầu đổi avatar")
    void test_updateAvatar_025() {
        when(repo.findById(ID)).thenReturn(user(false, OLD_AVATAR));

        assertThatThrownBy(() -> usecase.updateAvatar(ID, NEW_AVATAR))
                .isInstanceOf(ForbiddenException.class);
        verify(repo, never()).updateField(any(), any());
        verifyNoInteractions(storage, notifications);
    }

    @Test
    @DisplayName("trả về NotFoundException khi tài khoản cần đổi avatar không tồn tại")
    void test_updateAvatar_026() {
        assertThatThrownBy(() -> usecase.updateAvatar(ID, NEW_AVATAR))
                .isInstanceOf(NotFoundException.class);
        verify(repo, never()).updateField(any(), any());
        verifyNoInteractions(storage, notifications);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nguyễn Văn A", "Teacher123", "GV🎓"})
    @DisplayName("trả về true và giữ nguyên tên mới khi tên có Unicode, chữ số hoặc emoji")
    void test_updateUsername_027(String name) {
        when(repo.findById(ID)).thenReturn(user(true, DEFAULT_AVATAR));
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUsername(name);
        when(repo.updateField(ID, fields)).thenReturn(user(true, DEFAULT_AVATAR));

        assertThat(usecase.updateUsername(ID, name)).isTrue();
        verify(repo).updateField(ID, fields);
    }

    @Test
    @DisplayName(
            "trả về true khi tên còn đúng 254 ký tự sau khi bỏ khoảng trắng hai đầu để kiểm tra")
    void test_updateUsername_028() {
        String name = " " + "a".repeat(254) + " ";
        when(repo.findById(ID)).thenReturn(user(true, DEFAULT_AVATAR));
        UserUpdateFields fields = new UserUpdateFields();
        fields.setUsername(name);
        when(repo.updateField(ID, fields)).thenReturn(user(true, DEFAULT_AVATAR));

        assertThat(usecase.updateUsername(ID, name)).isTrue();
        verify(repo).updateField(ID, fields);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("trả về BadRequestException khi tên mới rỗng hoặc chỉ có khoảng trắng")
    void test_updateUsername_029(String name) {
        assertThatThrownBy(() -> usecase.updateUsername(ID, name))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("trả về BadRequestException khi tên mới vượt quá 254 ký tự")
    void test_updateUsername_030() {
        String name = "a".repeat(255);

        assertThatThrownBy(() -> usecase.updateUsername(ID, name))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng quá dài");
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("trả về InternalServerException khi repository không lưu được tên người dùng")
    void test_updateUsername_031() {
        when(repo.findById(ID)).thenReturn(user(true, DEFAULT_AVATAR));

        assertThatThrownBy(() -> usecase.updateUsername(ID, "Tên mới"))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("trả về ForbiddenException khi tài khoản bị khóa yêu cầu đổi tên")
    void test_updateUsername_032() {
        when(repo.findById(ID)).thenReturn(user(false, DEFAULT_AVATAR));

        assertThatThrownBy(() -> usecase.updateUsername(ID, "Tên mới"))
                .isInstanceOf(ForbiddenException.class);
        verify(repo, never()).updateField(any(), any());
    }

    @Test
    @DisplayName("trả về NotFoundException khi tài khoản cần đổi tên không tồn tại")
    void test_updateUsername_033() {
        assertThatThrownBy(() -> usecase.updateUsername(ID, "Tên mới"))
                .isInstanceOf(NotFoundException.class);
        verify(repo, never()).updateField(any(), any());
    }

    @Test
    @DisplayName("trả về hồ sơ với vai trò mới khi repository cập nhật vai trò thành công")
    void test_updateRole_034() {
        UserEntity account = user(true, DEFAULT_AVATAR);
        account.setRole("ROLE_MODERATOR");
        CommonUserResponseAllDTO request = new CommonUserResponseAllDTO();
        CommonUserResponseAllDTO updated = stubAccount(account);
        when(mapper.commonAllToEntity(request)).thenReturn(account);
        when(repo.updateRole(ID, "ROLE_MODERATOR")).thenReturn(account);

        assertThat(usecase.updateRole(request)).isSameAs(updated);
    }

    @Test
    @DisplayName("trả về InternalServerException khi repository không cập nhật được vai trò")
    void test_updateRole_035() {
        CommonUserResponseAllDTO request = new CommonUserResponseAllDTO();
        when(mapper.commonAllToEntity(request)).thenReturn(user(true, DEFAULT_AVATAR));

        assertThatThrownBy(() -> usecase.updateRole(request))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("trả về tài khoản bị khóa khi lưu trạng thái không hoạt động thành công")
    void test_lockUser_036() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setIsActive(false);
        UserEntity account = user(false, DEFAULT_AVATAR);
        CommonUserResponseAllDTO expected = stubAccount(account);
        when(repo.updateField(ID, fields)).thenReturn(account);

        assertThat(usecase.lockUser(ID)).isSameAs(expected);
    }

    @Test
    @DisplayName("trả về InternalServerException khi repository không khóa được tài khoản")
    void test_lockUser_037() {
        assertThatThrownBy(() -> usecase.lockUser(ID)).isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("trả về tài khoản hoạt động khi lưu trạng thái mở khóa thành công")
    void test_unlockUser_038() {
        UserUpdateFields fields = new UserUpdateFields();
        fields.setIsActive(true);
        UserEntity account = user(true, DEFAULT_AVATAR);
        CommonUserResponseAllDTO expected = stubAccount(account);
        when(repo.updateField(ID, fields)).thenReturn(account);

        assertThat(usecase.unlockUser(ID)).isSameAs(expected);
    }

    @Test
    @DisplayName("trả về InternalServerException khi repository không mở khóa được tài khoản")
    void test_unlockUser_039() {
        assertThatThrownBy(() -> usecase.unlockUser(ID))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("trả về hồ sơ có mật khẩu băm mới khi repository cập nhật mật khẩu thành công")
    void test_updatePassword_040() {
        UserEntity account = user(true, DEFAULT_AVATAR);
        account.setHashedPassword("new-hash");
        CommonUserResponseAllDTO request = new CommonUserResponseAllDTO();
        CommonUserResponseAllDTO updated = stubAccount(account);
        when(mapper.commonAllToEntity(request)).thenReturn(account);
        when(repo.updatePassword(ID, "new-hash")).thenReturn(account);

        assertThat(usecase.updatePassword(request)).isSameAs(updated);
    }

    @Test
    @DisplayName("trả về InternalServerException khi repository không cập nhật được mật khẩu")
    void test_updatePassword_041() {
        CommonUserResponseAllDTO request = new CommonUserResponseAllDTO();
        when(mapper.commonAllToEntity(request)).thenReturn(user(true, DEFAULT_AVATAR));

        assertThatThrownBy(() -> usecase.updatePassword(request))
                .isInstanceOf(InternalServerException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về hạn PLUS mới theo hạn hiện có khi repository lưu thành công")
    void test_grantPaidPlus_042(boolean currentPlus) {
        Instant existing = currentPlus ? NOW.plus(5, ChronoUnit.DAYS) : NOW.minusSeconds(1);
        Instant expiration = NOW.plus(currentPlus ? 35 : 30, ChronoUnit.DAYS);
        UserEntity account = user(true, DEFAULT_AVATAR);
        account.setPlusExpiresAt(existing);
        when(repo.findById(ID)).thenReturn(account);
        when(repo.grantPlus(ID, expiration)).thenReturn(account);

        assertThat(usecase.grantPaidPlus(ID, NOW, 30)).isEqualTo(expiration);
    }

    @Test
    @DisplayName("trả về null khi người dùng cần cấp PLUS không tồn tại")
    void test_grantPaidPlus_043() {
        assertThat(usecase.grantPaidPlus(ID, NOW, 30)).isNull();
        verify(repo, never()).grantPlus(any(), any());
    }

    @Test
    @DisplayName("trả về null khi repository không lưu được hạn PLUS")
    void test_grantPaidPlus_044() {
        when(repo.findById(ID)).thenReturn(user(true, DEFAULT_AVATAR));

        assertThat(usecase.grantPaidPlus(ID, NOW, 30)).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về kết quả thu hồi refresh token khi xóa được tài khoản có phiên đăng nhập")
    void test_deleteUser_045(boolean revoked) {
        when(repo.delete(ID)).thenReturn(true);
        when(refreshTokens.getUserRefreshToken(ID))
                .thenReturn(
                        List.of(
                                new RTResponseDTO("token-1", "jti-1", ID, NOW.plusSeconds(60), NOW),
                                new RTResponseDTO(
                                        "token-2", "jti-2", ID, NOW.plusSeconds(60), NOW)));
        when(refreshTokens.deleteMany(List.of("jti-1", "jti-2"))).thenReturn(revoked);

        assertThat(usecase.deleteUser(ID)).isEqualTo(revoked);
        verify(refreshTokens).deleteMany(List.of("jti-1", "jti-2"));
    }

    @Test
    @DisplayName("trả về true khi tài khoản bị xóa và không có refresh token cần thu hồi")
    void test_deleteUser_046() {
        when(repo.delete(ID)).thenReturn(true);
        when(refreshTokens.getUserRefreshToken(ID)).thenReturn(List.of());

        assertThat(usecase.deleteUser(ID)).isTrue();
        verify(refreshTokens, never()).deleteMany(anyList());
    }

    @Test
    @DisplayName("trả về false và giữ các phiên khi repository không xóa tài khoản")
    void test_deleteUser_047() {
        assertThat(usecase.deleteUser(ID)).isFalse();
        verifyNoInteractions(refreshTokens);
    }

    private CommonUserResponseAllDTO stubAccount(UserEntity account) {
        CommonUserResponseAllDTO response = new CommonUserResponseAllDTO();
        when(mapper.toCommonAllDTO(account)).thenReturn(response);
        return response;
    }

    private UserEntity user(boolean active, String avatar) {
        return new UserEntity(
                ID,
                EMAIL,
                "stored-hash",
                "Tên",
                "ROLE_TEACHER",
                "LOCAL",
                avatar,
                active,
                "FREE",
                NOW,
                NOW);
    }
}
