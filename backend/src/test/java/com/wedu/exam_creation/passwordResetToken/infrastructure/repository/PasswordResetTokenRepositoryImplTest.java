package com.wedu.exam_creation.passwordResetToken.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;
import com.wedu.exam_creation.passwordResetToken.infrastructure.document.PasswordResetTokenDocument;
import com.wedu.exam_creation.passwordResetToken.infrastructure.mapper.PasswordResetTokenMapper;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class PasswordResetTokenRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final PasswordResetTokenMapper mapper = mock(PasswordResetTokenMapper.class);
    private final PasswordResetTokenRepositoryImpl repository =
            new PasswordResetTokenRepositoryImpl(mongo, mapper);

    @Test
    @DisplayName("trả về không có ngoại lệ khi xóa token bằng bộ lọc đúng userId")
    void test_deleteByUserId_001() {
        repository.deleteByUserId("user-1");

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(PasswordResetTokenDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("userId", "user-1"));
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi lưu document đã ánh xạ từ entity")
    void test_save_002() {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        PasswordResetTokenDocument document = new PasswordResetTokenDocument();
        when(mapper.toDocument(entity)).thenReturn(document);

        repository.save(entity);

        verify(mongo).save(same(document));
    }

    @Test
    @DisplayName("trả về entity đã ánh xạ khi truy vấn đúng token hash và used bằng false")
    void test_findByTokenHashAndUsedFalse_003() {
        PasswordResetTokenDocument document = new PasswordResetTokenDocument();
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        when(mongo.findOne(any(Query.class), eq(PasswordResetTokenDocument.class)))
                .thenReturn(document);
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repository.findByTokenHashAndUsedFalse("test-hash")).isSameAs(entity);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(PasswordResetTokenDocument.class));
        assertThat(query.getValue().getQueryObject().getList("$and", Document.class))
                .containsExactlyInAnyOrder(
                        new Document("tokenHash", "test-hash"), new Document("used", false));
    }

    @Test
    @DisplayName("trả về null khi MongoDB không tìm thấy token chưa sử dụng")
    void test_findByTokenHashAndUsedFalse_004() {
        when(mongo.findOne(any(Query.class), eq(PasswordResetTokenDocument.class)))
                .thenReturn(null);
        when(mapper.toEntity(null)).thenReturn(null);

        assertThat(repository.findByTokenHashAndUsedFalse("missing-hash")).isNull();
    }

    @Test
    @DisplayName("trả về lỗi truy cập dữ liệu gốc khi MongoDB không lưu được token")
    void test_save_005() {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        PasswordResetTokenDocument document = new PasswordResetTokenDocument();
        when(mapper.toDocument(entity)).thenReturn(document);
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("test storage failure");
        when(mongo.save(document)).thenThrow(failure);

        assertThatThrownBy(() -> repository.save(entity)).isSameAs(failure);
    }
}
