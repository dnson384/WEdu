package com.fckedu.exam_creation.security.infrastructure.service;

import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.domain.repository.IUserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final IUserRepository userRepository;

    public CustomOAuth2UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Lấy thông tin từ Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // Kiểm tra dưới DB MongoDB thông qua Domain Port
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // Nếu chưa tồn tại -> Tạo tài khoản mới loại GOOGLE
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setUsername(name);
            newUser.setAvatarUrl(picture);
            newUser.setLoginMethod("GOOGLE");
            newUser.setRole("ROLE_USER");
            newUser.setIsActive(true);
            newUser.setHashedPassword(""); // Tài khoản Google không có mật khẩu local

            userRepository.save(newUser);
        } else {
            // Nếu đã tồn tại nhưng loginMethod là LOCAL -> Bạn có thể cập nhật hoặc báo lỗi tùy business
            UserEntity existingUser = userOptional.get();
            if ("GOOGLE".equals(existingUser.getLoginMethod())) {
                // Cập nhật lại avatar mới nếu cần
                existingUser.setAvatarUrl(picture);
                userRepository.save(existingUser);
            }
        }

        return oAuth2User;
    }
}
