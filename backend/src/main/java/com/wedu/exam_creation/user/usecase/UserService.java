package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserUsecase userUsecase;

    public UserService(UserUsecase userUsecase) {
        this.userUsecase = userUsecase;
    }

    // POST
    public CommonUserResponseAllDTO createNewUser(NewUserRequestDTO newUser, String hashedPassword) {
        return userUsecase.createNewUser(newUser, hashedPassword);
    }

    // UPDATE
    public CommonUserResponseAllDTO updateRole(CommonUserResponseAllDTO user) {
        return userUsecase.updateRole(user);
    }

    public CommonUserResponseAllDTO updatePassword(CommonUserResponseAllDTO user) {
        return userUsecase.updatePassword(user);
    }

    public CommonUserResponseAllDTO lockUnlockUser(String userId, boolean isLock) {
        return userUsecase.lockUnlockUser(userId, isLock);
    }

    public Optional<CommonUserResponseAllDTO> findByEmail(String email) {
        return userUsecase.findByEmail(email);
    }

    public CommonUserResponseAllDTO findById(String userId) {
        return userUsecase.findById(userId);
    }

    public List<CommonUserResponseDTO> getAllUsers() {
        return userUsecase.getAllUsers();
    }

    public List<CommonUserResponseDTO> findUserByKeyword(String keyword) {
        return userUsecase.findUserByKeyword(keyword);
    }
}
