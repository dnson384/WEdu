package com.wedu.exam_creation.payment;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.dto.RefundDTO;
import com.wedu.exam_creation.payment.infrastructure.*;
import com.wedu.exam_creation.payment.infrastructure.document.*;
import com.wedu.exam_creation.payment.infrastructure.mapper.*;
import com.wedu.exam_creation.payment.infrastructure.repository.*;
import com.wedu.exam_creation.payment.usecase.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Only runs against an explicitly supplied dedicated replica set, in a newly generated database. */
@EnabledIfEnvironmentVariable(named = "WEDU_PAYMENT_TEST_MONGO_URI", matches = ".+")
class RefundMongoIntegrationTest {
    MongoClient client;
    MongoTemplate mongo;
    RefundRepositoryImpl refunds;
    PaymentRepositoryImpl payments;
    RefundUsecase usecase;
    RefundAiService ai;
    VnpayGateway gateway;
    PaymentOrderEntity order;
    MongoTransactionManager manager;
    String orderId = "a".repeat(64);
    @BeforeEach void setup() {
        client = MongoClients.create(System.getenv("WEDU_PAYMENT_TEST_MONGO_URI"));
        mongo = new MongoTemplate(client, "wedu_refund_test_" + UUID.randomUUID().toString().replace("-", ""));
        mongo.createCollection(PaymentOrderDocument.class); mongo.createCollection(PaymentEmailDocument.class);
        new RefundIndexConfig(mongo).indexes();
        refunds = new RefundRepositoryImpl(mongo, Mappers.getMapper(RefundMapper.class));
        payments = new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        manager = new MongoTransactionManager(mongo.getMongoDatabaseFactory());
        RefundTransactions transactions = new RefundTransactions(manager);
        var users = mock(com.wedu.exam_creation.user.usecase.UserService.class);
        var teacher = new com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO();
        teacher.setAccountType("PLUS"); teacher.setIsActive(true); when(users.findById("teacher")).thenReturn(teacher);
        ai = new RefundAiService(refunds, transactions, users);
        RefundEntitlementService entitlement = mock(RefundEntitlementService.class);
        when(entitlement.preview(any(), any())).thenReturn(Instant.now());
        RefundSettings settings = new RefundSettings(); settings.setEnabled(true);
        VnpayProperties properties = new VnpayProperties(); properties.setEnabled(true); properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-only"); properties.setReturnUrl("https://example.com/me/bill");
        gateway = mock(VnpayGateway.class);
        when(gateway.query(any())).thenAnswer(i -> response("querydr", "01", "00", "123"));
        when(gateway.refund(any(), any())).thenAnswer(i -> response("refund", "02", "05", "456"));
        usecase = new RefundUsecase(refunds, payments, ai, entitlement, transactions, settings, gateway, properties);
        order = new PaymentOrderEntity(); order.setId(orderId); order.setUserId("teacher"); order.setEmail("teacher@example.com");
        order.setAmount(20000); order.setDurationDays(30); order.setCreatedAt(Instant.now().minusSeconds(3600));
        order.setPaidAt(order.getCreatedAt().plusSeconds(20)); order.setStatus("SUCCEEDED"); order.setTransactionNo("123"); order.setRefundTracked(true);
        order.setPlusStartsAt(order.getPaidAt()); order.setPlusExpiresAt(order.getPaidAt().plusSeconds(2592000)); payments.insert(order);
    }
    @AfterEach void cleanup() { if (mongo != null) mongo.getDb().drop(); if (client != null) client.close(); }
    Map<String, String> response(String command, String type, String status, String no) {
        return Map.of("vnp_Command", command, "vnp_TmnCode", "TESTCODE", "vnp_TxnRef", orderId, "vnp_Amount", "2000000",
                "vnp_ResponseCode", "00", "vnp_TransactionType", type, "vnp_TransactionStatus", status, "vnp_TransactionNo", no);
    }
    RefundDTO.Create body() { var b = new RefundDTO.Create(); b.setReason("Không còn nhu cầu"); b.setPolicyVersion(RefundSettings.POLICY); return b; }
    RefundDTO.Item create() { return usecase.create("teacher", orderId, "test-refund-key-12345", body()); }
    @Test void concurrentCreatePersistsOneRequestOneHoldOneEmail() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<String> task = () -> { start.await(10, TimeUnit.SECONDS); return create().getId(); };
            Future<String> one = executor.submit(task), two = executor.submit(task); start.countDown();
            assertThat(one.get(30, TimeUnit.SECONDS)).isEqualTo(two.get(30, TimeUnit.SECONDS));
            assertThat(mongo.getCollection("RefundRequests").countDocuments()).isEqualTo(1);
            assertThat(mongo.getCollection("PaymentEmails").countDocuments()).isEqualTo(1);
            assertThat(refunds.account("teacher").getHolds()).hasSize(1);
        } finally { executor.shutdownNow(); }
    }
    @Test void concurrentAdminsSendOnlyOneRefund() throws Exception {
        var request = create(); ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Boolean> task = () -> {
                start.await(10, TimeUnit.SECONDS); var b = new RefundDTO.Decision(); b.setRevision(request.getRevision());
                try { usecase.decide(null, request.getId(), "admin", "approve", b); return true; }
                catch (AdminTransactionException conflict) { assertThat(conflict.getStatus()).isEqualTo(409); return false; }
            };
            Future<Boolean> one = executor.submit(task), two = executor.submit(task); start.countDown();
            assertThat(List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            verify(gateway, times(1)).refund(any(), any()); assertThat(refunds.find(request.getId()).getStatus()).isEqualTo("PROCESSING");
        } finally { executor.shutdownNow(); }
    }
    @Test void inFlightAiCannotSlipThroughRefundEvaluation() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(); CountDownLatch began = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            Future<?> work = executor.submit(() -> new TransactionTemplate(manager).execute(ignored -> {
                Runnable persisted = ai.begin("teacher"); began.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                persisted.run(); return null;
            }));
            assertThat(began.await(10, TimeUnit.SECONDS)).isTrue(); var request = create();
            assertThat(request.getStatus()).isEqualTo("NEEDS_VERIFICATION"); release.countDown(); work.get(30, TimeUnit.SECONDS);
            var b = new RefundDTO.Decision(); b.setRevision(request.getRevision());
            assertThat(usecase.decide(null, request.getId(), "admin", "approve", b).getStatus()).isEqualTo("REJECTED");
            verify(gateway, never()).refund(any(), any()); assertThat(refunds.account("teacher").getHolds()).isEmpty();
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
