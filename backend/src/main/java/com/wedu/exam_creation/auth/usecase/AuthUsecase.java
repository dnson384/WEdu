package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.dto.request.ChangePasswordPayloadRequestDTO;
import com.wedu.exam_creation.user.dto.request.ChangePasswordRequestDTO;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthUsecase {
    private final UserService userService;
    private final SecurityService securityService;
    private final RefreshTokenService refreshTokenService;
    private final AuthDTOMapper mapper;

    public AuthUsecase(UserService userService, SecurityService securityService, RefreshTokenService refreshTokenService, AuthDTOMapper mapper) {
        this.userService = userService;
        this.securityService = securityService;
        this.refreshTokenService = refreshTokenService;
        this.mapper = mapper;
    }

    public AuthorizedResponseDTO register(NewUserRequestDTO newUser) {
        if (!newUser.getPlainPassword().equals(newUser.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không trùng khớp");
        }

        Optional<CommonUserResponseAllDTO> userEntityOptional = userService.findByEmail(newUser.getEmail());
        if (userEntityOptional.isPresent()) {
            throw new BadRequestException("Tài khoản đã tồn tại");
        }

        String hashedPassword = securityService.hashPassword(newUser.getPlainPassword());

        CommonUserResponseAllDTO createdUser = userService.createNewUser(newUser, hashedPassword);

        UserResponseDTO user = mapper.toUserResponseDTO(createdUser);

        if (user == null) {
            throw new InternalServerException("Lỗi trong quá trình chuyển đổi entity -> dto");
        }

        String jti = UUID.randomUUID().toString();

        // AT
        ATPayload accessTokenPayload = new ATPayload(
                jti, user.getId(), user.getEmail(), user.getRole()
        );
        String accessToken = securityService.generateAccessToken(accessTokenPayload);

        // RT
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
        Optional<CommonUserResponseAllDTO> commonUserResponseAllDTO = userService.findByEmail(payload.getEmail());
        if (commonUserResponseAllDTO.isEmpty()) {
            throw new NotFoundException("Tài khoản chưa tồn tại");
        }

        CommonUserResponseAllDTO user = commonUserResponseAllDTO.get();

        if (user.getLoginMethod().equals("GOOGLE")) {
            throw new UnAuthorizedException("Sai phương thức đăng nhập");
        }

        if (!user.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa");
        }

        // Validate password
        if (!securityService.validatePassword(payload.getPlainPassword(), user.getHashedPassword())) {
            throw new UnAuthorizedException("Mật khẩu không chính xác");
        }

        UserResponseDTO userDto = mapper.toUserResponseDTO(user);

        String jti = UUID.randomUUID().toString();

        // AT
        ATPayload accessTokenPayload = new ATPayload(
                jti, user.getId(), user.getEmail(), user.getRole()
        );
        String accessToken = securityService.generateAccessToken(accessTokenPayload);

        // RT
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

    public boolean logout(String authorization) {
        String accessToken = authorization.substring(7).trim();
        try {
            ATPayload atPayload = securityService.getPayloadFromAccessToken(accessToken);
            return refreshTokenService.delete(atPayload.getParentJti());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnAuthorizedException("RT không hợp lệ");
        }
    }

    @Transactional
    public boolean changePassword(String userId, ChangePasswordRequestDTO reqPayload) {
        ChangePasswordPayloadRequestDTO payload = reqPayload.getPayload();

        RTPayload rtPayload = securityService.getPayloadFromRefreshToken(reqPayload.getRefreshToken());

        if (!payload.getNewPassword().equals(payload.getConfirmNewPassword())) {
            throw new UnAuthorizedException("Mật khẩu xác nhận của mật khẩu mới không trùng khớp");
        }

        CommonUserResponseAllDTO user = userService.findById(userId);
        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        if (!securityService.validatePassword(payload.getOldPassword(), user.getHashedPassword())) {
            throw new UnAuthorizedException("Mật khẩu cũ không chính xác");
        }

        String newHashedPassword = securityService.hashPassword(payload.getNewPassword());

        user.setHashedPassword(newHashedPassword);
        return saveUserAndRevokeRT(user, rtPayload.getJti());
    }

    @Transactional
    public boolean lockAccount(String userId, String refreshToken) {
        CommonUserResponseAllDTO user = userService.findById(userId);

        RTPayload rtPayload = securityService.getPayloadFromRefreshToken(refreshToken);

        if (user == null) {
            throw new NotFoundException("Không tìm thấy tài khoản");
        }

        user.setIsActive(false);

        return saveUserAndRevokeRT(user, rtPayload.getJti());
    }

    public String regenerateAccessToken(String refreshToken) {
        securityService.validateRefreshToken(refreshToken);

        try {
            RTPayload rtPayload = securityService.getPayloadFromRefreshToken(refreshToken);

            RefreshTokenEntity token = refreshTokenService.getRefreshToken(rtPayload.getJti(), rtPayload.getUserId());

            CommonUserResponseAllDTO user = userService.findById(token.getUserId());

            if (user == null) {
                throw new NotFoundException("Người dùng không tồn tại");
            }

            ATPayload atPayload = new ATPayload(
                    rtPayload.getJti(), user.getId(), user.getEmail(), user.getRole()
            );

            return securityService.generateAccessToken(atPayload);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnAuthorizedException("RT không hợp lệ");
        }
    }

    @Transactional
    protected boolean saveUserAndRevokeRT(CommonUserResponseAllDTO user, String jti) {
        try {
            CommonUserResponseAllDTO savedUser = userService.updateUser(user);

            if (savedUser != null) {
                List<RTResponseDTO> rtResponseDTOS = refreshTokenService.getUserRefreshToken(savedUser.getId());

                List<String> tokenJtis = rtResponseDTOS.stream().map(RTResponseDTO::getJti).toList();

                List<String> jtisToDelete = tokenJtis.stream()
                        .filter(token -> !token.equals(jti))
                        .toList();

                if (jtisToDelete.isEmpty()) {
                    return true;
                }

                return refreshTokenService.deleteMany(jtisToDelete);
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return false;
    }
}
