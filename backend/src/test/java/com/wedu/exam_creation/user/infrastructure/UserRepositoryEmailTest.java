package com.wedu.exam_creation.user.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

class UserRepositoryEmailTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final UserRepositoryImpl repo =
            new UserRepositoryImpl(mongo, Mappers.getMapper(UserMapper.class));

    @Test
    void existenceLookupUsesOnlyCanonicalEmail() {
        repo.findByEmail(" Teacher@Example.COM ");
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(UserDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsOnlyKeys("email")
                .containsEntry("email", "teacher@example.com");
    }

    @Test
    void saveCanonicalizesEmailWithoutDerivedField() {
        when(mongo.save(any(UserDocument.class))).thenAnswer(call -> call.getArgument(0));
        var user = new UserEntity();
        user.setEmail(" Teacher@Example.COM ");
        assertThat(repo.save(user).getEmail()).isEqualTo("teacher@example.com");
        var saved = ArgumentCaptor.forClass(UserDocument.class);
        verify(mongo).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("teacher@example.com");
    }

    @Test
    void linkingUpdatesMethodAndSubjectWithoutOverwritingPasswordOrEntitlement() {
        when(mongo.findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(UserDocument.class)))
                .thenReturn(new UserDocument());
        repo.linkGoogle("teacher-id", "sub", "MIXED");
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .findAndModify(
                        query.capture(),
                        update.capture(),
                        any(FindAndModifyOptions.class),
                        eq(UserDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("role", "ROLE_TEACHER")
                .containsEntry("isActive", true);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("googleSubject", "sub")
                .containsEntry("loginMethod", "MIXED")
                .doesNotContainKeys("hashedPassword", "accountType", "plusExpiresAt", "email");
    }

    @Test
    void anotherGoogleSubjectOrInactiveAccountCannotBeOverwritten() {
        assertThatThrownBy(() -> repo.linkGoogle("teacher-id", "sub", "MIXED"))
                .isInstanceOf(ConflictException.class);
    }
}
