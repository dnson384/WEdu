package com.wedu.exam_creation.auth.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.domain.repository.IVerificationRepository;
import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.dto.response.VerificationResponseDTO;
import com.wedu.exam_creation.auth.infrastructure.mapper.VerificationMapper;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.MockedConstruction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class VerificationUsecaseTest {
    private final IVerificationRepository repo = mock(IVerificationRepository.class);
    private final UserService users = mock(UserService.class);
    private final SecurityService security = mock(SecurityService.class);
    private final MailService mail = mock(MailService.class);
    private final AuthUsecase auth = mock(AuthUsecase.class);
    private final AuthDTOMapper mapper = mock(AuthDTOMapper.class);
    private final VerificationMapper copy = Mappers.getMapper(VerificationMapper.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final Instant now = Instant.parse("2026-11-13T00:00:00Z");
    private final AtomicReference<Instant> time = new AtomicReference<>(now);
    private final Map<String, VerificationEntity> storage = new HashMap<>();
    private final List<String> sent = new ArrayList<>();
    private VerificationUsecase usecase;

    @BeforeEach
    void setup() {
        when(mapper.toUserResponseDTO(any()))
                .thenAnswer(
                        call -> {
                            CommonUserResponseAllDTO user = call.getArgument(0);
                            return new UserResponseDTO(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getUsername(),
                                    user.getRole(),
                                    user.getAvatarUrl());
                        });
        Clock clock =
                new Clock() {
                    public ZoneId getZone() {
                        return ZoneOffset.UTC;
                    }

                    public Clock withZone(ZoneId zone) {
                        return this;
                    }

                    public Instant instant() {
                        return time.get();
                    }
                };
        AtomicInteger sequence = new AtomicInteger();
        try (MockedConstruction<SecureRandom> random =
                mockConstruction(
                        SecureRandom.class,
                        (generator, context) -> {
                            when(generator.nextInt(1_000_000))
                                    .thenAnswer(call -> sequence.incrementAndGet());
                            doAnswer(
                                            call -> {
                                                byte[] bytes = call.getArgument(0);
                                                Arrays.fill(
                                                        bytes, (byte) sequence.incrementAndGet());
                                                return null;
                                            })
                                    .when(generator)
                                    .nextBytes(any(byte[].class));
                        })) {
            usecase =
                    new VerificationUsecase(
                            repo,
                            users,
                            security,
                            mail,
                            auth,
                            mapper,
                            manager,
                            clock,
                            "test-only-otp-secret-of-at-least-32-characters");
        }
        when(manager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        when(security.hashPassword(anyString())).thenReturn("bcrypt-hash");
        doAnswer(
                        call -> {
                            VerificationEntity entity = (VerificationEntity) call.getArgument(0);
                            storage.put(entity.getId(), cloneEntity(entity));
                            return null;
                        })
                .when(repo)
                .insert(any());
        when(repo.find(anyString()))
                .thenAnswer(call -> cloneEntity(storage.get(call.getArgument(0))));
        when(repo.replaceOtp(any(), any()))
                .thenAnswer(
                        call -> {
                            VerificationEntity entity = call.getArgument(0);
                            VerificationEntity stored = storage.get(entity.getId());
                            if (stored.isConsumed() || stored.getRevision() != entity.getRevision())
                                return false;
                            stored = cloneEntity(entity);
                            stored.setRevision(stored.getRevision() + 1);
                            stored.setReady(false);
                            storage.put(stored.getId(), stored);
                            return true;
                        });
        doAnswer(
                        call -> {
                            storage.get(call.getArgument(0)).setReady(call.getArgument(2));
                            return null;
                        })
                .when(repo)
                .markReady(anyString(), anyString(), anyBoolean());
        doAnswer(
                        call -> {
                            sent.add(call.getArgument(1));
                            return null;
                        })
                .when(mail)
                .sendVerificationOtp(anyString(), anyString());
        doAnswer(
                        call -> {
                            storage.get(call.getArgument(0))
                                    .setAttempts(
                                            storage.get(call.getArgument(0)).getAttempts() + 1);
                            return null;
                        })
                .when(repo)
                .failAttempt(anyString(), anyString(), any());
        when(repo.consumeOtp(anyString(), anyString(), any()))
                .thenAnswer(
                        call -> {
                            VerificationEntity entity = storage.get(call.getArgument(0));
                            if (entity == null
                                    || entity.isConsumed()
                                    || !entity.isReady()
                                    || entity.getAttempts() >= 5
                                    || !entity.getOtpHash().equals(call.getArgument(1))
                                    || !entity.getOtpExpiresAt().isAfter(time.get())) return null;
                            entity.setConsumed(true);
                            return cloneEntity(entity);
                        });
        when(repo.consumeHandoff(anyString(), anyString(), any()))
                .thenAnswer(
                        call -> {
                            VerificationEntity entity = storage.get(call.getArgument(0));
                            if (entity == null
                                    || entity.isConsumed()
                                    || !entity.getBindingHash().equals(call.getArgument(1))
                                    || !entity.getExpiresAt().isAfter(time.get())) return null;
                            entity.setConsumed(true);
                            return cloneEntity(entity);
                        });
    }

    private VerificationEntity cloneEntity(VerificationEntity entity) {
        return copy.toEntity(copy.toDocument(entity));
    }

    private NewUserRequestDTO input() {
        return new NewUserRequestDTO(
                "Teacher@Example.com", "password123", "password123", "Teacher", "LOCAL");
    }

    private CommonUserResponseAllDTO teacher() {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId("teacher-id");
        user.setEmail("teacher@example.com");
        user.setUsername("Teacher");
        user.setRole("ROLE_TEACHER");
        user.setIsActive(true);
        user.setLoginMethod("LOCAL");
        return user;
    }

    private void tokens() {
        when(auth.generateToken(any(UserResponseDTO.class)))
                .thenReturn(new AuthUsecase.TokenResult(true, "at", "rt"));
    }

    private String google(String email, boolean authoritative) {
        return usecase.googleHandoff(
                email,
                "Teacher",
                "google-sub",
                true,
                authoritative,
                VerificationUsecase.hash("verifier"));
    }

    @Test
    @DisplayName(
            "trả về hồ sơ tạm chứa mật khẩu băm và gửi OTP sáu chữ số khi đăng ký LOCAL hợp lệ")
    void test_register_001() {
        VerificationUsecase.Pending pending = usecase.register(input());
        VerificationEntity entity = storage.get(VerificationUsecase.hash(pending.token()));
        assertThat(entity.getEmail()).isEqualTo("teacher@example.com");
        assertThat(entity.getHashedPassword()).isEqualTo("bcrypt-hash");
        assertThat(entity.getOtpHash()).hasSize(64).isNotEqualTo(sent.get(0));
        assertThat(sent.get(0)).matches("[0-9]{6}");
        assertThat(pending.verification().getResendAt()).isEqualTo(now.plusSeconds(120));
        verify(users, never()).createNewUser(any(), any());
        verifyNoInteractions(auth);
    }

    @Test
    @DisplayName("trả về ConflictException và không gửi OTP khi email đã tồn tại")
    void test_register_002() {
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        assertThatThrownBy(() -> usecase.register(input())).isInstanceOf(ConflictException.class);
        verifyNoInteractions(repo, mail, auth);
        verify(security, never()).hashPassword(any());
    }

    @Test
    @DisplayName(
            "trả về BadRequestException khi biểu mẫu dùng GOOGLE hoặc xác nhận mật khẩu không khớp")
    void test_register_003() {
        NewUserRequestDTO input = input();
        input.setLoginMethod("GOOGLE");
        assertThatThrownBy(() -> usecase.register(input)).isInstanceOf(BadRequestException.class);
        input.setLoginMethod("LOCAL");
        input.setConfirmPassword("different");
        assertThatThrownBy(() -> usecase.register(input)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repo, mail, auth);
    }

    @Test
    @DisplayName("trả về JWT đúng một lần khi OTP hợp lệ và không cho dùng lại")
    void test_verify_004() {
        VerificationUsecase.Pending pending = usecase.register(input());
        tokens();
        when(users.createNewUser(any(), eq("bcrypt-hash"))).thenReturn(teacher());
        assertThat(usecase.verify(pending.token(), sent.get(0)).getAccessToken()).isEqualTo("at");
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .isInstanceOf(VerificationException.class);
        verify(users, times(1)).createNewUser(any(), eq("bcrypt-hash"));
        verify(manager).commit(any());
    }

    @Test
    @DisplayName("trả về lỗi hết lượt và không tạo user khi nhập sai OTP năm lần")
    void test_verify_005() {
        VerificationUsecase.Pending pending = usecase.register(input());
        String wrong = sent.get(0).equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> usecase.verify(pending.token(), wrong))
                    .isInstanceOf(VerificationException.class);
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .hasMessageContaining("5");
        assertThat(usecase.metadata(pending.token()).getRemainingAttempts()).isZero();
        verifyNoInteractions(auth);
        verify(users, never()).createNewUser(any(), any());
    }

    @Test
    @DisplayName("trả về lỗi hết hạn khi OTP đủ năm phút hoặc phiên đủ ba mươi phút")
    void test_verify_006() {
        VerificationUsecase.Pending pending = usecase.register(input());
        time.set(now.plusSeconds(300));
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .hasMessageContaining("hết hạn");
        time.set(now.plusSeconds(1800));
        assertThatThrownBy(() -> usecase.metadata(pending.token())).hasMessageContaining("Phiên");
    }

    @Test
    @DisplayName("trả về OTP mới sau thời gian chờ khi đã đủ một trăm hai mươi giây")
    void test_resend_007() {
        VerificationUsecase.Pending pending = usecase.register(input());
        assertThatThrownBy(() -> usecase.resend(pending.token()))
                .isInstanceOf(VerificationException.class);
        String hash = storage.get(VerificationUsecase.hash(pending.token())).getOtpHash();
        time.set(now.plusSeconds(120));
        usecase.resend(pending.token());
        assertThat(sent).hasSize(2);
        assertThat(storage.get(VerificationUsecase.hash(pending.token())).getRevision())
                .isEqualTo(2);
        assertThat(storage.get(VerificationUsecase.hash(pending.token())).getOtpHash())
                .isNotEqualTo(hash);
    }

    @Test
    @DisplayName("trả về lỗi gửi OTP và không đánh dấu sẵn sàng khi SMTP thất bại")
    void test_register_008() {
        doThrow(new RuntimeException("SMTP failure"))
                .when(mail)
                .sendVerificationOtp(anyString(), anyString());
        assertThatThrownBy(() -> usecase.register(input()))
                .isInstanceOf(VerificationException.class)
                .hasMessageContaining("Không thể gửi");
        assertThat(storage.values()).allMatch(entity -> !entity.isReady());
        verifyNoInteractions(auth);
    }

    @Test
    @DisplayName("trả về lỗi giới hạn và không gửi email khi quota email đã hết")
    void test_register_009() {
        doThrow(new VerificationException("OTP_RATE_LIMIT", "wait", 429, 120))
                .when(repo)
                .reserveEmail(eq("teacher@example.com"), any());
        assertThatThrownBy(() -> usecase.register(input()))
                .isInstanceOf(VerificationException.class);
        verifyNoInteractions(mail, auth);
    }

    @Test
    @DisplayName("trả về ConflictException và rollback khi email được đăng ký trong lúc chờ OTP")
    void test_verify_010() {
        VerificationUsecase.Pending pending = usecase.register(input());
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .isInstanceOf(ConflictException.class);
        verify(manager).rollback(any());
        verifyNoInteractions(auth);
    }

    @Test
    @DisplayName("trả về InternalServerException và rollback khi lưu refresh token thất bại")
    void test_verify_011() {
        VerificationUsecase.Pending pending = usecase.register(input());
        when(users.createNewUser(any(), any())).thenReturn(teacher());
        when(auth.generateToken(any())).thenReturn(new AuthUsecase.TokenResult(false, "at", "rt"));
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .isInstanceOf(InternalServerException.class);
        verify(manager).rollback(any());
        verify(manager, never()).commit(any());
    }

    @Test
    @DisplayName("trả về phiên chờ OTP khi tài khoản Google chưa tồn tại")
    void test_exchange_012() {
        VerificationUsecase.Exchange result =
                usecase.exchange(google("teacher@example.com", true), "verifier");
        assertThat(result.authorized()).isNull();
        assertThat(result.pending()).isNotNull();
        assertThat(storage.get(VerificationUsecase.hash(result.pending().token())).getPurpose())
                .isEqualTo("GOOGLE_REGISTER");
        verifyNoInteractions(auth);
        verify(users, never()).createNewUser(any(), any());
    }

    @Test
    @DisplayName("trả về tài khoản GOOGLE không có mật khẩu LOCAL khi xác thực OTP thành công")
    void test_verify_013() {
        VerificationUsecase.Exchange result =
                usecase.exchange(google("teacher@example.com", true), "verifier");
        tokens();
        when(users.createNewUser(any(), eq(""))).thenReturn(teacher());
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(teacher());
        usecase.verify(result.pending().token(), sent.get(0));
        verify(users)
                .createNewUser(argThat(input -> input.getLoginMethod().equals("GOOGLE")), eq(""));
        verify(users).linkGoogle("teacher-id", "google-sub");
    }

    @Test
    @DisplayName("trả về liên kết Google sau OTP khi email ngoài chưa liên kết")
    void test_verify_014() {
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        VerificationUsecase.Exchange result =
                usecase.exchange(google("teacher@example.com", false), "verifier");
        verify(users, never()).linkGoogle(any(), any());
        tokens();
        when(users.findById("teacher-id")).thenReturn(teacher());
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(teacher());
        usecase.verify(result.pending().token(), sent.get(0));
        verify(users).linkGoogle("teacher-id", "google-sub");
        verify(users, never()).createNewUser(any(), any());
    }

    @Test
    @DisplayName("trả về JWT và liên kết trực tiếp khi email Google có thẩm quyền trùng tài khoản")
    void test_exchange_015() {
        CommonUserResponseAllDTO teacher = teacher();
        teacher.setEmail("teacher@gmail.com");
        when(users.findByEmail("teacher@gmail.com")).thenReturn(Optional.of(teacher));
        CommonUserResponseAllDTO mixed = teacher();
        mixed.setEmail("teacher@gmail.com");
        mixed.setLoginMethod("MIXED");
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(mixed);
        tokens();
        VerificationUsecase.Exchange result =
                usecase.exchange(google("teacher@gmail.com", true), "verifier");
        assertThat(result.authorized()).isNotNull();
        verify(users).linkGoogle("teacher-id", "google-sub");
        verifyNoInteractions(mail);
    }

    @Test
    @DisplayName("trả về JWT không gửi OTP khi email ngoài đã liên kết Google")
    void test_exchange_016() {
        CommonUserResponseAllDTO mixed = teacher();
        mixed.setLoginMethod("MIXED");
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(mixed));
        when(users.hasGoogleLink("teacher@example.com", "google-sub")).thenReturn(true);
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(mixed);
        tokens();
        assertThat(usecase.exchange(google("teacher@example.com", false), "verifier").authorized())
                .isNotNull();
        verify(users, atLeastOnce()).findByEmail("teacher@example.com");
        verifyNoInteractions(mail);
    }

    @Test
    @DisplayName("trả về phiên chờ OTP khi Google đổi email và không tìm user theo subject cũ")
    void test_exchange_017() {
        VerificationUsecase.Exchange result =
                usecase.exchange(google("changed@gmail.com", true), "verifier");
        assertThat(result.pending()).isNotNull();
        assertThat(result.authorized()).isNull();
        verify(users).findByEmail("changed@gmail.com");
        verify(users, never()).linkGoogle(any(), any());
        verifyNoInteractions(auth);
    }

    @Test
    @DisplayName("trả về ConflictException và rollback khi tài khoản đã liên kết subject khác")
    void test_exchange_018() {
        CommonUserResponseAllDTO user = teacher();
        user.setEmail("teacher@gmail.com");
        when(users.findByEmail("teacher@gmail.com")).thenReturn(Optional.of(user));
        when(users.linkGoogle("teacher-id", "google-sub"))
                .thenThrow(new ConflictException("Already linked"));
        assertThatThrownBy(() -> usecase.exchange(google("teacher@gmail.com", true), "verifier"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(auth);
        verify(manager).rollback(any());
    }

    @Test
    @DisplayName("trả về ConflictException không cấp JWT khi Google subject thuộc email khác")
    void test_exchange_019() {
        CommonUserResponseAllDTO user = teacher();
        user.setEmail("teacher@gmail.com");
        when(users.findByEmail("teacher@gmail.com")).thenReturn(Optional.of(user));
        when(users.linkGoogle("teacher-id", "google-sub"))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate subject"));
        assertThatThrownBy(() -> usecase.exchange(google("teacher@gmail.com", true), "verifier"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("tài khoản khác");
        verifyNoInteractions(auth);
        verify(manager).rollback(any());
    }

    @Test
    @DisplayName("trả về ForbiddenException khi user không phải giáo viên hoặc bị khóa")
    void test_exchange_020() {
        for (String role : List.of("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")) {
            CommonUserResponseAllDTO user = teacher();
            user.setRole(role);
            when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(user));
            String code = google("teacher@example.com", true);
            assertThatThrownBy(() -> usecase.exchange(code, "verifier"))
                    .isInstanceOf(ForbiddenException.class);
        }
        CommonUserResponseAllDTO locked = teacher();
        locked.setIsActive(false);
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(locked));
        assertThatThrownBy(() -> usecase.exchange(google("teacher@example.com", true), "verifier"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(auth, mail);
    }

    @Test
    @DisplayName(
            "trả về lỗi xác thực khi trình duyệt khác đổi handoff hoặc handoff đã dùng hay hết hạn")
    void test_exchange_021() {
        String code = google("teacher@example.com", true);
        assertThatThrownBy(() -> usecase.exchange(code, "another-browser"))
                .isInstanceOf(VerificationException.class);
        usecase.exchange(code, "verifier");
        assertThatThrownBy(() -> usecase.exchange(code, "verifier"))
                .isInstanceOf(VerificationException.class);
        String expiredCode = google("teacher@example.com", true);
        time.set(now.plusSeconds(60));
        assertThatThrownBy(() -> usecase.exchange(expiredCode, "verifier"))
                .isInstanceOf(VerificationException.class);
    }

    @Test
    @DisplayName("trả về BadRequestException trước persistence khi Google email chưa xác minh")
    void test_googleHandoff_022() {
        assertThatThrownBy(
                        () ->
                                usecase.googleHandoff(
                                        "teacher@gmail.com",
                                        "Teacher",
                                        "sub",
                                        false,
                                        true,
                                        "a".repeat(64)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repo, users, auth, mail);
    }

    @Test
    @DisplayName("trả về metadata che email và thời gian cố định khi phiên còn hiệu lực")
    void test_metadata_023() {
        VerificationUsecase.Pending pending = usecase.register(input());
        VerificationResponseDTO metadata = usecase.metadata(pending.token());
        assertThat(metadata.getEmail()).isEqualTo("t***@example.com");
        assertThat(metadata.getServerTime()).isEqualTo(now);
        assertThat(metadata.getRemainingAttempts()).isEqualTo(5);
        assertThat(metadata.getExpiresAt()).isEqualTo(now.plusSeconds(1800));
    }

    @Test
    @DisplayName("trả về VERIFICATION_EXPIRED khi token không có đúng định dạng")
    void test_metadata_024() {
        assertThatThrownBy(() -> usecase.metadata("invalid"))
                .isInstanceOfSatisfying(
                        VerificationException.class,
                        error -> {
                            assertThat(error.getCode()).isEqualTo("VERIFICATION_EXPIRED");
                            assertThat(error.getStatus()).isEqualTo(401);
                        });
        verifyNoInteractions(repo, auth, mail);
    }

    @Test
    @DisplayName("trả về VERIFICATION_EXPIRED khi phiên không tồn tại trong repository")
    void test_metadata_025() {
        assertThatThrownBy(() -> usecase.metadata("a".repeat(64)))
                .isInstanceOfSatisfying(
                        VerificationException.class,
                        error -> assertThat(error.getCode()).isEqualTo("VERIFICATION_EXPIRED"));
        verifyNoInteractions(auth, mail);
    }

    @Test
    @DisplayName(
            "trả về ConflictException và không gửi email khi phiên thay đổi trước lúc ghi OTP gửi lại")
    void test_resend_026() {
        VerificationUsecase.Pending pending = usecase.register(input());
        time.set(now.plusSeconds(120));
        doReturn(false).when(repo).replaceOtp(any(VerificationEntity.class), eq(time.get()));
        clearInvocations(mail, repo);

        assertThatThrownBy(() -> usecase.resend(pending.token()))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(mail, auth);
        verify(repo, never()).markReady(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("trả về OTP có hạn không vượt quá hạn phiên khi gửi lại gần cuối phiên xác thực")
    void test_resend_027() {
        VerificationUsecase.Pending pending = usecase.register(input());
        time.set(now.plusSeconds(1799));

        VerificationResponseDTO metadata = usecase.resend(pending.token());

        assertThat(metadata.getOtpExpiresAt()).isEqualTo(now.plusSeconds(1800));
        assertThat(metadata.getExpiresAt()).isEqualTo(metadata.getOtpExpiresAt());
        assertThat(metadata.getRemainingAttempts()).isEqualTo(5);
        assertThat(sent).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName(
            "trả về mã handoff và lưu định danh đã chuẩn hóa có hạn 60 giây khi email Google đã xác minh")
    void test_googleHandoff_028() {
        String code =
                usecase.googleHandoff(
                        "Teacher@Example.COM", "", "google-subject", true, true, "a".repeat(64));

        assertThat(code).matches("[a-f0-9]{64}");
        VerificationEntity identity = storage.get(VerificationUsecase.hash(code));
        assertThat(identity).isNotNull();
        assertThat(identity.getId()).isNotEqualTo(code);
        assertThat(identity.getPurpose()).isEqualTo("HANDOFF");
        assertThat(identity.getEmail()).isEqualTo("teacher@example.com");
        assertThat(identity.getUsername()).isEqualTo("Teacher");
        assertThat(identity.getGoogleSubject()).isEqualTo("google-subject");
        assertThat(identity.getBindingHash()).isEqualTo("a".repeat(64));
        assertThat(identity.isAuthoritativeEmail()).isTrue();
        assertThat(identity.getExpiresAt()).isEqualTo(now.plusSeconds(60));
        verifyNoInteractions(users, auth, mail);
    }
}
