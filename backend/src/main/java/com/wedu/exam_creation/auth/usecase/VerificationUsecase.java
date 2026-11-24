package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.domain.repository.IVerificationRepository;
import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.VerificationResponseDTO;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VerificationUsecase {
    private final IVerificationRepository repo;
    private final UserService users;
    private final SecurityService security;
    private final MailService mail;
    private final AuthUsecase auth;
    private final AuthDTOMapper mapper;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final byte[] otpSecret;
    private final SecureRandom random = new SecureRandom();

    public VerificationUsecase(
            IVerificationRepository repo,
            UserService users,
            SecurityService security,
            MailService mail,
            AuthUsecase auth,
            AuthDTOMapper mapper,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${auth.otp-secret}") String otpSecret) {
        if (otpSecret.length() < 32)
            throw new IllegalArgumentException("OTP_SECRET must contain at least 32 characters");
        this.repo = repo;
        this.users = users;
        this.security = security;
        this.mail = mail;
        this.auth = auth;
        this.mapper = mapper;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.otpSecret = otpSecret.getBytes(StandardCharsets.UTF_8);
    }

    public record Pending(String token, VerificationResponseDTO verification) {}

    public record Exchange(Pending pending, AuthorizedResponseDTO authorized) {}

    public Pending register(NewUserRequestDTO input) {
        if (!"LOCAL".equals(input.getLoginMethod()))
            throw new BadRequestException("Đăng ký bằng biểu mẫu chỉ hỗ trợ LOCAL");
        if (!input.getPlainPassword().equals(input.getConfirmPassword()))
            throw new BadRequestException("Mật khẩu xác nhận không trùng khớp");
        String email = normalizeEmail(input.getEmail());
        if (users.findByEmail(email).isPresent())
            throw new ConflictException("Tài khoản đã tồn tại");
        VerificationEntity entity = new VerificationEntity();
        entity.setPurpose("LOCAL_REGISTER");
        entity.setEmail(email);
        entity.setUsername(input.getUsername().trim());
        entity.setHashedPassword(security.hashPassword(input.getPlainPassword()));
        return begin(entity);
    }

    private Pending begin(VerificationEntity entity) {
        String token = newToken();
        entity.setId(hash(token));
        entity.setExpiresAt(clock.instant().plusSeconds(1800));
        repo.insert(entity);
        return new Pending(token, send(entity));
    }

    public VerificationResponseDTO metadata(String token) {
        return metadata(active(token));
    }

    private VerificationResponseDTO metadata(VerificationEntity entity) {
        String email = entity.getEmail();
        int at = email.indexOf('@');
        String masked = email.substring(0, 1) + "***" + email.substring(at);
        return new VerificationResponseDTO(
                masked,
                entity.getExpiresAt(),
                entity.getOtpExpiresAt(),
                entity.getResendAt(),
                Math.max(0, 5 - entity.getAttempts()),
                clock.instant());
    }

    public VerificationResponseDTO resend(String token) {
        VerificationEntity entity = active(token);
        Instant now = clock.instant();
        if (entity.getResendAt() != null && entity.getResendAt().isAfter(now)) {
            throw new VerificationException(
                    "OTP_COOLDOWN",
                    "Chưa đến thời gian gửi lại OTP",
                    429,
                    Math.max(1, entity.getResendAt().getEpochSecond() - now.getEpochSecond()));
        }
        return send(entity);
    }

    private VerificationResponseDTO send(VerificationEntity entity) {
        Instant now = clock.instant();
        repo.reserveEmail(entity.getEmail(), now);
        String otp;
        do {
            otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        } while (otpHash(entity.getId(), otp).equals(entity.getOtpHash()));
        entity.setOtpHash(otpHash(entity.getId(), otp));
        entity.setOtpExpiresAt(
                now.plusSeconds(300).isBefore(entity.getExpiresAt())
                        ? now.plusSeconds(300)
                        : entity.getExpiresAt());
        entity.setResendAt(now.plusSeconds(120));
        entity.setAttempts(0);
        if (!repo.replaceOtp(entity, now))
            throw new ConflictException("Phiên xác thực đã thay đổi, vui lòng thử lại");
        try {
            mail.sendVerificationOtp(entity.getEmail(), otp);
            repo.markReady(entity.getId(), entity.getOtpHash(), true);
        } catch (RuntimeException exception) {
            throw new VerificationException(
                    "OTP_DELIVERY_FAILED", "Không thể gửi OTP. Vui lòng thử lại sau", 503);
        }
        return metadata(entity);
    }

    private VerificationEntity active(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) throw expired();
        VerificationEntity entity = repo.find(hash(token));
        if (entity == null
                || entity.isConsumed()
                || "HANDOFF".equals(entity.getPurpose())
                || !entity.getExpiresAt().isAfter(clock.instant())) throw expired();
        return entity;
    }

    public AuthorizedResponseDTO verify(String token, String otp) {
        VerificationEntity entity = active(token);
        Instant now = clock.instant();
        if (entity.getAttempts() >= 5)
            throw new VerificationException(
                    "OTP_ATTEMPTS_EXHAUSTED", "Đã nhập sai 5 lần. Vui lòng gửi lại mã", 400);
        if (!entity.isReady()
                || entity.getOtpExpiresAt() == null
                || !entity.getOtpExpiresAt().isAfter(now))
            throw new VerificationException(
                    "OTP_EXPIRED", "OTP đã hết hạn hoặc chưa gửi thành công", 400);
        String candidate = otpHash(entity.getId(), otp);
        if (!MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                entity.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            repo.failAttempt(entity.getId(), entity.getOtpHash(), now);
            throw new VerificationException("OTP_INVALID", "OTP không chính xác", 400);
        }
        try {
            return transactions.execute(
                    status -> {
                        VerificationEntity consumed =
                                repo.consumeOtp(entity.getId(), candidate, clock.instant());
                        if (consumed == null)
                            throw new ConflictException("OTP đã thay đổi hoặc đã được sử dụng");
                        CommonUserResponseAllDTO user;
                        if ("GOOGLE_LINK".equals(consumed.getPurpose())) {
                            user = users.findById(consumed.getUserId());
                            requireTeacher(user);
                            if (!normalizeEmail(user.getEmail()).equals(consumed.getEmail()))
                                throw new ConflictException("Email tài khoản đã thay đổi");
                            user = users.linkGoogle(user.getId(), consumed.getGoogleSubject());
                        } else {
                            if (users.findByEmail(consumed.getEmail()).isPresent())
                                throw new ConflictException(
                                        "Email đã được đăng ký. Vui lòng đăng nhập");
                            boolean google = "GOOGLE_REGISTER".equals(consumed.getPurpose());
                            NewUserRequestDTO input =
                                    new NewUserRequestDTO(
                                            consumed.getEmail(),
                                            null,
                                            null,
                                            consumed.getUsername(),
                                            google ? "GOOGLE" : "LOCAL");
                            user =
                                    users.createNewUser(
                                            input, google ? "" : consumed.getHashedPassword());
                            if (google)
                                user = users.linkGoogle(user.getId(), consumed.getGoogleSubject());
                        }
                        return authorize(user);
                    });
        } catch (DuplicateKeyException exception) {
            throw new ConflictException(
                    "Email hoặc tài khoản Google đã được đăng ký. Vui lòng đăng nhập");
        }
    }

    public String googleHandoff(
            String email,
            String name,
            String subject,
            boolean verified,
            boolean authoritativeEmail,
            String bindingHash) {
        if (!verified
                || email == null
                || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                || subject == null
                || subject.isBlank()
                || bindingHash == null
                || !bindingHash.matches("[a-f0-9]{64}"))
            throw new BadRequestException(
                    "Danh tính Google không hợp lệ hoặc email chưa được xác minh");
        String token = newToken();
        VerificationEntity entity = new VerificationEntity();
        entity.setId(hash(token));
        entity.setPurpose("HANDOFF");
        entity.setEmail(normalizeEmail(email));
        entity.setUsername(
                name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name);
        entity.setGoogleSubject(subject);
        entity.setAuthoritativeEmail(authoritativeEmail);
        entity.setBindingHash(bindingHash);
        entity.setExpiresAt(clock.instant().plusSeconds(60));
        repo.insert(entity);
        return token;
    }

    public Exchange exchange(String code, String verifier) {
        VerificationEntity identity =
                repo.consumeHandoff(hash(code), hash(verifier), clock.instant());
        if (identity == null) throw expired();
        CommonUserResponseAllDTO existing = users.findByEmail(identity.getEmail()).orElse(null);
        if (existing != null) {
            requireTeacher(existing);
            if (identity.isAuthoritativeEmail()
                    || users.hasGoogleLink(identity.getEmail(), identity.getGoogleSubject())) {
                try {
                    return new Exchange(
                            null,
                            transactions.execute(
                                    status -> {
                                        CommonUserResponseAllDTO current =
                                                users.findByEmail(identity.getEmail())
                                                        .orElseThrow(
                                                                () ->
                                                                        new ConflictException(
                                                                                "Email tài khoản đã thay đổi"));
                                        requireTeacher(current);
                                        if (!current.getId().equals(existing.getId()))
                                            throw new ConflictException("Tài khoản đã thay đổi");
                                        return authorize(
                                                users.linkGoogle(
                                                        current.getId(),
                                                        identity.getGoogleSubject()));
                                    }));
                } catch (DuplicateKeyException exception) {
                    throw new ConflictException("Tài khoản Google đã liên kết với tài khoản khác");
                }
            }
        }
        VerificationEntity pending = new VerificationEntity();
        pending.setPurpose(existing == null ? "GOOGLE_REGISTER" : "GOOGLE_LINK");
        pending.setEmail(identity.getEmail());
        pending.setUsername(identity.getUsername());
        pending.setGoogleSubject(identity.getGoogleSubject());
        pending.setUserId(existing == null ? null : existing.getId());
        return new Exchange(begin(pending), null);
    }

    private AuthorizedResponseDTO authorize(CommonUserResponseAllDTO user) {
        requireTeacher(user);
        var dto = mapper.toUserResponseDTO(user);
        if (dto == null) throw new InternalServerException("Không thể tạo phiên đăng nhập");
        var tokens = auth.generateToken(dto);
        if (!tokens.saved()) throw new InternalServerException("Không thể lưu phiên đăng nhập");
        return new AuthorizedResponseDTO(dto, tokens.accessToken(), tokens.refreshToken());
    }

    private void requireTeacher(CommonUserResponseAllDTO user) {
        if (user == null
                || !Boolean.TRUE.equals(user.getIsActive())
                || !"ROLE_TEACHER".equals(user.getRole()))
            throw new ForbiddenException(
                    "Google và đăng ký mới chỉ dành cho tài khoản Giáo viên đang hoạt động");
    }

    private VerificationException expired() {
        return new VerificationException(
                "VERIFICATION_EXPIRED", "Phiên xác thực đã hết hạn. Vui lòng bắt đầu lại", 401);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String normalizeEmail(String email) {
        return com.wedu.exam_creation.common.util.EmailNormalizer.normalize(email);
    }

    public static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String otpHash(String id, String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(otpSecret, "HmacSHA256"));
            return HexFormat.of()
                    .formatHex(mac.doFinal((id + ":" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
