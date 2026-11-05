package com.wedu.exam_creation.payment;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.payment.infrastructure.PaymentSchedulingConfig;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import com.wedu.exam_creation.payment.infrastructure.repository.PaymentRepositoryImpl;
import com.wedu.exam_creation.payment.usecase.PaymentUsecase;
import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.user.domain.entity.PlusEntitlement;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Uses only a newly generated database. Requires a dedicated test replica set.
@EnabledIfEnvironmentVariable(named = "WEDU_PAYMENT_TEST_MONGO_URI", matches = ".+")
class PaymentMongoIntegrationTest {
    MongoClient client;
    MongoTemplate mongo;
    PaymentUsecase payments;
    VnpayGateway gateway;
    PaymentRepositoryImpl repository;
    Instant initialExpiry;

    @BeforeEach
    void setup() {
        client = MongoClients.create(System.getenv("WEDU_PAYMENT_TEST_MONGO_URI"));
        mongo = new MongoTemplate(client, "wedu_payment_test_" + UUID.randomUUID().toString().replace("-", ""));
        mongo.createCollection(UserDocument.class);
        mongo.createCollection(PaymentOrderDocument.class);
        mongo.createCollection(PaymentEmailDocument.class);
        new PaymentSchedulingConfig(mongo).indexes();
        repository = new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        UserRepositoryImpl userRepository = new UserRepositoryImpl(mongo, Mappers.getMapper(UserMapper.class));
        UserService users = mock(UserService.class);
        when(users.grantPaidPlus(any(), any(), anyInt())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            var user = userRepository.findById(id);
            if (user == null) return null;
            Instant expiry = PlusEntitlement.extend(user.getPlusExpiresAt(), invocation.getArgument(1), invocation.getArgument(2));
            return userRepository.grantPlus(id, expiry).getPlusExpiresAt();
        });
        VnpayProperties properties = new VnpayProperties();
        properties.setEnabled(true);
        properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-only");
        properties.setReturnUrl("https://example.com/me/bill");
        gateway = new VnpayGateway(properties);
        payments = new PaymentUsecase(repository, users, gateway, properties, new MongoTransactionManager(mongo.getMongoDatabaseFactory()));
        UserDocument user = new UserDocument();
        user.setId("teacher");
        user.setAccountType("PLUS");
        initialExpiry = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        user.setPlusExpiresAt(initialExpiry);
        mongo.insert(user);
    }

    @AfterEach
    void cleanup() {
        if (mongo != null && mongo.getDb().getName().startsWith("wedu_payment_test_")) mongo.getDb().drop();
        if (client != null) client.close();
    }

    Map<String, String> order(String id) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(id);
        order.setUserId("teacher");
        order.setEmail("test@example.com");
        order.setStatus("PENDING");
        order.setAmount(20000);
        order.setDurationDays(30);
        order.setCreatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plusSeconds(900));
        repository.insert(order);
        Map<String, String> values = new HashMap<>(Map.of("vnp_TmnCode", "TESTCODE", "vnp_TxnRef", id,
                "vnp_Amount", "2000000", "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_TransactionNo", "12345"));
        values.put("vnp_SecureHash", gateway.sign(VnpayGateway.canonical(values)));
        return values;
    }

    void parallel(Map<String, String> first, Map<String, String> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Map<String, String>> a = () -> {
                start.await();
                return payments.ipn(first);
            };
            Callable<Map<String, String>> b = () -> {
                start.await();
                return payments.ipn(second);
            };
            Future<Map<String, String>> fa = pool.submit(a), fb = pool.submit(b);
            start.countDown();
            // A bounded conflict retry may ask VNPay to redeliver; exercise that redelivery too.
            for (var result : List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS)))
                assertThat(result.get("RspCode")).isIn("00", "02", "99");
            assertThat(payments.ipn(first).get("RspCode")).isIn("00", "02");
            assertThat(payments.ipn(second).get("RspCode")).isIn("00", "02");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void parallelCreatesShareOneOrderAndDeletionNeverReusesProviderReference() throws Exception {
        CommonUserResponseAllDTO teacher = new CommonUserResponseAllDTO();
        teacher.setId("teacher");
        teacher.setEmail("teacher@example.com");
        teacher.setRole("ROLE_TEACHER");
        teacher.setIsActive(true);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        String firstId;
        try {
            Callable<String> create = () -> {
                start.await();
                return payments.create(teacher, "parallel-create-key-123456", "127.0.0.1").getOrderId();
            };
            Future<String> a = pool.submit(create), b = pool.submit(create);
            start.countDown();
            firstId = a.get(20, TimeUnit.SECONDS);
            assertThat(b.get(20, TimeUnit.SECONDS)).isEqualTo(firstId);
            assertThat(mongo.count(new Query(), PaymentOrderDocument.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
        mongo.remove(Query.query(Criteria.where("_id").is(firstId)), PaymentOrderDocument.class);
        assertThat(payments.create(teacher, "parallel-create-key-123456", "127.0.0.1").getOrderId())
                .isNotEqualTo(firstId);
    }

    @Test
    void returnAndIpnShareOneAtomicEntitlementUpdate() throws Exception {
        var values = order("c".repeat(64));
        var request = new PaymentDTO.ReturnConfirmation();
        request.setParameters(values.entrySet().stream().map(e -> List.of(e.getKey(), e.getValue())).toList());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> returned = pool.submit(() -> {
                start.await();
                try { payments.confirmReturn("teacher", "c".repeat(64), request); }
                catch (RuntimeException ignored) { /* A bounded transaction retry can require redelivery. */ }
                return null;
            });
            Future<?> ipn = pool.submit(() -> { start.await(); return payments.ipn(values); });
            start.countDown();
            returned.get(20, TimeUnit.SECONDS);
            ipn.get(20, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertThat(payments.confirmReturn("teacher", "c".repeat(64), request).getStatus()).isEqualTo("SUCCEEDED");
        assertThat(payments.ipn(values).get("RspCode")).isEqualTo("02");
        assertThat(mongo.findById("teacher", UserDocument.class).getPlusExpiresAt())
                .isEqualTo(initialExpiry.plusSeconds(2592000));
        assertThat(mongo.count(new Query(), PaymentEmailDocument.class)).isEqualTo(1);
    }

    @Test
    void simultaneousDuplicateIpnAddsOnlyOnePeriod() throws Exception {
        var values = order("a".repeat(64));
        parallel(values, values);
        assertThat(mongo.findById("teacher", UserDocument.class).getPlusExpiresAt()).isEqualTo(initialExpiry.plusSeconds(2592000));
        assertThat(mongo.count(new Query(), PaymentEmailDocument.class)).isEqualTo(1);
    }

    @Test
    void twoPaidOrdersAccumulateWithoutLostUpdate() throws Exception {
        parallel(order("a".repeat(64)), order("b".repeat(64)));
        assertThat(mongo.findById("teacher", UserDocument.class).getPlusExpiresAt()).isEqualTo(initialExpiry.plusSeconds(5184000));
        assertThat(mongo.count(new Query(), PaymentEmailDocument.class)).isEqualTo(2);
    }

    @Test
    void outboxFailureRollsBackOrderAndEntitlement() {
        var values = order("a".repeat(64));
        PaymentEmailDocument collision = new PaymentEmailDocument();
        collision.setId("a".repeat(64) + ":SUCCEEDED");
        mongo.insert(collision);
        assertThat(payments.ipn(values)).containsEntry("RspCode", "99");
        assertThat(repository.find("a".repeat(64)).getStatus()).isEqualTo("PENDING");
        assertThat(mongo.findById("teacher", UserDocument.class).getPlusExpiresAt()).isEqualTo(initialExpiry);
    }
}
