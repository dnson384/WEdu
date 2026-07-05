package com.fckedu.exam_creation.user.usecase;

import com.fckedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.fckedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.fckedu.exam_creation.security.service.SecurityService;
import com.fckedu.exam_creation.storage.service.S3Service;
import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.fckedu.exam_creation.user.dto.request.*;
import com.fckedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.fckedu.exam_creation.user.dto.response.UserResponseDTO;
import com.fckedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserUsecase {
    private final UserRepositoryImpl repo;
    private final UserDTOMapper mapperDTO;
    private final SecurityService securityService;
    private final RefreshTokenService refreshTokenService;
    private final S3Service s3Service;

    public UserUsecase(UserRepositoryImpl repo, UserDTOMapper mapperDTO, SecurityService securityService, RefreshTokenService refreshTokenService, S3Service s3Service) {
        this.repo = repo;
        this.mapperDTO = mapperDTO;
        this.securityService = securityService;
        this.refreshTokenService = refreshTokenService;
        this.s3Service = s3Service;
    }

    public AuthorizedResponseDTO register(NewUserRequestDTO newUser) {
        String hashedPassword = securityService.hashPassword(newUser.getPlainPassword());
        UserEntity newUserEntity = new UserEntity(
                null,
                newUser.getEmail(),
                hashedPassword,
                newUser.getUsername(),
                "ROLE_TEACHER",
                newUser.getLoginMethod(),
                "avatars/default-avatar-user.png",
                true,
                "Free",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserEntity createdUser = repo.save(newUserEntity);

        UserResponseDTO user = mapperDTO.toUserResponseDTO(createdUser);

        // AT
        ATPayload accessTokenPayload = new ATPayload(
                user.getId(), user.getEmail(), user.getRole()
        );
        String accessToken = securityService.generateAccessToken(accessTokenPayload);

        // RT
        String jti = UUID.randomUUID().toString();
        RTPayload refreshTokenPayload = new RTPayload(
                jti, user.getId(), user.getEmail(), user.getRole()
        );
        String refreshToken = securityService.generateRefreshToken(refreshTokenPayload);


        // Luu RT
        NewRTRequestDTO newRTRequestDTO = securityService.parseNewRefreshToken(refreshToken);
        boolean saveNewRT = refreshTokenService.save(newRTRequestDTO);

        if (!saveNewRT) {
            throw new InternalServerException("Lỗi trong quá trình lưu RT!");
        }

        return new AuthorizedResponseDTO(
                user, accessToken, refreshToken
        );
    }

    public AuthorizedResponseDTO login(LoginUserRequestDTO payload) {
        UserEntity user = repo.findByEmail(payload.getEmail())
                .orElseThrow(() -> new NotFoundException("Tài khoản chưa tồn tại"));

        if (user.getLoginMethod().equals("GOOGLE")) {
            throw new UnAuthorizedException("Sai phương thức đăng nhập");
        }

        if (!user.getIsActive()) {
            throw new UnAuthorizedException("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa");
        }

        // Validate password
        if (!securityService.validatePassword(payload.getPlainPassword(), user.getHashedPassword())) {
            throw new UnAuthorizedException("Mật khẩu không chính xác");
        }

        UserResponseDTO userDto = mapperDTO.toUserResponseDTO(user);

        // AT
        ATPayload accessTokenPayload = new ATPayload(
                user.getId(), user.getEmail(), user.getRole()
        );
        String accessToken = securityService.generateAccessToken(accessTokenPayload);

        // RT
        String jti = UUID.randomUUID().toString();
        RTPayload refreshTokenPayload = new RTPayload(
                jti, user.getId(), user.getEmail(), user.getRole()
        );
        String refreshToken = securityService.generateRefreshToken(refreshTokenPayload);

        // Luu RT
        NewRTRequestDTO newRTRequestDTO = securityService.parseNewRefreshToken(refreshToken);
        boolean saveNewRT = refreshTokenService.save(newRTRequestDTO);

        if (!saveNewRT) {
            throw new InternalServerException("Lỗi trong quá trình lưu RT!");
        }

        return new AuthorizedResponseDTO(
                userDto, accessToken, refreshToken
        );
    }

    public boolean logout(String refreshToken) {
        boolean isValidated = securityService.validateRefreshToken(refreshToken);

        if (isValidated) {
            RTPayload payload = securityService.getPayloadFromRefreshToken(refreshToken);
            return refreshTokenService.delete(payload.getJti());
        }

        return false;
    }

    public boolean updateAvatar(String userId, String s3Key) {
        UserEntity user = repo.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        String oldAvatar = user.getAvatarUrl();
        String newAvatar = "avatars/" + s3Key;

        user.setAvatarUrl(newAvatar);
        UserEntity updatedUser = repo.save(user);

        if (updatedUser != null && !oldAvatar.equals("avatars/default-avatar-user.png")) {
            s3Service.deleteFile(oldAvatar);
        }
        return true;
    }

    public boolean updateUser(String userId, UpdateUserRequestDTO payload) {
        UserEntity user = repo.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        user.setUsername(payload.getUsername());

        return repo.save(user) != null;
    }

    public boolean changePassword(String userId, ChangePasswordRequestDTO reqPayload) {
        ChangePasswordPayloadRequestDTO payload = reqPayload.getPayload();

        RTPayload rtPayload = securityService.getPayloadFromRefreshToken(reqPayload.getRefreshToken());

        if (!payload.getNewPassword().equals(payload.getConfirmNewPassword())) {
            throw new UnAuthorizedException("Mật khẩu xác nhận của mật khẩu mới không trùng khớp");
        }

        UserEntity user = repo.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        if (!securityService.validatePassword(payload.getOldPassword(), user.getHashedPassword())) {
            throw new UnAuthorizedException("Mật khẩu cũ không chính xác");
        }

        String newHashedPassword = securityService.hashPassword(payload.getNewPassword());

        user.setHashedPassword(newHashedPassword);
        UserEntity savedUser = repo.save(user);

        if (savedUser != null) {
            List<RTResponseDTO> rtResponseDTOS = refreshTokenService.getUserRefreshToken(savedUser.getId());

            List<String> tokenJtis = rtResponseDTOS.stream().map(RTResponseDTO::getJti).toList();

            List<String> jtisToDelete = tokenJtis.stream()
                    .filter(token -> !token.equals(rtPayload.getJti()))
                    .toList();

            if (tokenJtis.isEmpty()) {
                return true;
            }

            return refreshTokenService.deleteMany(jtisToDelete);
        }
        return false;
    }

    public boolean lockAccount(String userId, String refreshToken) {
        UserEntity user = repo.findById(userId);

        RTPayload rtPayload = securityService.getPayloadFromRefreshToken(refreshToken);

        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        user.setIsActive(false);

        UserEntity savedUser = repo.save(user);

        if (savedUser != null) {
            List<RTResponseDTO> rtResponseDTOS = refreshTokenService.getUserRefreshToken(savedUser.getId());

            List<String> tokenJtis = rtResponseDTOS.stream().map(RTResponseDTO::getJti).toList();

            List<String> jtisToDelete = tokenJtis.stream()
                    .filter(token -> !token.equals(rtPayload.getJti()))
                    .toList();

            if (jtisToDelete.isEmpty()) {
                return true;
            }

            return refreshTokenService.deleteMany(jtisToDelete);
        }
        return repo.save(user) != null;
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