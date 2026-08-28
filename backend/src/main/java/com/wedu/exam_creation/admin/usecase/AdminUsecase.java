package com.wedu.exam_creation.admin.usecase;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.user.usecase.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUsecase {
    private final UserService userService;

    public AdminUsecase(UserService userService) {
        this.userService = userService;
    }

    public List<CommonUserResponseDTO> getAllUsers(CommonUserResponseAllDTO user) {
        if (!user.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không có quyền thực hiện");
        }

        return userService.getAllUsers();
    }

    public List<CommonUserResponseDTO> findUsers(CommonUserResponseAllDTO user, String keyword) {
        if (!user.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không có quyền thực hiện");
        }
        
        return userService.findUserByKeyword(keyword);
    }
}
