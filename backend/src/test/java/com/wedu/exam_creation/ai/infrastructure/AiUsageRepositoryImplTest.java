package com.wedu.exam_creation.ai.infrastructure;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.infrastructure.document.AiUsageDocument;
import com.wedu.exam_creation.ai.infrastructure.document.AiBudgetDocument;
import com.wedu.exam_creation.ai.infrastructure.mapper.AiUsageMapper;
import com.wedu.exam_creation.ai.infrastructure.repository.AiUsageRepositoryImpl;
import com.wedu.exam_creation.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiUsageRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final AiUsageRepositoryImpl repository = new AiUsageRepositoryImpl(mongo, Mappers.getMapper(AiUsageMapper.class));

    @Test
    void noConfigurationMeansNoLimitsWithoutWritingDefaults() {
        var budget = repository.getBudget();
        assertThat(budget.getRevision()).isZero();
        assertThat(budget.getMonthlyBudgetUsd()).isNull();
        assertThat(budget.getMonthlyTokenQuota()).isNull();
        verify(mongo, never()).insert(any(AiBudgetDocument.class));
    }

    @Test
    void concurrentInitialCreationReturnsConflictInsteadOfOverwriting() {
        when(mongo.insert(any(AiBudgetDocument.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThatThrownBy(() -> repository.updateBudget(new AiBudgetEntity())).isInstanceOf(ConflictException.class);
    }

    @Test
    void updatesOnlyExpectedRevisionAndAllowsClearingOneLimit() {
        var budget = new AiBudgetEntity();
        budget.setRevision(5);
        budget.setMonthlyBudgetUsd(new BigDecimal("12.3456789"));
        var result = new AiBudgetDocument();
        result.setId("global");
        result.setRevision(6);
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(AiBudgetDocument.class))).thenReturn(result);
        assertThat(repository.updateBudget(budget).getRevision()).isEqualTo(6);
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class), eq(AiBudgetDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", "global").containsEntry("revision", 5L);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class)).containsEntry("monthlyTokenQuota", null);
    }

    @Test
    void staleRevisionNeverUpsertsOrOverwrites() {
        var budget = new AiBudgetEntity();
        budget.setRevision(2);
        assertThatThrownBy(() -> repository.updateBudget(budget)).isInstanceOf(ConflictException.class);
        verify(mongo, never()).insert(any(AiBudgetDocument.class));
    }

    @Test
    void aggregationFiltersBeforeGroupingAndUsesVietnamDays() {
        when(mongo.indexOps(AiUsageDocument.class)).thenReturn(mock(IndexOperations.class));
        when(mongo.aggregate(any(Aggregation.class), eq("AiUsage"), eq(AiDailyUsageEntity.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));
        Instant from = Instant.parse("2026-10-31T17:00:00Z");
        Instant until = Instant.parse("2026-11-01T17:00:00Z");
        repository.findDaily(from, until, "deepseek-flash");
        var capture = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo).aggregate(capture.capture(), eq("AiUsage"), eq(AiDailyUsageEntity.class));
        var pipeline = capture.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        Document match = pipeline.get(0).get("$match", Document.class);
        assertThat(match).containsEntry("model", "deepseek-flash");
        assertThat(match.get("createdAt", Document.class)).containsEntry("$gte", Date.from(from)).containsEntry("$lt", Date.from(until));
        Document date = pipeline.get(1).get("$group", Document.class).get("_id", Document.class).get("$dateToString", Document.class);
        assertThat(date).containsEntry("timezone", "Asia/Ho_Chi_Minh");
        assertThat(pipeline.get(3).get("$sort", Document.class)).containsEntry("day", 1);
    }

    @Test
    void usageWriteSuspendsQuestionTransactionAndIsNotPartOfItsRollback() {
        var manager = new TestTransactions();
        var indexes = mock(IndexOperations.class);
        when(mongo.indexOps(AiUsageDocument.class)).thenReturn(indexes);
        when(mongo.save(any(AiUsageDocument.class))).thenAnswer(call -> {
            assertThat(manager.active).as("usage must be written outside the question transaction").isFalse();
            return call.getArgument(0);
        });
        var proxy = new ProxyFactory(repository);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        var independent = (IAiUsageRepository) proxy.getProxy();
        new TransactionTemplate(manager).executeWithoutResult(transaction -> {
            assertThat(manager.active).isTrue();
            independent.saveUsage(new AiUsageEntity());
            assertThat(manager.active).as("question transaction must resume").isTrue();
            transaction.setRollbackOnly();
        });
        assertThat(manager.rolledBack).isTrue();
        verify(mongo).save(any(AiUsageDocument.class));
    }

    private static class TestTransactions extends AbstractPlatformTransactionManager {
        private boolean active;
        private boolean rolledBack;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected boolean isExistingTransaction(Object transaction) { return active; }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { active = true; }
        @Override protected Object doSuspend(Object transaction) { active = false; return true; }
        @Override protected void doResume(Object transaction, Object suspended) { active = true; }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { rolledBack = true; }
        @Override protected void doCleanupAfterCompletion(Object transaction) { active = false; }
    }
}
