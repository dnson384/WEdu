package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepositoryImpl repo;
    private final UserDTOMapper mapper;
    private final RefreshTokenService refreshTokenService;
    private final SecurityService securityService;
    private final S3Service s3Service;

    public UserService(UserRepositoryImpl repo, UserDTOMapper mapper, RefreshTokenService refreshTokenService, SecurityService securityService, S3Service s3Service) {
        this.repo = repo;
        this.mapper = mapper;
        this.refreshTokenService = refreshTokenService;

        this.securityService = securityService;
        this.s3Service = s3Service;
    }

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

    public CommonUserResponseAllDTO updateUser(CommonUserResponseAllDTO user) {
        UserEntity userEntity = mapper.commonAllToEntity(user);

        UserEntity savedUser = repo.save(userEntity);

        return mapper.toCommonAllDTO(savedUser);
    }

    public Optional<CommonUserResponseAllDTO> findByEmail(String email) {
        return repo.findByEmail(email).map(mapper::toCommonAllDTO);
    }

    public CommonUserResponseAllDTO findById(String userId) {
        return mapper.toCommonAllDTO(repo.findById(userId));
    }

    public CommonUserResponseDTO getMe(String accessToken, String refreshToken) {
        String userId = securityService.getPayloadFromAccessToken(accessToken).getUserId();
        RTPayload rtPayload = securityService.getPayloadFromRefreshToken(refreshToken);

        if (!rtPayload.getUserId().equals(userId)) {
            throw new UnAuthorizedException("userId không trùng khớp");
        }

        if (!refreshTokenService.exists(rtPayload.getJti(), userId)) {
            throw new NotFoundException("RT không tồn tại");
        }

        UserEntity user = repo.findById(userId);

        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        if (!user.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khoá");
        }

        CommonUserResponseDTO userResponse = mapper.toCommonDTO(user);

        String avatarUrl = s3Service.generatePresignedUrl(userResponse.getAvatarUrl());
        userResponse.setAvatarUrl(avatarUrl);

        return userResponse;
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
}
