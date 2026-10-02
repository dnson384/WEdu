package com.wedu.exam_creation.passwordResetToken.usecase;

import org.springframework.stereotype.Service;

@Service
public class PasswordResetTokenService {
    private final PasswordResetTokenUsecase usecase;

    public PasswordResetTokenService(PasswordResetTokenUsecase usecase) {
        this.usecase = usecase;
    }

    public void requestReset(String email) {
        usecase.requestReset(email);
    }

    public void resetPassword(String plainToken, String newPassword, String confirmNewPassword) {
        usecase.resetPassword(plainToken, newPassword, confirmNewPassword);
    }
}
