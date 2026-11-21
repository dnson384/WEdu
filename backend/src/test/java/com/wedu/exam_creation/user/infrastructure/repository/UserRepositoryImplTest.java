package com.wedu.exam_creation.user.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;

class UserRepositoryImplTest {
    @Test
    @DisplayName("trả về không có ngoại lệ và giữ quyền PLUS khi đổi mật khẩu hoặc vai trò")
    void test_updatePassword_001() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        UserRepositoryImpl users = new UserRepositoryImpl(mongo, mock(UserMapper.class));
        users.updatePassword("teacher", "hash");
        users.updateRole("teacher", "ROLE_MODERATOR");
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2))
                .findAndModify(
                        any(Query.class),
                        update.capture(),
                        any(FindAndModifyOptions.class),
                        eq(UserDocument.class));
        assertThat(
                        update.getAllValues()
                                .get(0)
                                .getUpdateObject()
                                .get("$set", org.bson.Document.class))
                .containsKeys("hashedPassword", "updatedAt")
                .doesNotContainKeys("accountType", "plusExpiresAt", "role");
        assertThat(
                        update.getAllValues()
                                .get(1)
                                .getUpdateObject()
                                .get("$set", org.bson.Document.class))
                .containsKeys("role", "updatedAt")
                .doesNotContainKeys("accountType", "plusExpiresAt", "hashedPassword");
        verify(mongo, never()).save(any());
    }
}
