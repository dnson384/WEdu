package com.wedu.exam_creation.user.dto.mapper;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.response.UserResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDTOMapper {
    UserResponseDTO toUserResponseDTO(UserEntity entity);

    CommonUserResponseAllDTO toCommonAllDTO(UserEntity entity);

    CommonUserResponseDTO toCommonDTO(UserEntity entity);
}
