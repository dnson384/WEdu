package com.wedu.exam_creation.user.dto.mapper;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.wedu.exam_creation.user.domain.entity.PlusEntitlement;
import java.time.Instant;

@Mapper(componentModel = "spring", imports = {PlusEntitlement.class, Instant.class})
public interface UserDTOMapper {
    @Mapping(target = "accountType", expression = "java(PlusEntitlement.effectiveType(entity.getAccountType(), entity.getPlusExpiresAt(), Instant.now()))")
    CommonUserResponseAllDTO toCommonAllDTO(UserEntity entity);

    @Mapping(target = "accountType", expression = "java(PlusEntitlement.effectiveType(entity.getAccountType(), entity.getPlusExpiresAt(), Instant.now()))")
    CommonUserResponseDTO toCommonDTO(UserEntity entity);

    UserEntity commonAllToEntity(CommonUserResponseAllDTO dto);
}
