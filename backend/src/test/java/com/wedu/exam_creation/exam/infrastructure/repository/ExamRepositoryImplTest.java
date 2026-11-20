package com.wedu.exam_creation.exam.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.DeleteResult;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.exam.domain.entity.ExamEntity;
import com.wedu.exam_creation.exam.infrastructure.document.ExamDocument;
import com.wedu.exam_creation.exam.infrastructure.mapper.ExamMapper;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class ExamRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ExamMapper mapper = mock(ExamMapper.class);
    private final ExamRepositoryImpl repository = new ExamRepositoryImpl(mongo, mapper);
    private final IllegalStateException failure = new IllegalStateException("Mongo unavailable");

    private void assertOwnerFilter(Query query) {
        assertThat(query.getQueryObject())
                .isEqualTo(
                        new Document(
                                "$and",
                                List.of(
                                        new Document("_id", "exam"),
                                        new Document("userId", "owner"))));
    }

    @Test
    @DisplayName("trả về ID document đã insert khi lưu đề thành công")
    void test_saveExam_001() {
        ExamEntity entity = new ExamEntity();
        ExamDocument document = new ExamDocument();
        ExamDocument saved = new ExamDocument();
        saved.setId("inserted");
        when(mapper.entityToDocument(entity)).thenReturn(document);
        when(mongo.insert(document)).thenReturn(saved);
        assertThat(repository.saveExam(entity)).isEqualTo("inserted");
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi insert đề thất bại")
    void test_saveExam_002() {
        ExamEntity entity = new ExamEntity();
        ExamDocument document = new ExamDocument();
        when(mapper.entityToDocument(entity)).thenReturn(document);
        when(mongo.insert(document)).thenThrow(failure);
        assertThatThrownBy(() -> repository.saveExam(entity)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về lỗi mapping và không insert khi chuyển document thất bại")
    void test_saveExam_003() {
        ExamEntity entity = new ExamEntity();
        when(mapper.entityToDocument(entity)).thenThrow(failure);
        assertThatThrownBy(() -> repository.saveExam(entity)).isSameAs(failure);
        verifyNoInteractions(mongo);
    }

    @Test
    @DisplayName("trả về entity khi query kết hợp ID đề và owner tìm thấy document")
    void test_getExamById_001() {
        ExamDocument document = new ExamDocument();
        ExamEntity entity = new ExamEntity();
        when(mongo.findOne(any(Query.class), eq(ExamDocument.class))).thenReturn(document);
        when(mapper.docToEntity(document)).thenReturn(entity);
        assertThat(repository.getExamById("owner", "exam")).isSameAs(entity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(ExamDocument.class));
        assertOwnerFilter(query.getValue());
    }

    @Test
    @DisplayName("trả về NotFoundException khi query theo owner không tìm thấy đề")
    void test_getExamById_002() {
        assertThatThrownBy(() -> repository.getExamById("owner", "exam"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tồn tại bài kiểm tra");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(ExamDocument.class));
        assertOwnerFilter(query.getValue());
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi đọc đề theo owner thất bại")
    void test_getExamById_003() {
        when(mongo.findOne(any(Query.class), eq(ExamDocument.class))).thenThrow(failure);
        assertThatThrownBy(() -> repository.getExamById("owner", "exam")).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về entity đúng thứ tự khi truy vấn tất cả đề của owner")
    void test_getAllUserExams_001() {
        ExamDocument first = new ExamDocument();
        first.setId("e1");
        ExamDocument second = new ExamDocument();
        second.setId("e2");
        ExamEntity firstEntity = new ExamEntity();
        firstEntity.setId("e1");
        ExamEntity secondEntity = new ExamEntity();
        secondEntity.setId("e2");
        when(mongo.find(any(Query.class), eq(ExamDocument.class)))
                .thenReturn(List.of(first, second));
        when(mapper.docToEntity(first)).thenReturn(firstEntity);
        when(mapper.docToEntity(second)).thenReturn(secondEntity);
        assertThat(repository.getAllUserExams("owner")).containsExactly(firstEntity, secondEntity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(ExamDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("userId", "owner"));
    }

    @Test
    @DisplayName("trả về NotFoundException khi owner chưa có đề")
    void test_getAllUserExams_002() {
        when(mongo.find(any(Query.class), eq(ExamDocument.class))).thenReturn(List.of());
        assertThatThrownBy(() -> repository.getAllUserExams("owner"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Danh sách bài kiểm tra trống");
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi đọc danh sách đề thất bại")
    void test_getAllUserExams_003() {
        when(mongo.find(any(Query.class), eq(ExamDocument.class))).thenThrow(failure);
        assertThatThrownBy(() -> repository.getAllUserExams("owner")).isSameAs(failure);
    }

    @Test
    @DisplayName(
            "trả về entity theo thứ tự Mongo khi query recent lọc owner sắp xếp giảm dần và giới hạn 5")
    void test_getRecentExams_001() {
        ExamDocument first = new ExamDocument();
        first.setId("new");
        ExamDocument second = new ExamDocument();
        second.setId("old");
        ExamEntity firstEntity = new ExamEntity();
        firstEntity.setId("new");
        ExamEntity secondEntity = new ExamEntity();
        secondEntity.setId("old");
        when(mongo.find(any(Query.class), eq(ExamDocument.class)))
                .thenReturn(List.of(first, second));
        when(mapper.docToEntity(first)).thenReturn(firstEntity);
        when(mapper.docToEntity(second)).thenReturn(secondEntity);
        assertThat(repository.getRecentExams("owner")).containsExactly(firstEntity, secondEntity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(ExamDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("userId", "owner"));
        assertThat(query.getValue().getSortObject()).isEqualTo(new Document("updatedAt", -1));
        assertThat(query.getValue().getLimit()).isEqualTo(5);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi owner chưa có đề recent")
    void test_getRecentExams_002() {
        when(mongo.find(any(Query.class), eq(ExamDocument.class))).thenReturn(List.of());
        assertThat(repository.getRecentExams("owner")).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi query recent thất bại")
    void test_getRecentExams_003() {
        when(mongo.find(any(Query.class), eq(ExamDocument.class))).thenThrow(failure);
        assertThatThrownBy(() -> repository.getRecentExams("owner")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về true khi xóa đúng một đề với điều kiện ID và owner")
    void test_deleteExam_001() {
        when(mongo.remove(any(Query.class), eq(ExamDocument.class)))
                .thenReturn(DeleteResult.acknowledged(1));
        assertThat(repository.deleteExam("owner", "exam")).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(ExamDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(
                        new Document(
                                "$and",
                                List.of(
                                        new Document("userId", "owner"),
                                        new Document("_id", "exam"))));
    }

    @Test
    @DisplayName("trả về false khi không đề nào khớp ID và owner cần xóa")
    void test_deleteExam_002() {
        when(mongo.remove(any(Query.class), eq(ExamDocument.class)))
                .thenReturn(DeleteResult.acknowledged(0));
        assertThat(repository.deleteExam("owner", "exam")).isFalse();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(ExamDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(
                        new Document(
                                "$and",
                                List.of(
                                        new Document("userId", "owner"),
                                        new Document("_id", "exam"))));
    }

    @Test
    @DisplayName("trả về InternalServerException khi Mongo báo xóa nhiều hơn một đề")
    void test_deleteExam_003() {
        when(mongo.remove(any(Query.class), eq(ExamDocument.class)))
                .thenReturn(DeleteResult.acknowledged(2));
        assertThatThrownBy(() -> repository.deleteExam("owner", "exam"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Xóa quá giới hạn. Yêu cầu Rollback!");
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi xóa đề thất bại")
    void test_deleteExam_004() {
        when(mongo.remove(any(Query.class), eq(ExamDocument.class))).thenThrow(failure);
        assertThatThrownBy(() -> repository.deleteExam("owner", "exam")).isSameAs(failure);
    }
}
