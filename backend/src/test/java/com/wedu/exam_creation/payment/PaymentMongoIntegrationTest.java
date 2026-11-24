package com.wedu.exam_creation.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.payment.infrastructure.PaymentSchedulingConfig;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import com.wedu.exam_creation.payment.infrastructure.repository.PaymentRepositoryImpl;
import com.wedu.exam_creation.payment.usecase.PaymentUsecase;
import com.wedu.exam_creation.user.domain.entity.PlusEntitlement;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import com.wedu.exam_creation.user.usecase.UserService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

// Uses only a newly generated database. Requires a dedicated test replica set.
@EnabledIfEnvironmentVariable(named = "WEDU_PAYMENT_TEST_MONGO_URI", matches = ".+")
class PaymentMongoIntegrationTest {
    @Test
    @DisplayName("trả về số đơn di trú và xóa bản giao dịch tách khi đã xác minh bản nhúng")
    void test_migrate_001() {
        String id = "e".repeat(64);
        mongo.getCollection("PaymentOrders")
                .insertOne(
                        new org.bson.Document("_id", id)
                                .append(
                                        "userId",
                                        new org.bson.types.ObjectId("507f1f77bcf86cd799439011")));
        mongo.getCollection("VnpayTransactions")
                .insertOne(
                        new org.bson.Document("_id", id)
                                .append("transactionNo", "789")
                                .append("responseCode", "00"));
        com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase migration =
                new com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase(
                        new com.wedu.exam_creation.payment.infrastructure.repository
                                .PaymentStorageMigrationRepositoryImpl(mongo),
                        new MongoTransactionManager(mongo.getMongoDatabaseFactory()));
        assertThat(migration.migrate()).isEqualTo(1);
        assertThat(migration.migrate()).isZero();
        assertThat(repository.find(id).getVnpayTransaction().getTransactionNo()).isEqualTo("789");
        assertThat(mongo.getCollection("VnpayTransactions").countDocuments()).isZero();
        assertThat(
                        mongo.findById(id, org.bson.Document.class, "PaymentOrders")
                                .get("vnpayTransaction", org.bson.Document.class))
                .doesNotContainKeys("id", "_id");
    }

    @Test
    @DisplayName("trả về dữ liệu giao dịch được giữ nguyên khi chạy di trú hai lần")
    void test_migrate_002() {
        String userId = "507f1f77bcf86cd799439011";
        String id = "d".repeat(64);
        java.util.Date paidAt = java.util.Date.from(Instant.now().minusSeconds(60));
        mongo.insert(
                new org.bson.Document("_id", id)
                        .append("userId", userId)
                        .append("status", "SUCCEEDED")
                        .append("email", "teacher@example.com")
                        .append("createdAt", paidAt)
                        .append("paidAt", paidAt)
                        .append("plusExpiresAt", java.util.Date.from(initialExpiry))
                        .append("transactionNo", "456")
                        .append("responseCode", "00")
                        .append("transactionStatus", "00")
                        .append("lastVerifiedAt", paidAt)
                        .append("clientIp", "127.0.0.1")
                        .append("manualReview", false)
                        .append("refundTracked", true)
                        .append("plusLegacyProtected", true)
                        .append("revision", 1L),
                "PaymentOrders");
        com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase migration =
                new com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase(
                        new com.wedu.exam_creation.payment.infrastructure.repository
                                .PaymentStorageMigrationRepositoryImpl(mongo),
                        new MongoTransactionManager(mongo.getMongoDatabaseFactory()));
        assertThat(migration.migrate()).isEqualTo(1);
        assertThat(migration.migrate()).isZero();
        org.bson.Document stored = mongo.findById(id, org.bson.Document.class, "PaymentOrders");
        assertThat(stored)
                .containsEntry("userId", new org.bson.types.ObjectId(userId))
                .containsEntry("plusLegacyProtected", true)
                .doesNotContainKeys(
                        "clientIp",
                        "manualReview",
                        "refundTracked",
                        "transactionNo",
                        "responseCode",
                        "transactionStatus",
                        "lastVerifiedAt");
        assertThat(repository.find(id).getVnpayTransaction().getTransactionNo()).isEqualTo("456");
        assertThat(stored.get("vnpayTransaction", org.bson.Document.class))
                .containsEntry("transactionNo", "456")
                .doesNotContainKeys("id", "_id");
        assertThat(mongo.collectionExists("VnpayTransactions")).isFalse();
        assertThat(repository.searchTransactions("456", "SUCCEEDED", null, null, 0)).hasSize(1);
        assertThat(repository.countTransactions("456", "SUCCEEDED", null, null)).isEqualTo(1);
        assertThat(repository.claimVerification(id, "507f1f77bcf86cd799439012", Instant.now()))
                .isFalse();
        assertThat(repository.claimVerification(id, userId, Instant.now())).isTrue();
        assertThat(repository.claimVerification(id, userId, Instant.now())).isFalse();
        assertThat(mongo.collectionExists("RefundAccounts")).isFalse();
    }

    MongoClient client;
    MongoTemplate mongo;
    PaymentUsecase payments;
    VnpayGateway gateway;
    PaymentRepositoryImpl repository;
    Instant initialExpiry;

    @BeforeEach
    void setup() {
        client = MongoClients.create(System.getenv("WEDU_PAYMENT_TEST_MONGO_URI"));
        mongo =
                new MongoTemplate(
                        client,
                        "wedu_payment_test_" + UUID.randomUUID().toString().replace("-", ""));
        mongo.createCollection(UserDocument.class);
        mongo.createCollection(PaymentOrderDocument.class);
        mongo.createCollection(PaymentEmailDocument.class);
        new PaymentSchedulingConfig(mongo).indexes();
        repository = new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        UserRepositoryImpl userRepository =
                new UserRepositoryImpl(mongo, Mappers.getMapper(UserMapper.class));
        UserService users = mock(UserService.class);
        when(users.grantPaidPlus(any(), any(), anyInt()))
                .thenAnswer(
                        invocation -> {
                            String id = invocation.getArgument(0);
                            com.wedu.exam_creation.user.domain.entity.UserEntity user =
                                    userRepository.findById(id);
                            if (user == null) return null;
                            Instant expiry =
                                    PlusEntitlement.extend(
                                            user.getPlusExpiresAt(),
                                            invocation.getArgument(1),
                                            invocation.getArgument(2));
                            return userRepository.grantPlus(id, expiry).getPlusExpiresAt();
                        });
        VnpayProperties properties = new VnpayProperties();
        properties.setEnabled(true);
        properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-only");
        properties.setReturnUrl("https://example.com/me/bill");
        gateway = new VnpayGateway(properties);
        payments =
                new PaymentUsecase(
                        repository,
                        users,
                        gateway,
                        properties,
                        new MongoTransactionManager(mongo.getMongoDatabaseFactory()));
        UserDocument user = new UserDocument();
        user.setId("507f1f77bcf86cd799439011");
        user.setAccountType("PLUS");
        initialExpiry =
                Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        user.setPlusExpiresAt(initialExpiry);
        mongo.insert(user);
    }

    @AfterEach
    void cleanup() {
        if (mongo != null && mongo.getDb().getName().startsWith("wedu_payment_test_"))
            mongo.getDb().drop();
        if (client != null) client.close();
    }

    Map<String, String> order(String id) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(id);
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        null, null, null, null));
        order.setUserId("507f1f77bcf86cd799439011");
        order.setEmail("test@example.com");
        order.setStatus("PENDING");
        order.setAmount(20000);
        order.setDurationDays(30);
        order.setCreatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plusSeconds(900));
        repository.insert(order);
        Map<String, String> values =
                new HashMap<>(
                        Map.of(
                                "vnp_TmnCode",
                                "TESTCODE",
                                "vnp_TxnRef",
                                id,
                                "vnp_Amount",
                                "2000000",
                                "vnp_ResponseCode",
                                "00",
                                "vnp_TransactionStatus",
                                "00",
                                "vnp_TransactionNo",
                                "12345"));
        values.put("vnp_SecureHash", gateway.sign(VnpayGateway.canonical(values)));
        return values;
    }

    void parallel(Map<String, String> first, Map<String, String> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Map<String, String>> a =
                    () -> {
                        start.await();
                        return payments.ipn(first);
                    };
            Callable<Map<String, String>> b =
                    () -> {
                        start.await();
                        return payments.ipn(second);
                    };
            Future<Map<String, String>> fa = pool.submit(a), fb = pool.submit(b);
            start.countDown();
            // A bounded conflict retry may ask VNPay to redeliver; exercise that redelivery too.
            for (Map<String, String> result :
                    List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS)))
                assertThat(result.get("RspCode")).isIn("00", "02", "99");
            assertThat(payments.ipn(first).get("RspCode")).isIn("00", "02");
            assertThat(payments.ipn(second).get("RspCode")).isIn("00", "02");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("trả về một đơn cho các yêu cầu song song và mã mới khi xóa đơn cũ")
    void test_create_003() throws Exception {
        CommonUserResponseAllDTO teacher = new CommonUserResponseAllDTO();
        teacher.setId("507f1f77bcf86cd799439011");
        teacher.setEmail("teacher@example.com");
        teacher.setRole("ROLE_TEACHER");
        teacher.setIsActive(true);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        String firstId;
        try {
            Callable<String> create =
                    () -> {
                        start.await();
                        return payments.create(teacher, "parallel-create-key-123456", "127.0.0.1")
                                .getOrderId();
                    };
            Future<String> a = pool.submit(create), b = pool.submit(create);
            start.countDown();
            firstId = a.get(20, TimeUnit.SECONDS);
            assertThat(b.get(20, TimeUnit.SECONDS)).isEqualTo(firstId);
            assertThat(mongo.count(new Query(), PaymentOrderDocument.class)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        mongo.remove(Query.query(Criteria.where("_id").is(firstId)), PaymentOrderDocument.class);
        assertThat(payments.create(teacher, "parallel-create-key-123456", "127.0.0.1").getOrderId())
                .isNotEqualTo(firstId);
    }

    @Test
    @DisplayName("trả về một lần cấp PLUS khi Return và IPN cùng xác nhận một đơn")
    void test_confirmReturn_004() throws Exception {
        Map<String, String> values = order("c".repeat(64));
        PaymentDTO.ReturnConfirmation request = new PaymentDTO.ReturnConfirmation();
        request.setParameters(
                values.entrySet().stream().map(e -> List.of(e.getKey(), e.getValue())).toList());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> returned =
                    pool.submit(
                            () -> {
                                start.await();
                                try {
                                    payments.confirmReturn(
                                            "507f1f77bcf86cd799439011",
                                            "c".repeat(64),
                                            request,
                                            "127.0.0.1");
                                } catch (RuntimeException ignored) {
                                    /* A bounded transaction retry can require redelivery. */
                                }
                                return null;
                            });
            Future<?> ipn =
                    pool.submit(
                            () -> {
                                start.await();
                                return payments.ipn(values);
                            });
            start.countDown();
            returned.get(20, TimeUnit.SECONDS);
            ipn.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(
                        payments.confirmReturn(
                                        "507f1f77bcf86cd799439011",
                                        "c".repeat(64),
                                        request,
                                        "127.0.0.1")
                                .getStatus())
                .isEqualTo("SUCCEEDED");
        assertThat(payments.ipn(values).get("RspCode")).isEqualTo("02");
        assertThat(
                        mongo.findById("507f1f77bcf86cd799439011", UserDocument.class)
                                .getPlusExpiresAt())
                .isEqualTo(initialExpiry.plusSeconds(2592000));
        assertThat(mongo.count(new Query(), PaymentEmailDocument.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về một kỳ PLUS khi hai IPN trùng nhau xử lý đồng thời")
    void test_ipn_005() throws Exception {
        Map<String, String> values = order("a".repeat(64));
        parallel(values, values);
        assertThat(
                        mongo.findById("507f1f77bcf86cd799439011", UserDocument.class)
                                .getPlusExpiresAt())
                .isEqualTo(initialExpiry.plusSeconds(2592000));
        assertThat(mongo.count(new Query(), PaymentEmailDocument.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về tổng thời hạn của cả hai kỳ khi hai đơn thành công cùng lúc")
    void test_ipn_006() throws Exception {
        parallel(order("a".repeat(64)), order("b".repeat(64)));
        assertThat(
                        mongo.findById("507f1f77bcf86cd799439011", UserDocument.class)
                                .getPlusExpiresAt())
                .isEqualTo(initialExpiry.plusSeconds(5184000));
        assertThat(mongo.count(new Query(), PaymentEmailDocument.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("trả về mã lỗi retry và rollback khi không ghi được email vào outbox")
    void test_ipn_007() {
        Map<String, String> values = order("a".repeat(64));
        PaymentEmailDocument collision = new PaymentEmailDocument();
        collision.setId("a".repeat(64) + ":SUCCEEDED");
        mongo.insert(collision);
        assertThat(payments.ipn(values)).containsEntry("RspCode", "99");
        assertThat(repository.find("a".repeat(64)).getStatus()).isEqualTo("PENDING");
        assertThat(repository.find("a".repeat(64)).getVnpayTransaction().getTransactionNo())
                .isNull();
        assertThat(repository.find("a".repeat(64)).getVnpayTransaction().getResponseCode())
                .isNull();
        assertThat(
                        mongo.findById("507f1f77bcf86cd799439011", UserDocument.class)
                                .getPlusExpiresAt())
                .isEqualTo(initialExpiry);
    }
}
