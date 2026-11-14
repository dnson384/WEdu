package com.wedu.exam_creation.auth.infrastructure.mapper;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.infrastructure.document.VerificationDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VerificationMapper {
    VerificationEntity toEntity(VerificationDocument document);

    VerificationDocument toDocument(VerificationEntity entity);
}
