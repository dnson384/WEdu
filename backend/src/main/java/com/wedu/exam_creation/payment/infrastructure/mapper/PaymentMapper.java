package com.wedu.exam_creation.payment.infrastructure.mapper;
import org.mapstruct.Mapper;
import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.infrastructure.document.*;
@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentOrderEntity toEntity(PaymentOrderDocument document);
    PaymentOrderDocument toDocument(PaymentOrderEntity entity);
    PaymentEmailEntity toEntity(PaymentEmailDocument document);
    PaymentEmailDocument toDocument(PaymentEmailEntity entity);
}
