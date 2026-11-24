package com.wedu.exam_creation.auth.domain.repository;

import com.wedu.exam_creation.auth.domain.entity.AuthMigrationChangeEntity;
import com.wedu.exam_creation.auth.domain.entity.AuthMigrationUserEntity;
import java.util.List;

public interface IAuthMigrationRepository {
    List<AuthMigrationUserEntity> findUsers();

    void dropLegacyIndexes();

    void apply(List<AuthMigrationChangeEntity> changes);
}
