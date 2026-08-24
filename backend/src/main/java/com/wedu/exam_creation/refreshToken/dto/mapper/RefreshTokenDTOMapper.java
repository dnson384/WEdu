package com.wedu.exam_creation.refreshToken.dto.mapper;

import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenDTOMapper {
    RTResponseDTO toResponseDTO(RefreshTokenEntity entity);
}
