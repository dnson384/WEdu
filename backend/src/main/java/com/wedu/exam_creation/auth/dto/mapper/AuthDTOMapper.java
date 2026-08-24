package com.wedu.exam_creation.auth.dto.mapper;

import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthDTOMapper {
    UserResponseDTO toUserResponseDTO(CommonUserResponseAllDTO allDTO);
}
