package com.wedu.exam_creation.admin.usecase;

import com.wedu.exam_creation.admin.dto.request.LockOrUnlockRequestDTO;
import com.wedu.exam_creation.admin.dto.request.SetRoleRequestDTO;
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

    public List<CommonUserResponseDTO> getAllUsers(CommonUserResponseAllDTO user) {
        return userService.getAllUsers();
    }

    public List<CommonUserResponseDTO> findUsers(CommonUserResponseAllDTO user, String keyword) {
        return userService.findUserByKeyword(keyword);
    }

    public CommonUserResponseDTO updateUserRole(CommonUserResponseAllDTO user, SetRoleRequestDTO req) {
        if (req.getUserId().equals(user.getId())) {
            throw new BadRequestException("Không được phép thay đổi quyền của bản thân");
        }

        if (!isValidateRole(req.getRole())) {
            throw new BadRequestException("Quyền không hợp lệ");
        }

        CommonUserResponseAllDTO curUser = userService.findById(req.getUserId());
        if (curUser == null) {
            throw new NotFoundException("Không tìm thấy người dùng để phân quyền");
        }
        if (curUser.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không được phép thay đổi quyền của admin khác");
        }

        curUser.setRole(req.getRole());

        CommonUserResponseAllDTO updatedUser = userService.updateRole(curUser);
        return mapper.commonAllToCommonDTO(updatedUser);
    }

    public CommonUserResponseDTO lockOrUnlockUser(CommonUserResponseAllDTO user, LockOrUnlockRequestDTO req) {
        if (req.getUserId().equals(user.getId())) {
            throw new BadRequestException("Không được phép khóa/mở khóa bản thân");
        }

        CommonUserResponseAllDTO curUser = userService.findById(req.getUserId());
        if (curUser == null) {
            throw new NotFoundException("Không tìm thấy người dùng để khóa/mở khóa");
        }
        if (curUser.getRole().equals("ROLE_ADMIN")) {
            throw new ForbiddenException("Không được phép khoá/mở khóa của admin khác");
        }

        if (req.isLock() == !curUser.getIsActive()) {
            return mapper.commonAllToCommonDTO(curUser);
        }

        CommonUserResponseAllDTO updatedUser = userService.lockUnlockUser(curUser.getId(), req.isLock());

        return mapper.commonAllToCommonDTO(updatedUser);
    }

    private boolean isValidateRole(String role) {
        if (role == null || role.isBlank()) return false;
        return List.of("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_TEACHER").contains(role);
    }
}
