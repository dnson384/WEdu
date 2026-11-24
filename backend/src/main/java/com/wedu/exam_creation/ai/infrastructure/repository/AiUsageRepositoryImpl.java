package com.wedu.exam_creation.ai.infrastructure.repository;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.infrastructure.document.AiBudgetDocument;
import com.wedu.exam_creation.ai.infrastructure.document.AiUsageDocument;
import com.wedu.exam_creation.ai.infrastructure.mapper.AiUsageMapper;
import com.wedu.exam_creation.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AiUsageRepositoryImpl implements IAiUsageRepository {
    private final MongoTemplate mongoTemplate;
    private final AiUsageMapper mapper;
    private volatile boolean indexesReady;

    private synchronized void ensureIndexes() {
        if (indexesReady) return;

        mongoTemplate.indexOps(AiUsageDocument.class)
                .createIndex(new Index().on("createdAt", Sort.Direction.ASC));
        mongoTemplate.indexOps(AiUsageDocument.class)
                .createIndex(new Index().on("model", Sort.Direction.ASC).on("createdAt", Sort.Direction.ASC));
        indexesReady = true;
    }

    @Override
    public void saveUsage(AiUsageEntity usage) {
        ensureIndexes();
        mongoTemplate.save(mapper.toDocument(usage));
    }

    @Override
    public List<AiDailyUsageEntity> findDaily(Instant from, Instant until, String model) {
        ensureIndexes();

        Document match = new Document("createdAt", new Document("$gte", Date.from(from)).append("$lt", Date.from(until)));
        if (model != null) {
            match.append("model", model);
        }

        Document group = new Document("_id", new Document("$dateToString", new Document("format", "%Y-%m-%d")
                .append("date", "$createdAt").append("timezone", "Asia/Ho_Chi_Minh")))
                .append("requests", new Document("$sum", 1))
                .append("unknownUsageRequests", new Document("$sum", new Document("$cond", List.of("$usageKnown", 0, 1))))
                .append("inputTokens", new Document("$sum", "$inputTokens"))
                .append("outputTokens", new Document("$sum", "$outputTokens"))
                .append("totalTokens", new Document("$sum", "$totalTokens"))
                .append("estimatedCostUsd", new Document("$sum", "$estimatedCostUsd"));

        Aggregation aggregation = Aggregation.newAggregation(
                context -> new Document("$match", match),
                context -> new Document("$group", group),
                context -> new Document("$project", new Document("_id", 0).append("day", "$_id")
                        .append("requests", 1).append("unknownUsageRequests", 1).append("inputTokens", 1)
                        .append("outputTokens", 1).append("totalTokens", 1).append("estimatedCostUsd", 1)),
                Aggregation.sort(Sort.Direction.ASC, "day"));

        return mongoTemplate.aggregate(aggregation, "AiUsage", AiDailyUsageEntity.class).getMappedResults();
    }

    @Override
    public List<String> findModels() {
        return mongoTemplate.findDistinct(new Query(), "model", AiUsageDocument.class, String.class);
    }

    @Override
    public AiBudgetEntity getBudget() {
        AiBudgetDocument document = mongoTemplate.findById("global", AiBudgetDocument.class);
        return document == null ? new AiBudgetEntity() : mapper.toEntity(document);
    }

    @Override
    public AiBudgetEntity updateBudget(AiBudgetEntity budget) {
        if (budget.getRevision() == 0) {
            AiBudgetDocument document = mapper.toDocument(budget);
            document.setId("global");
            document.setRevision(1);

            try {
                return mapper.toEntity(mongoTemplate.insert(document));
            } catch (DuplicateKeyException conflict) {
                throw new ConflictException("Cấu hình đã thay đổi. Hãy tải lại trước khi lưu.");
            }
        }

        Criteria criteria = new Criteria();
        criteria.andOperator(
                Criteria.where("_id").is(budget.getId()),
                Criteria.where("revision").is(budget.getRevision())
        );

        Query query = new Query(criteria);

        Update update = new Update()
                .set("monthlyBudgetUsd", budget.getMonthlyBudgetUsd())
                .set("monthlyTokenQuota", budget.getMonthlyTokenQuota())
                .inc("revision", 1);

        AiBudgetDocument result = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                AiBudgetDocument.class
        );
        if (result == null) {
            throw new ConflictException("Cấu hình đã thay đổi. Hãy tải lại trước khi lưu.");
        }

        return mapper.toEntity(result);
    }
}
