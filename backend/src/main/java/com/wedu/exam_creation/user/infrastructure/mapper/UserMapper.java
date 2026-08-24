package com.wedu.exam_creation.user.infrastructure.mapper;


import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserEntity toEntity(UserDocument document);

    UserDocument toDocument(UserEntity entity);
}
