package com.wedu.exam_creation.payment.infrastructure.mapper;

import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.infrastructure.document.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefundMapper {
    RefundEntity toEntity(RefundDocument document);
    RefundDocument toDocument(RefundEntity entity);
    RefundAccountEntity toEntity(RefundAccountDocument document);
    RefundAccountDocument toDocument(RefundAccountEntity entity);
}
