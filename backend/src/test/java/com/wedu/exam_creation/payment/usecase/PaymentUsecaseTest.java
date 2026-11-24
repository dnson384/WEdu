package com.wedu.exam_creation.payment.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.ServiceUnavailableException;
import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.payment.infrastructure.VnpayQueryException;
import com.wedu.exam_creation.user.usecase.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PaymentUsecaseTest {
    @Test
    @DisplayName("trả về URL checkout dùng IP hiện tại khi tạo lại hoặc tải lại đơn")
    void test_create_001() {
        when(repository.findByIdempotencyKeyHash(anyString())).thenReturn(order);
        assertThat(payments.create(teacher, "key-for-test-123456", "192.0.2.10").getPaymentUrl())
                .contains("vnp_IpAddr=192.0.2.10");
        assertThat(payments.get("teacher", id, "192.0.2.20").getPaymentUrl())
                .contains("vnp_IpAddr=192.0.2.20");
        verify(repository, never()).insert(any());
    }

    private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant PLUS_EXPIRY = NOW.plusSeconds(2592000);
    private MockedStatic<Instant> clock;

    @AfterEach
    void closeClock() {
        clock.close();
    }

    final String id = "a".repeat(64);
    IPaymentRepository repository;
    UserService users;
    VnpayGateway gateway;
    VnpayProperties properties;
    PaymentUsecase payments;
    PlatformTransactionManager manager;
    PaymentOrderEntity order;
    CommonUserResponseAllDTO teacher;

    @BeforeEach
    void setup() {
        clock = mockStatic(Instant.class, CALLS_REAL_METHODS);
        clock.when(Instant::now).thenReturn(NOW);
        repository = mock(IPaymentRepository.class);
        users = mock(UserService.class);
        manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        properties = new VnpayProperties();
        properties.setEnabled(true);
        properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-secret-only");
        properties.setReturnUrl("https://example.com/me/bill");
        gateway = spy(new VnpayGateway(properties));
        payments = new PaymentUsecase(repository, users, gateway, properties, manager);
        order = new PaymentOrderEntity();
        order.setId(id);
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        null, null, null, null));
        order.setUserId("teacher");
        order.setEmail("teacher@example.com");
        order.setAmount(20000);
        order.setDurationDays(30);
        order.setStatus("PENDING");
        order.setCreatedAt(NOW);
        order.setExpiresAt(NOW.plusSeconds(900));
        when(repository.find(id)).thenReturn(order);
        when(repository.update(any(), anyLong())).thenReturn(true);
        when(users.grantPaidPlus(eq("teacher"), any(), eq(30))).thenReturn(PLUS_EXPIRY);
        teacher = new CommonUserResponseAllDTO();
        teacher.setId("teacher");
        teacher.setRole("ROLE_TEACHER");
        teacher.setEmail("teacher@example.com");
        teacher.setIsActive(true);
    }

    Map<String, String> result(String response, String status) {
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
                                response,
                                "vnp_TransactionStatus",
                                status,
                                "vnp_TransactionNo",
                                "123456"));
        return signed(values);
    }

    Map<String, String> signed(Map<String, String> values) {
        values.put("vnp_SecureHash", gateway.sign(VnpayGateway.canonical(values)));
        return values;
    }

    PaymentDTO.ReturnConfirmation receipt(Map<String, String> values) {
        PaymentDTO.ReturnConfirmation request = new PaymentDTO.ReturnConfirmation();
        request.setParameters(
                new ArrayList<>(
                        values.entrySet().stream()
                                .map(e -> List.of(e.getKey(), e.getValue()))
                                .toList()));
        return request;
    }

    @Test
    @DisplayName(
            "trả về đơn thành công và quyền PLUS chỉ được cấp một lần khi Return hợp lệ đến trước IPN")
    void test_confirmReturn_002() {
        Map<String, String> values = result("00", "00");
        assertThat(payments.confirmReturn("teacher", id, receipt(values), "127.0.0.1").getStatus())
                .isEqualTo("SUCCEEDED");
        assertThat(payments.confirmReturn("teacher", id, receipt(values), "127.0.0.1").getStatus())
                .isEqualTo("SUCCEEDED");
        assertThat(payments.ipn(values)).containsEntry("RspCode", "02");
        verify(users, times(1)).grantPaidPlus(eq("teacher"), any(), eq(30));
        verify(repository, times(1)).enqueueEmail(any());
        verify(gateway, never()).query(any());
    }

    @Test
    @DisplayName("trả về đơn thành công khi Return hợp lệ đến sau hạn thanh toán")
    void test_confirmReturn_003() {
        order.setStatus("EXPIRED");
        order.setExpiresAt(NOW.minusSeconds(60));
        assertThat(
                        payments.confirmReturn(
                                        "teacher", id, receipt(result("00", "00")), "127.0.0.1")
                                .getStatus())
                .isEqualTo("SUCCEEDED");
    }

    @Test
    @DisplayName("trả về lỗi xác thực khi Return thiếu chữ ký hợp lệ dù client báo thành công")
    void test_confirmReturn_004() {
        Map<String, String> values = result("00", "00");
        values.remove("vnp_SecureHash");
        PaymentDTO.ReturnConfirmation unsigned = receipt(values);
        assertThatThrownBy(() -> payments.confirmReturn("teacher", id, unsigned, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);
        values = result("00", "00");
        values.put("vnp_Amount", "100");
        PaymentDTO.ReturnConfirmation tampered = receipt(values);
        assertThatThrownBy(() -> payments.confirmReturn("teacher", id, tampered, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("trả về lỗi khi Return không khớp chủ đơn, mã đơn, merchant hoặc số tiền")
    void test_confirmReturn_005() {
        assertThatThrownBy(
                        () ->
                                payments.confirmReturn(
                                        "another-teacher",
                                        id,
                                        receipt(result("00", "00")),
                                        "127.0.0.1"))
                .isInstanceOf(NotFoundException.class);
        for (Map.Entry<String, String> change :
                List.of(
                        Map.entry("vnp_TxnRef", "b".repeat(64)),
                        Map.entry("vnp_TmnCode", "OTHER"),
                        Map.entry("vnp_Amount", "100"))) {
            Map<String, String> values = result("00", "00");
            values.put(change.getKey(), change.getValue());
            PaymentDTO.ReturnConfirmation request = receipt(signed(values));
            assertThatThrownBy(() -> payments.confirmReturn("teacher", id, request, "127.0.0.1"))
                    .isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("trả về BadRequestException khi Return có tham số trùng")
    void test_confirmReturn_006() {
        PaymentDTO.ReturnConfirmation request = receipt(result("00", "00"));
        request.getParameters().add(List.of("vnp_Amount", "2000000"));
        assertThatThrownBy(() -> payments.confirmReturn("teacher", id, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("trả về đơn thanh toán thất bại khi Return hợp lệ báo giao dịch lỗi")
    void test_confirmReturn_007() {
        assertThat(
                        payments.confirmReturn(
                                        "teacher", id, receipt(result("24", "02")), "127.0.0.1")
                                .getStatus())
                .isEqualTo("CANCELLED");
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName(
            "trả về mã xác nhận thành công và cấp PLUS một lần khi callback hợp lệ được gửi lặp")
    void test_ipn_008() {
        Map<String, String> result = result("00", "00");
        assertThat(payments.ipn(result)).containsEntry("RspCode", "00");
        assertThat(payments.ipn(result)).containsEntry("RspCode", "02");
        assertThat(order.getStatus()).isEqualTo("SUCCEEDED");
        verify(users, times(1)).grantPaidPlus(eq("teacher"), any(), eq(30));
        verify(repository, times(1)).enqueueEmail(any());
        verify(manager, times(2)).commit(any());
    }

    @Test
    @DisplayName("trả về mã lỗi chữ ký khi chữ ký callback không khớp")
    void test_ipn_009() {
        Map<String, String> result = result("00", "00");
        result.put("vnp_Amount", "100");
        assertThat(payments.ipn(result)).containsEntry("RspCode", "97");
        verify(repository, never()).find(any());
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("trả về mã lỗi số tiền khi IPN có số tiền khác với đơn")
    void test_ipn_010() {
        Map<String, String> result = result("00", "00");
        result.put("vnp_Amount", "100");
        assertThat(payments.ipn(signed(result))).containsEntry("RspCode", "04");
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("trả về mã lỗi tương ứng khi merchant sai hoặc không có đơn")
    void test_ipn_011() {
        Map<String, String> result = result("00", "00");
        result.put("vnp_TmnCode", "OTHER");
        assertThat(payments.ipn(signed(result))).containsEntry("RspCode", "97");
        Map<String, String> missing = result("00", "00");
        missing.put("vnp_TxnRef", "b".repeat(64));
        assertThat(payments.ipn(signed(missing))).containsEntry("RspCode", "01");
    }

    @Test
    @DisplayName("trả về thanh toán thành công khi IPN hợp lệ đến sau lúc đơn hết hạn")
    void test_ipn_012() {
        order.setStatus("EXPIRED");
        order.setExpiresAt(NOW.minusSeconds(60));
        assertThat(payments.ipn(result("00", "00"))).containsEntry("RspCode", "00");
        assertThat(order.getStatus()).isEqualTo("SUCCEEDED");
        verify(users).grantPaidPlus(eq("teacher"), any(), eq(30));
    }

    @Test
    @DisplayName("trả về trạng thái thất bại hoặc hủy khi VNPay báo lỗi hoặc hủy giao dịch")
    void test_ipn_013() {
        assertThat(payments.ipn(result("24", "02"))).containsEntry("RspCode", "00");
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        verifyNoInteractions(users);
        ArgumentCaptor<PaymentEmailEntity> email =
                ArgumentCaptor.forClass(PaymentEmailEntity.class);
        verify(repository).enqueueEmail(email.capture());
        assertThat(email.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("trả về kết quả thất bại và đưa email vào hàng chờ khi giao dịch bị từ chối")
    void test_ipn_014() {
        payments.ipn(result("51", "02"));
        assertThat(order.getStatus()).isEqualTo("FAILED");
        verify(repository).enqueueEmail(any());
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("trả về trạng thái chờ khi mã giao dịch chưa xác định kết quả")
    void test_ipn_015() {
        payments.ipn(result("00", "01"));
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verifyNoInteractions(users);
        verify(repository, never()).enqueueEmail(any());
    }

    @Test
    @DisplayName("trả về đơn đã trả tiền và cần xử lý thủ công khi tài khoản không còn tồn tại")
    void test_ipn_016() {
        when(users.grantPaidPlus(any(), any(), anyInt())).thenReturn(null);
        payments.ipn(result("00", "00"));
        assertThat(order.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(order.isManualReview()).isTrue();
    }

    @Test
    @DisplayName("trả về mã yêu cầu thử lại khi cập nhật đơn thất bại và giao dịch rollback")
    void test_ipn_017() {
        when(repository.update(any(), anyLong()))
                .thenThrow(new IllegalStateException("db unavailable"));
        assertThat(payments.ipn(result("00", "00"))).containsEntry("RspCode", "99");
        verify(manager).rollback(any());
        verify(repository, never()).enqueueEmail(any());
    }

    @Test
    @DisplayName("trả về kết quả của lần xử lý thắng khi cập nhật giao dịch xung đột")
    void test_ipn_018() {
        when(repository.update(any(), anyLong()))
                .thenThrow(new OptimisticLockingFailureException("race"));
        // The second read models another transaction having already committed success.
        assertThat(payments.ipn(result("00", "00"))).containsEntry("RspCode", "02");
        verify(manager).rollback(any());
        verify(manager).commit(any());
    }

    @Test
    @DisplayName("trả về NotFoundException khi tài khoản khác đọc hoặc xác minh đơn")
    void test_get_019() {
        assertThatThrownBy(() -> payments.get("other", id, "127.0.0.1"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> payments.verify("other", id, "127.0.0.1"))
                .isInstanceOf(NotFoundException.class);
        verify(gateway, never()).query(any());
    }

    @Test
    @DisplayName("trả về đơn hết hạn và không cấp URL khi đọc đơn đã quá hạn")
    void test_get_020() {
        order.setExpiresAt(NOW.minusSeconds(1));
        assertThat(payments.get("teacher", id, "127.0.0.1").getStatus()).isEqualTo("EXPIRED");
        assertThat(payments.get("teacher", id, "127.0.0.1").getPaymentUrl()).isNull();
        verify(repository, times(1)).enqueueEmail(any());
    }

    @Test
    @DisplayName("trả về giá từ server và cùng mã đơn khi gửi lại cùng idempotency key")
    void test_create_021() {
        when(repository.insert(any())).thenAnswer(i -> i.getArgument(0));
        PaymentDTO.Order first = payments.create(teacher, "key-for-test-123456", "127.0.0.1");
        ArgumentCaptor<PaymentOrderEntity> capture =
                ArgumentCaptor.forClass(PaymentOrderEntity.class);
        verify(repository).insert(capture.capture());
        assertThat(capture.getValue().getExpiresAt())
                .isEqualTo(capture.getValue().getCreatedAt().plusSeconds(300));
        assertThat(first.getOrderId()).matches("[a-f0-9]{64}");
        assertThat(capture.getValue().getIdempotencyKeyHash())
                .matches("[a-f0-9]{64}")
                .isNotEqualTo(first.getOrderId());
        when(repository.findByIdempotencyKeyHash(capture.getValue().getIdempotencyKeyHash()))
                .thenReturn(capture.getValue());
        PaymentDTO.Order second = payments.create(teacher, "key-for-test-123456", "127.0.0.2");
        assertThat(second.getOrderId()).isEqualTo(first.getOrderId());
        assertThat(second.getExpiresAt()).isEqualTo(first.getExpiresAt());
        assertThat(second.getAmount()).isEqualTo(20000);
        assertThat(second.getPaymentUrl())
                .contains("vnp_Amount=2000000")
                .doesNotContain("vnp_BankCode=");
        verify(repository, times(1)).insert(any());
    }

    @Test
    @DisplayName("trả về mã tham chiếu mới khi yêu cầu lại bằng key của đơn đã bị xóa")
    void test_create_022() {
        when(repository.insert(any())).thenAnswer(i -> i.getArgument(0));
        PaymentDTO.Order first = payments.create(teacher, "key-for-test-123456", "127.0.0.1");
        // The repository remains empty, simulating deletion between requests.
        PaymentDTO.Order second = payments.create(teacher, "key-for-test-123456", "127.0.0.1");
        assertThat(second.getOrderId()).matches("[a-f0-9]{64}").isNotEqualTo(first.getOrderId());
        assertThat(second.getPaymentUrl()).contains("vnp_TxnRef=" + second.getOrderId());
        ArgumentCaptor<PaymentOrderEntity> capture =
                ArgumentCaptor.forClass(PaymentOrderEntity.class);
        verify(repository, times(2)).insert(capture.capture());
        assertThat(capture.getAllValues().get(0).getIdempotencyKeyHash())
                .isEqualTo(capture.getAllValues().get(1).getIdempotencyKeyHash());
    }

    @Test
    @DisplayName("trả về đơn cũ khi gửi lại key của đơn tạo theo cơ chế legacy")
    void test_create_023() throws Exception {
        String hash =
                java.util.HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                "teacher:key-for-test-123456"
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8)));
        order.setId(hash);
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        null, null, null, null));
        when(repository.find(hash)).thenReturn(order);
        assertThat(payments.create(teacher, "key-for-test-123456", "127.0.0.1").getOrderId())
                .isEqualTo(hash);
        verify(repository, never()).insert(any());
    }

    @Test
    @DisplayName("trả về đơn đã tạo khi request đồng thời vướng ràng buộc unique key")
    void test_create_024() {
        when(repository.findByIdempotencyKeyHash(anyString())).thenReturn(null, order);
        when(repository.insert(any()))
                .thenThrow(new DuplicateKeyException("request already inserted"));
        assertThat(payments.create(teacher, "key-for-test-123456", "127.0.0.1").getOrderId())
                .isEqualTo(id);
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("trả về lỗi duplicate key khi khóa xung đột không có đơn thắng tương ứng")
    void test_create_025() {
        when(repository.insert(any())).thenThrow(new DuplicateKeyException("other conflict"));
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("trả về lỗi phù hợp khi cấu hình tắt, vai trò sai hoặc tài khoản khóa")
    void test_create_026() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(InternalServerException.class);
        properties.setEnabled(true);
        teacher.setRole("ROLE_ADMIN");
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
        teacher.setRole("ROLE_TEACHER");
        teacher.setIsActive(false);
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).insert(any());
    }

    @Test
    @DisplayName("trả về ConflictException khi xác minh lại trước khi hết thời gian chờ")
    void test_verify_027() {
        assertThat(payments.verify("teacher", id, "127.0.0.1").getStatus()).isEqualTo("PENDING");
        verify(gateway, never()).query(any());
    }

    @Test
    @DisplayName("trả về trạng thái chờ khi mã thành công query thiếu kết quả giao dịch thành công")
    void test_verify_028() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        doReturn(result("00", "01")).when(gateway).query(order);
        assertThat(payments.verify("teacher", id, "127.0.0.1").getStatus()).isEqualTo("PENDING");
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName(
            "trả về đơn thành công và kết quả cấp PLUS duy nhất khi query đã xác minh rồi nhận IPN")
    void test_verify_029() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        Map<String, String> result = result("00", "00");
        result.put("vnp_TransactionType", "01");
        doReturn(result).when(gateway).query(order);
        assertThat(payments.verify("teacher", id, "127.0.0.1").getStatus()).isEqualTo("SUCCEEDED");
        assertThat(payments.ipn(signed(result))).containsEntry("RspCode", "02");
        verify(users, times(1)).grantPaidPlus(any(), any(), anyInt());
    }

    @Test
    @DisplayName("trả về lỗi xác minh và giữ trạng thái chờ khi VNPay timeout")
    void test_verify_030() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException()).when(gateway).query(order);
        assertThatThrownBy(() -> payments.verify("teacher", id, "127.0.0.1"))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("trả về lỗi xác minh khi VNPay trả kết quả chưa thể xác thực")
    void test_verify_031() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        doThrow(new VnpayQueryException("UNVERIFIED_RESPONSE", "99", null))
                .when(gateway)
                .query(order);
        assertThatThrownBy(() -> payments.verify("teacher", id, "127.0.0.1"))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("trả về giá, thời hạn và trạng thái tắt khi merchant chưa được bật")
    void test_plan_032() {
        properties.setEnabled(false);

        assertThat(payments.plan()).isEqualTo(new PaymentDTO.Plan(20_000, "VND", 30, false));
        verifyNoInteractions(repository, users);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và không ghi khi đơn chờ vẫn còn hạn")
    void test_expire_033() {
        payments.expire(id);

        assertThat(order.getStatus()).isEqualTo("PENDING");
        verify(repository, never()).update(any(), anyLong());
        verify(repository, never()).enqueueEmail(any());
    }
}
