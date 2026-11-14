package com.wedu.exam_creation.auth.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.domain.repository.IVerificationRepository;
import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.infrastructure.mapper.VerificationMapper;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class VerificationUsecaseTest {
    private final IVerificationRepository repo = mock(IVerificationRepository.class);
    private final UserService users = mock(UserService.class);
    private final SecurityService security = mock(SecurityService.class);
    private final MailService mail = mock(MailService.class);
    private final AuthUsecase auth = mock(AuthUsecase.class);
    private final AuthDTOMapper mapper = Mappers.getMapper(AuthDTOMapper.class);
    private final VerificationMapper copy = Mappers.getMapper(VerificationMapper.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final Instant now = Instant.parse("2026-11-13T00:00:00Z");
    private final AtomicReference<Instant> time = new AtomicReference<>(now);
    private final Map<String, VerificationEntity> storage = new HashMap<>();
    private final List<String> sent = new ArrayList<>();
    private VerificationUsecase usecase;

    @BeforeEach
    void setup() {
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
        when(manager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        when(security.hashPassword(anyString())).thenReturn("bcrypt-hash");
        doAnswer(
                        call -> {
                            var entity = (VerificationEntity) call.getArgument(0);
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
                            var stored = storage.get(entity.getId());
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
                            var entity = storage.get(call.getArgument(0));
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
                            var entity = storage.get(call.getArgument(0));
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
        var user = new CommonUserResponseAllDTO();
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
    void registrationOnlyStoresHashAndSendsSixDigits() {
        var pending = usecase.register(input());
        var entity = storage.get(VerificationUsecase.hash(pending.token()));
        assertThat(entity.getEmail()).isEqualTo("teacher@example.com");
        assertThat(entity.getHashedPassword()).isEqualTo("bcrypt-hash");
        assertThat(entity.getOtpHash()).hasSize(64).isNotEqualTo(sent.get(0));
        assertThat(sent.get(0)).matches("[0-9]{6}");
        assertThat(pending.verification().getResendAt()).isEqualTo(now.plusSeconds(120));
        verify(users, never()).createNewUser(any(), any());
        verifyNoInteractions(auth);
    }

    @Test
    void existingEmailDoesNotSendOrSavePending() {
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        assertThatThrownBy(() -> usecase.register(input())).isInstanceOf(ConflictException.class);
        verifyNoInteractions(repo, mail, auth);
        verify(security, never()).hashPassword(any());
    }

    @Test
    void rejectsGoogleByFormAndMismatchedPassword() {
        var input = input();
        input.setLoginMethod("GOOGLE");
        assertThatThrownBy(() -> usecase.register(input)).isInstanceOf(BadRequestException.class);
        input.setLoginMethod("LOCAL");
        input.setConfirmPassword("different");
        assertThatThrownBy(() -> usecase.register(input)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repo, mail, auth);
    }

    @Test
    void correctOtpCreatesUserAndIssuesTokensOnce() {
        var pending = usecase.register(input());
        tokens();
        when(users.createNewUser(any(), eq("bcrypt-hash"))).thenReturn(teacher());
        assertThat(usecase.verify(pending.token(), sent.get(0)).getAccessToken()).isEqualTo("at");
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .isInstanceOf(VerificationException.class);
        verify(users, times(1)).createNewUser(any(), eq("bcrypt-hash"));
        verify(manager).commit(any());
    }

    @Test
    void fiveFailuresRequireNewCodeAndNeverCreateUser() {
        var pending = usecase.register(input());
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
    void otpExpiresAtFiveMinutesAndSessionAtThirtyMinutes() {
        var pending = usecase.register(input());
        time.set(now.plusSeconds(300));
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .hasMessageContaining("hết hạn");
        time.set(now.plusSeconds(1800));
        assertThatThrownBy(() -> usecase.metadata(pending.token())).hasMessageContaining("Phiên");
    }

    @Test
    void resendEnforces120SecondsAndInvalidatesOldHash() {
        var pending = usecase.register(input());
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
    void failedDeliveryNeverMarksCodeReadyOrCreatesUser() {
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
    void globalEmailLimitIsNotBypassedByStartingAgain() {
        doThrow(new VerificationException("OTP_RATE_LIMIT", "wait", 429, 120))
                .when(repo)
                .reserveEmail(eq("teacher@example.com"), any());
        assertThatThrownBy(() -> usecase.register(input()))
                .isInstanceOf(VerificationException.class);
        verifyNoInteractions(mail, auth);
    }

    @Test
    void concurrentRegistrationConflictDoesNotIssueTokens() {
        var pending = usecase.register(input());
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .isInstanceOf(ConflictException.class);
        verify(manager).rollback(any());
        verifyNoInteractions(auth);
    }

    @Test
    void refreshPersistenceFailureRollsBackCompletion() {
        var pending = usecase.register(input());
        when(users.createNewUser(any(), any())).thenReturn(teacher());
        when(auth.generateToken(any())).thenReturn(new AuthUsecase.TokenResult(false, "at", "rt"));
        assertThatThrownBy(() -> usecase.verify(pending.token(), sent.get(0)))
                .isInstanceOf(InternalServerException.class);
        verify(manager).rollback(any());
        verify(manager, never()).commit(any());
    }

    @Test
    void googleNewAccountStillRequiresOtp() {
        var result = usecase.exchange(google("teacher@example.com", true), "verifier");
        assertThat(result.authorized()).isNull();
        assertThat(result.pending()).isNotNull();
        assertThat(storage.get(VerificationUsecase.hash(result.pending().token())).getPurpose())
                .isEqualTo("GOOGLE_REGISTER");
        verifyNoInteractions(auth);
        verify(users, never()).createNewUser(any(), any());
    }

    @Test
    void googleOtpCreatesTeacherWithoutLocalPassword() {
        var result = usecase.exchange(google("teacher@example.com", true), "verifier");
        tokens();
        when(users.createNewUser(any(), eq(""))).thenReturn(teacher());
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(teacher());
        usecase.verify(result.pending().token(), sent.get(0));
        verify(users)
                .createNewUser(argThat(input -> input.getLoginMethod().equals("GOOGLE")), eq(""));
        verify(users).linkGoogle("teacher-id", "google-sub");
    }

    @Test
    void externalEmailLinksOnlyAfterOtp() {
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        var result = usecase.exchange(google("teacher@example.com", false), "verifier");
        verify(users, never()).linkGoogle(any(), any());
        tokens();
        when(users.findById("teacher-id")).thenReturn(teacher());
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(teacher());
        usecase.verify(result.pending().token(), sent.get(0));
        verify(users).linkGoogle("teacher-id", "google-sub");
        verify(users, never()).createNewUser(any(), any());
    }

    @Test
    void authoritativeExistingEmailLinksDirectly() {
        var teacher = teacher();
        teacher.setEmail("teacher@gmail.com");
        when(users.findByEmail("teacher@gmail.com")).thenReturn(Optional.of(teacher));
        var mixed = teacher();
        mixed.setEmail("teacher@gmail.com");
        mixed.setLoginMethod("MIXED");
        when(users.linkGoogle("teacher-id", "google-sub")).thenReturn(mixed);
        tokens();
        var result = usecase.exchange(google("teacher@gmail.com", true), "verifier");
        assertThat(result.authorized()).isNotNull();
        verify(users).linkGoogle("teacher-id", "google-sub");
        verifyNoInteractions(mail);
    }

    @Test
    void linkedExternalEmailLogsInWithoutAnotherOtp() {
        var mixed = teacher();
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
    void changedGoogleEmailCannotFindOldAccountBySubject() {
        var result = usecase.exchange(google("changed@gmail.com", true), "verifier");
        assertThat(result.pending()).isNotNull();
        assertThat(result.authorized()).isNull();
        verify(users).findByEmail("changed@gmail.com");
        verify(users, never()).linkGoogle(any(), any());
        verifyNoInteractions(auth);
    }

    @Test
    void subjectConflictCannotReplaceExistingGoogleLink() {
        var user = teacher();
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
    void googleIdentityAlreadyBoundToAnotherEmailReturnsConflictWithoutJwt() {
        var user = teacher();
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
    void roleAndActiveStatusAreAuthoritative() {
        for (String role : List.of("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")) {
            var user = teacher();
            user.setRole(role);
            when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(user));
            String code = google("teacher@example.com", true);
            assertThatThrownBy(() -> usecase.exchange(code, "verifier"))
                    .isInstanceOf(ForbiddenException.class);
        }
        var locked = teacher();
        locked.setIsActive(false);
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(locked));
        assertThatThrownBy(() -> usecase.exchange(google("teacher@example.com", true), "verifier"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(auth, mail);
    }

    @Test
    void handoffRejectsDifferentBrowserReplayAndExpiry() {
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
    void unverifiedGoogleEmailIsRejectedBeforePersistence() {
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
}
