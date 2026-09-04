package com.wedu.exam_creation.user.domain.repository;

import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.user.domain.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface IUserRepository {
    Optional<UserEntity> findByEmail(String email);

    UserEntity save(UserEntity user);

    UserEntity updateField(String userId, UserUpdateFields updateFields);

    UserEntity findById(String userId);

    boolean delete(String userId);

    List<UserEntity> all();

    List<UserEntity> findByKeyword(String keyword);
}
