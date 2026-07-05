package com.fckedu.exam_creation.refreshToken.dto.mapper;

import com.fckedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.fckedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenDTOMapper {
    RTResponseDTO toResponseDTO(RefreshTokenEntity entity);
}
