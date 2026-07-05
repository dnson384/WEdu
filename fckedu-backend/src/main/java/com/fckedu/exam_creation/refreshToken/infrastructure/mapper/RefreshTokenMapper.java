package com.fckedu.exam_creation.refreshToken.infrastructure.mapper;

import com.fckedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.fckedu.exam_creation.refreshToken.infrastructure.document.RefreshTokenDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {
    RefreshTokenEntity toEntity(RefreshTokenDocument document);

    RefreshTokenDocument toDocument(RefreshTokenEntity entity);
}
