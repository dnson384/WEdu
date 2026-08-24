package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserUsecase {
    private final UserRepositoryImpl repo;
    private final RefreshTokenService refreshTokenService;
    private final S3Service s3Service;

    public UserUsecase(UserRepositoryImpl repo, RefreshTokenService refreshTokenService, S3Service s3Service) {
        this.repo = repo;
        this.refreshTokenService = refreshTokenService;
        this.s3Service = s3Service;
    }

    public boolean updateAvatar(String userId, String s3Key) {
        if (s3Key.isEmpty()) {
            throw new BadRequestException("s3Key rỗng");
        }

        UserEntity user = repo.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        if (!user.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }

        String oldAvatar = user.getAvatarUrl();

        user.setAvatarUrl(s3Key);
        UserEntity updatedUser = repo.save(user);

        if (updatedUser == null) {
            try {
                s3Service.deleteFile(s3Key);
            } catch (Exception e) {
                System.err.printf("Không xóa được ảnh %s: %s", s3Key, e.getMessage());
            }

            throw new InternalServerException("Có lỗi trong quá trình cập nhật avatar");
        } else if (!oldAvatar.equals("avatars/default-avatar-user.png")) {
            try {
                s3Service.deleteFile(oldAvatar);
            } catch (Exception e) {
                System.err.printf("Không xóa được ảnh cũ %s: %s", oldAvatar, e.getMessage());
            }
        }
        return true;
    }
    
    public boolean updateUser(String userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Tên người dùng rỗng");
        }

        if (username.trim().length() > 254) {
            throw new BadRequestException("Tên người dùng quá dài");
        }

        UserEntity user = repo.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        if (!user.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }

        user.setUsername(username);
        UserEntity updatedUser = repo.save(user);

        if (updatedUser == null) {
            throw new InternalServerException("Có lỗi trong quá trình cập nhật tài khoản");
        }
        return true;
    }

    public boolean deleteAccount(String userId) {
        boolean isDeleted = repo.delete(userId);

        if (isDeleted) {
            List<RTResponseDTO> rtResponseDTOS = refreshTokenService.getUserRefreshToken(userId);

            List<String> tokenJtis = rtResponseDTOS.stream().map(RTResponseDTO::getJti).toList();

            if (tokenJtis.isEmpty()) {
                return true;
            }

            return refreshTokenService.deleteMany(tokenJtis);
        }
        return false;
    }
}