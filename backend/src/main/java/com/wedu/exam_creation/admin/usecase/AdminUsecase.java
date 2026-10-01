package com.wedu.exam_creation.admin.usecase;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.user.usecase.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUsecase {
    private final UserService userService;
    private final UserCommonDTOMapper mapper;

    public AdminUsecase(UserService userService, UserCommonDTOMapper mapper) {
        this.userService = userService;
        this.mapper = mapper;
    }

    public List<CommonUserResponseDTO> getAllUsers(CommonUserResponseAllDTO admin) {
        return userService.getAllUsers();
    }

    public List<CommonUserResponseDTO> findUsers(CommonUserResponseAllDTO admin, String keyword) {
        return userService.findUserByKeyword(keyword);
    }

    public CommonUserResponseDTO updateUserRole(CommonUserResponseAllDTO admin, String userId, String role) {
        if (userId.equals(admin.getId())) {
            throw new BadRequestException("Không được phép thay đổi quyền của bản thân");
        }

        if (!isValidateRole(role)) {
            throw new BadRequestException("Quyền không hợp lệ");
        }

        CommonUserResponseAllDTO curUser = userService.findById(userId);
        if (curUser == null) {
            throw new NotFoundException("Không tìm thấy người dùng để phân quyền");
        }
        if (curUser.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không được phép thay đổi quyền của admin khác");
        }

        curUser.setRole(role);

        CommonUserResponseAllDTO updatedUser = userService.updateRole(curUser);
        return mapper.commonAllToCommonDTO(updatedUser);
    }

    public CommonUserResponseDTO lockUser(CommonUserResponseAllDTO admin, String userId) {
        if (userId.equals(admin.getId())) {
            throw new BadRequestException("Không được phép khóa tài khoản bản thân");
        }

        CommonUserResponseAllDTO curUser = userService.findById(userId);
        if (curUser == null) {
            throw new NotFoundException("Không tìm thấy người dùng để khóa");
        }
        if (curUser.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không được phép khóa của admin khác");
        }

        if (!curUser.getIsActive()) {
            return mapper.commonAllToCommonDTO(curUser);
        }

        CommonUserResponseAllDTO updatedUser = userService.lockUser(curUser.getId());

        return mapper.commonAllToCommonDTO(updatedUser);
    }

    public CommonUserResponseDTO unlockUser(CommonUserResponseAllDTO admin, String userId) {
        if (userId.equals(admin.getId())) {
            throw new BadRequestException("Không được phép mở khóa tài khoản bản thân");
        }

        CommonUserResponseAllDTO curUser = userService.findById(userId);
        if (curUser == null) {
            throw new NotFoundException("Không tìm thấy người dùng để mở khóa");
        }
        if (curUser.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không được phép mở khóa của admin khác");
        }

        if (curUser.getIsActive()) {
            return mapper.commonAllToCommonDTO(curUser);
        }

        CommonUserResponseAllDTO updatedUser = userService.unlockUser(curUser.getId());

        return mapper.commonAllToCommonDTO(updatedUser);
    }

    public boolean deleteUser(CommonUserResponseAllDTO admin, String userId) {
        if (userId.equals(admin.getId())) {
            throw new BadRequestException("Không được phép xóa tài khoản bản thân");
        }

        CommonUserResponseAllDTO curUser = userService.findById(userId);
        if (curUser == null) {
            throw new NotFoundException("Không tìm thấy tài khoản để xóa");
        }
        if (curUser.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không được phép xóa tài khoản của admin khác");
        }

        return userService.deleteUser(userId);
    }

    private boolean isValidateRole(String role) {
        if (role == null || role.isBlank()) return false;
        return List.of("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_TEACHER").contains(role);
    }
}
