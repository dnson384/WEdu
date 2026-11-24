package com.wedu.exam_creation.refreshToken.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.DeleteResult;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.wedu.exam_creation.refreshToken.infrastructure.document.RefreshTokenDocument;
import com.wedu.exam_creation.refreshToken.infrastructure.mapper.RefreshTokenMapper;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class RefreshTokenRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final RefreshTokenMapper mapper = mock(RefreshTokenMapper.class);
    private final RefreshTokenRepositoryImpl repository =
            new RefreshTokenRepositoryImpl(mongo, mapper);

    @ParameterizedTest
    @CsvSource({"stored-id,true", ",false"})
    @DisplayName("trả về kết quả theo ID document đã lưu khi ghi metadata token")
    void test_save_001(String savedId, boolean expected) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshTokenDocument input = new RefreshTokenDocument();
        RefreshTokenDocument persisted = new RefreshTokenDocument();
        persisted.setId(savedId);
        when(mapper.toDocument(entity)).thenReturn(input);
        when(mongo.save(same(input))).thenReturn(persisted);

        assertThat(repository.save(entity)).isEqualTo(expected);
        verify(mongo).save(same(input));
    }

    @Test
    @DisplayName("trả về lỗi truy cập dữ liệu gốc khi MongoDB không lưu được token")
    void test_save_002() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshTokenDocument document = new RefreshTokenDocument();
        when(mapper.toDocument(entity)).thenReturn(document);
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("test storage failure");
        when(mongo.save(document)).thenThrow(failure);

        assertThatThrownBy(() -> repository.save(entity)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về entity đã ánh xạ khi truy vấn khớp cả JTI lẫn user ID")
    void test_getRefreshTokenByJti_003() {
        RefreshTokenDocument document = new RefreshTokenDocument();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        when(mongo.findOne(any(Query.class), eq(RefreshTokenDocument.class))).thenReturn(document);
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repository.getRefreshTokenByJti("jti", "user")).isSameAs(entity);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(RefreshTokenDocument.class));
        assertThat(query.getValue().getQueryObject().getList("$and", Document.class))
                .containsExactlyInAnyOrder(
                        new Document("jti", "jti"), new Document("userId", "user"));
    }

    @Test
    @DisplayName("trả về null khi MongoDB không tìm thấy cặp JTI và user ID")
    void test_getRefreshTokenByJti_004() {
        when(mongo.findOne(any(Query.class), eq(RefreshTokenDocument.class))).thenReturn(null);
        when(mapper.toEntity(null)).thenReturn(null);

        assertThat(repository.getRefreshTokenByJti("missing", "user")).isNull();
    }

    @Test
    @DisplayName("trả về danh sách entity đã ánh xạ khi truy vấn token theo user ID")
    void test_getRefreshTokenByUserId_005() {
        RefreshTokenDocument document = new RefreshTokenDocument();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        when(mongo.find(any(Query.class), eq(RefreshTokenDocument.class)))
                .thenReturn(List.of(document));
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repository.getRefreshTokenByUserId("user")).containsExactly(entity);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(RefreshTokenDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("userId", "user"));
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi truy vấn user ID không có phiên nào")
    void test_getRefreshTokenByUserId_006() {
        when(mongo.find(any(Query.class), eq(RefreshTokenDocument.class))).thenReturn(List.of());

        assertThat(repository.getRefreshTokenByUserId("user")).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về true khi MongoDB xóa đúng một token theo JTI")
    void test_delete_007() {
        when(mongo.remove(any(Query.class), eq(RefreshTokenDocument.class)))
                .thenReturn(DeleteResult.acknowledged(1));

        assertThat(repository.delete("jti")).isTrue();

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(RefreshTokenDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("jti", "jti"));
    }

    @Test
    @DisplayName("trả về NotFoundException khi thao tác xóa JTI không khớp token nào")
    void test_delete_008() {
        when(mongo.remove(any(Query.class), eq(RefreshTokenDocument.class)))
                .thenReturn(DeleteResult.acknowledged(0));

        assertThatThrownBy(() -> repository.delete("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy RT");
    }

    @Test
    @DisplayName("trả về InternalServerException khi thao tác xóa một JTI báo xóa nhiều token")
    void test_delete_009() {
        when(mongo.remove(any(Query.class), eq(RefreshTokenDocument.class)))
                .thenReturn(DeleteResult.acknowledged(2));

        assertThatThrownBy(() -> repository.delete("duplicate"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Dữ liệu không nhất quán, xóa dư");
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,true", "2,true"})
    @DisplayName("trả về true nếu xóa được ít nhất một token khi lọc theo danh sách JTI")
    void test_deleteMany_010(long count, boolean expected) {
        List<String> jtis = List.of("jti-1", "jti-2");
        when(mongo.remove(any(Query.class), eq(RefreshTokenDocument.class)))
                .thenReturn(DeleteResult.acknowledged(count));

        assertThat(repository.deleteMany(jtis)).isEqualTo(expected);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(RefreshTokenDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(new Document("jti", new Document("$in", jtis)));
    }
}
