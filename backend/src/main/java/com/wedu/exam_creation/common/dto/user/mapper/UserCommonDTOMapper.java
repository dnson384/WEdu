package com.wedu.exam_creation.common.dto.user.mapper;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCommonDTOMapper {
    CommonUserResponseDTO commonAllToCommonDTO(CommonUserResponseAllDTO allDTO);
}
