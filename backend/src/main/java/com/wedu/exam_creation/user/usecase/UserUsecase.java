package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.notification.service.TelegramNotificationService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserUsecase {
    private final UserRepositoryImpl repo;
    private final UserDTOMapper mapper;
    private final UserCommonDTOMapper mapperCommon;
    private final RefreshTokenService refreshTokenService;
    private final S3Service s3Service;
    private final TelegramNotificationService notiService;

    public UserUsecase(UserRepositoryImpl repo, UserDTOMapper mapper, UserCommonDTOMapper mapperCommon, RefreshTokenService refreshTokenService, S3Service s3Service, TelegramNotificationService notiService) {
        this.repo = repo;
        this.mapper = mapper;
        this.mapperCommon = mapperCommon;
        this.refreshTokenService = refreshTokenService;
        this.s3Service = s3Service;
        this.notiService = notiService;
    }

    // POST
    public CommonUserResponseAllDTO createNewUser(NewUserRequestDTO newUser, String hashedPassword) {
        UserEntity newUserEntity = new UserEntity(
                null,
                newUser.getEmail(),
                hashedPassword,
                newUser.getUsername(),
                "ROLE_TEACHER",
                newUser.getLoginMethod(),
                "avatars/default-avatar-user.png",
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserEntity createdUser = repo.save(newUserEntity);

        return mapper.toCommonAllDTO(createdUser);
    }


    // GET
    public CommonUserResponseDTO getMe(CommonUserResponseAllDTO user) {
        CommonUserResponseDTO userResponse = mapperCommon.commonAllToCommonDTO(user);

        String avatarUrl = s3Service.generatePresignedUrl(userResponse.getAvatarUrl());
        userResponse.setAvatarUrl(avatarUrl);

        return userResponse;
    }

    public Optional<CommonUserResponseAllDTO> findByEmail(String email) {
        return repo.findByEmail(email).map(mapper::toCommonAllDTO);
    }

    public CommonUserResponseAllDTO findById(String userId) {
        return mapper.toCommonAllDTO(repo.findById(userId));
    }

    public List<CommonUserResponseDTO> getAllUsers() {
        List<UserEntity> users = repo.all();

        if (users.isEmpty()) {
            throw new NotFoundException("Hệ thống chưa có người dùng nào");
        }

        return users.stream().map(mapper::toCommonDTO).toList();
    }

    public List<CommonUserResponseDTO> findUserByKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKeyword = keyword.trim();

            List<UserEntity> users = repo.findByKeyword(cleanKeyword);

            if (users.isEmpty()) {
                throw new NotFoundException("Không tồn tại người dùng");
            }

            return users.stream().map(mapper::toCommonDTO).toList();
        }
        throw new BadRequestException("Từ khóa không được rỗng");
    }

    // UPDATE
    public boolean updateAvatar(String userId, String s3Key) {
        if (s3Key.isEmpty()) {
            throw new BadRequestException("s3Key rỗng");
        }

        UserEntity user = requireActiveUser(userId);
        String oldAvatar = user.getAvatarUrl();

        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setS3Key(s3Key);
        UserEntity updatedUser = this.updateField(userId, updateFields);

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
                String message = String.format("Không xóa được ảnh cũ %s: %s", oldAvatar, e.getMessage());
                System.err.printf(message);
                notiService.notifyFailedDeleteImage(message);
            }
        }
        return true;
    }

    public CommonUserResponseAllDTO updateRole(CommonUserResponseAllDTO user) {
        UserEntity userEntity = mapper.commonAllToEntity(user);

        UserEntity updatedUser = repo.save(userEntity);

        if (updatedUser == null) {
            throw new InternalServerException("Có lỗi trong quá trình cập nhật vai trò người dùng");
        }

        return mapper.toCommonAllDTO(updatedUser);
    }

    public CommonUserResponseDTO updateUsername(String userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Tên người dùng rỗng");
        }

        if (username.trim().length() > 254) {
            throw new BadRequestException("Tên người dùng quá dài");
        }

        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setUsername(username);
        UserEntity updatedUser = this.updateField(userId, updateFields);

        if (updatedUser == null) {
            throw new InternalServerException("Có lỗi trong quá trình cập nhật vai trò người dùng");
        }
        return mapper.toCommonDTO(updatedUser);
    }

    public CommonUserResponseAllDTO lockUnlockUser(String userId, boolean isLock) {
        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setIsActive(!isLock);

        UserEntity updatedUser = this.updateField(userId, updateFields);
        if (updatedUser == null) {
            throw new InternalServerException("Có lỗi trong quá trình cập nhật vai trò người dùng");
        }
        return mapper.toCommonAllDTO(updatedUser);
    }

    public CommonUserResponseAllDTO updatePassword(CommonUserResponseAllDTO user) {
        UserEntity userEntity = mapper.commonAllToEntity(user);

        UserEntity updatedUser = repo.save(userEntity);
        if (updatedUser == null) {
            throw new InternalServerException("Có lỗi trong quá trình cập nhật vai trò người dùng");
        }

        return mapper.toCommonAllDTO(updatedUser);
    }

    // DELETE
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

    private UserEntity updateField(String userId, UserUpdateFields updateFields) {
        if (updateFields.getS3Key() == null &&
                updateFields.getUsername() == null &&
                updateFields.getIsActive() == null
        ) {
            throw new BadRequestException("Không có trường nào cần cập nhật");
        }

        if (updateFields.getIsActive() == null) {
            requireActiveUser(userId);
        }

        return repo.updateField(userId, updateFields);
    }

    private UserEntity requireActiveUser(String userId) {
        UserEntity user = repo.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }
        if (!user.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        return user;
    }
}