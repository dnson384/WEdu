package com.wedu.exam_creation.passwordResetToken.usecase;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;
import com.wedu.exam_creation.passwordResetToken.domain.repository.IPasswordResetTokenRepository;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetTokenUsecase {
    private static final long EXPIRY_MINUTES = 15;

    private final UserService userService;
    private final IPasswordResetTokenRepository repo;
    private final SecurityService securityService;
    private final MailService mailService;
    private final String frontendUrl;

    public PasswordResetTokenUsecase(
            UserService userService,
            IPasswordResetTokenRepository repo,
            SecurityService securityService,
            MailService mailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.userService = userService;
        this.repo = repo;
        this.securityService = securityService;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
    }

    public void requestReset(String email) {
        Optional<CommonUserResponseAllDTO> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new NotFoundException("Tài khoản không tồn tại");
        }

        CommonUserResponseAllDTO user = userOpt.get();

        if (user.getId() == null || user.getId().isBlank()) {
            throw new BadRequestException("Id nguời dùng rỗng");
        }

        repo.deleteByUserId(user.getId());

        String plainToken = this.generatePlainToken();
        String tokenHash = this.hashToken(plainToken);

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                null,
                tokenHash,
                user.getId(),
                Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES),
                false,
                Instant.now()
        );

        repo.save(entity);

        String resetLink = frontendUrl + "/auth/reset-password?token=" + plainToken;
        mailService.sendResetPasswordEmail(user.getEmail(), resetLink);
    }

    public void resetPassword(String plainToken, String newPassword, String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            throw new BadRequestException("Mật khẩu xác nhận không trùng khớp");
        }

        String tokenHash = this.hashToken(plainToken);

        PasswordResetTokenEntity resetToken = repo.findByTokenHashAndUsedFalse(tokenHash);

        if (resetToken == null) {
            throw new BadRequestException("Token không hợp lệ hoặc đã được sử dụng");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Token đã hết hạn");
        }

        CommonUserResponseAllDTO user = userService.findById(resetToken.getUserId());
        if (user == null) {
            throw new NotFoundException("Tài khoản không tồn tại");
        }

        String hashedPassword = securityService.hashPassword(newPassword);
        user.setHashedPassword(hashedPassword);

        userService.updatePassword(user);

        resetToken.setUsed(true);
        repo.save(resetToken);
    }

    private String generatePlainToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException("Hashing algorithm not found");
        }
    }
}
