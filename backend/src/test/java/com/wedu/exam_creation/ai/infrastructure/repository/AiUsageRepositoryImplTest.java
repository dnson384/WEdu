package com.wedu.exam_creation.ai.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.infrastructure.document.AiBudgetDocument;
import com.wedu.exam_creation.ai.infrastructure.document.AiUsageDocument;
import com.wedu.exam_creation.ai.infrastructure.mapper.AiUsageMapper;
import com.wedu.exam_creation.common.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AiUsageRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final AiUsageMapper mapper = mock(AiUsageMapper.class);
    private final AiUsageRepositoryImpl repository = new AiUsageRepositoryImpl(mongo, mapper);

    @Test
    @DisplayName("trả về ngân sách revision 0 và hạn mức trống khi chưa có cấu hình")
    void test_getBudget_001() {
        AiBudgetEntity budget = repository.getBudget();
        assertThat(budget.getRevision()).isZero();
        assertThat(budget.getMonthlyBudgetUsd()).isNull();
        assertThat(budget.getMonthlyTokenQuota()).isNull();
        verify(mongo, never()).insert(any(AiBudgetDocument.class));
    }

    @Test
    @DisplayName("trả về ConflictException khi cấu hình đã được tạo bởi yêu cầu khác")
    void test_updateBudget_002() {
        AiBudgetEntity budget = new AiBudgetEntity();
        when(mapper.toDocument(budget)).thenReturn(new AiBudgetDocument());
        when(mongo.insert(any(AiBudgetDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        assertThatThrownBy(() -> repository.updateBudget(budget))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("trả về revision mới và quota trống khi cập nhật đúng ID và revision")
    void test_updateBudget_003() {
        AiBudgetEntity budget = new AiBudgetEntity();
        budget.setId("global");
        budget.setRevision(5);
        budget.setMonthlyBudgetUsd(new BigDecimal("12.3456789"));
        AiBudgetDocument result = new AiBudgetDocument();
        result.setId("global");
        result.setRevision(6);
        AiBudgetEntity saved = new AiBudgetEntity();
        saved.setRevision(6);
        when(mapper.toEntity(result)).thenReturn(saved);
        when(mongo.findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(AiBudgetDocument.class)))
                .thenReturn(result);
        assertThat(repository.updateBudget(budget)).isSameAs(saved);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .findAndModify(
                        query.capture(),
                        update.capture(),
                        any(FindAndModifyOptions.class),
                        eq(AiBudgetDocument.class));
        assertThat(query.getValue().getQueryObject().getList("$and", Document.class))
                .containsExactlyInAnyOrder(
                        new Document("_id", "global"), new Document("revision", 5L));
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsEntry("monthlyBudgetUsd", budget.getMonthlyBudgetUsd())
                .containsEntry("monthlyTokenQuota", null);
        assertThat(update.getValue().getUpdateObject().get("$inc", Document.class))
                .containsEntry("revision", 1);
    }

    @Test
    @DisplayName("trả về ConflictException khi revision cũ và không có bản ghi được cập nhật")
    void test_updateBudget_004() {
        AiBudgetEntity budget = new AiBudgetEntity();
        budget.setRevision(2);
        assertThatThrownBy(() -> repository.updateBudget(budget))
                .isInstanceOf(ConflictException.class);
        verify(mongo, never()).insert(any(AiBudgetDocument.class));
    }

    @Test
    @DisplayName("trả về usage theo ngày Việt Nam khi lọc model và khoảng thời gian trước khi nhóm")
    void test_findDaily_005() {
        when(mongo.indexOps(AiUsageDocument.class)).thenReturn(mock(IndexOperations.class));
        when(mongo.aggregate(any(Aggregation.class), eq("AiUsage"), eq(AiDailyUsageEntity.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));
        Instant from = Instant.parse("2026-10-31T17:00:00Z");
        Instant until = Instant.parse("2026-11-01T17:00:00Z");
        repository.findDaily(from, until, "deepseek-flash");
        ArgumentCaptor<Aggregation> capture = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo).aggregate(capture.capture(), eq("AiUsage"), eq(AiDailyUsageEntity.class));
        List<Document> pipeline = capture.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        Document match = pipeline.get(0).get("$match", Document.class);
        assertThat(match).containsEntry("model", "deepseek-flash");
        assertThat(match.get("createdAt", Document.class))
                .containsEntry("$gte", Date.from(from))
                .containsEntry("$lt", Date.from(until));
        Document date =
                pipeline.get(1)
                        .get("$group", Document.class)
                        .get("_id", Document.class)
                        .get("$dateToString", Document.class);
        assertThat(date).containsEntry("timezone", "Asia/Ho_Chi_Minh");
        assertThat(pipeline.get(3).get("$sort", Document.class)).containsEntry("day", 1);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi ghi usage ngoài transaction câu hỏi bị rollback")
    void test_saveUsage_006() {
        TestTransactions manager = new TestTransactions();
        IndexOperations indexes = mock(IndexOperations.class);
        AiUsageEntity usage = new AiUsageEntity();
        AiUsageDocument document = new AiUsageDocument();
        when(mapper.toDocument(usage)).thenReturn(document);
        when(mongo.indexOps(AiUsageDocument.class)).thenReturn(indexes);
        when(mongo.save(any(AiUsageDocument.class)))
                .thenAnswer(
                        call -> {
                            assertThat(manager.active)
                                    .as("usage must be written outside the question transaction")
                                    .isFalse();
                            return call.getArgument(0);
                        });
        ProxyFactory proxy = new ProxyFactory(repository);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(
                new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        IAiUsageRepository independent = (IAiUsageRepository) proxy.getProxy();
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        transaction -> {
                            assertThat(manager.active).isTrue();
                            independent.saveUsage(usage);
                            assertThat(manager.active)
                                    .as("question transaction must resume")
                                    .isTrue();
                            transaction.setRollbackOnly();
                        });
        assertThat(manager.rolledBack).isTrue();
        verify(mongo).save(document);
    }

    @Test
    @DisplayName("trả về ngân sách đã lưu khi tạo cấu hình global với revision 1")
    void test_updateBudget_007() {
        AiBudgetEntity budget = new AiBudgetEntity();
        AiBudgetDocument document = new AiBudgetDocument();
        AiBudgetEntity saved = new AiBudgetEntity();
        when(mapper.toDocument(budget)).thenReturn(document);
        when(mongo.insert(document)).thenReturn(document);
        when(mapper.toEntity(document)).thenReturn(saved);

        assertThat(repository.updateBudget(budget)).isSameAs(saved);

        verify(mongo).insert(document);
        assertThat(document.getId()).isEqualTo("global");
        assertThat(document.getRevision()).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về danh sách model từng được sử dụng khi đọc các model đã ghi nhận")
    void test_findModels_008() {
        when(mongo.findDistinct(
                        any(Query.class), eq("model"), eq(AiUsageDocument.class), eq(String.class)))
                .thenReturn(List.of("deepseek-flash", "historic-model"));

        assertThat(repository.findModels())
                .containsExactlyInAnyOrder("deepseek-flash", "historic-model");
    }

    private static class TestTransactions extends AbstractPlatformTransactionManager {
        private boolean active;
        private boolean rolledBack;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return active;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active = true;
        }

        @Override
        protected Object doSuspend(Object transaction) {
            active = false;
            return true;
        }

        @Override
        protected void doResume(Object transaction, Object suspended) {
            active = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rolledBack = true;
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            active = false;
        }
    }
}
