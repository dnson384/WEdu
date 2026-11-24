package com.wedu.exam_creation.common.exception;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wedu.exam_creation.auth.usecase.VerificationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;

class GlobalExceptionHandlerTest {
    private final Runnable action = mock(Runnable.class);
    private final MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new TestController(action))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

    @Test
    @DisplayName("trả về HTTP 409 và chặn cache khi bản ghi đã bị cập nhật đồng thời")
    void test_conflict_001() throws Exception {
        failWith(new OptimisticLockingFailureException("Thông tin nội bộ"))
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.message").value("Yêu cầu đang thay đổi. Vui lòng tải lại."));
    }

    @Test
    @DisplayName("trả về HTTP 429 kèm Retry-After khi yêu cầu xác thực phải chờ")
    void test_handleVerification_002() throws Exception {
        failWith(new VerificationException("OTP_COOLDOWN", "Vui lòng chờ", 429, 15))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "15"))
                .andExpect(jsonPath("$.message").value("Vui lòng chờ"))
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"))
                .andExpect(jsonPath("$.retryAfter").value(15));
    }

    @Test
    @DisplayName("trả về HTTP 400 và bỏ header Retry-After khi OTP sai và không có thời gian chờ")
    void test_handleVerification_003() throws Exception {
        failWith(new VerificationException("OTP_INVALID", "OTP không hợp lệ", 400))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.message").value("OTP không hợp lệ"))
                .andExpect(jsonPath("$.code").value("OTP_INVALID"))
                .andExpect(jsonPath("$.retryAfter").value(0));
    }

    @Test
    @DisplayName("trả về HTTP 503 kèm mã lỗi và thời gian chờ khi không thể xác minh thanh toán")
    void test_handleServiceUnavailable_004() throws Exception {
        failWith(new ServiceUnavailableException("Hãy thử lại sau"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.message").value("Hãy thử lại sau"))
                .andExpect(jsonPath("$.code").value("PAYMENT_VERIFICATION_UNAVAILABLE"));
    }

    @ParameterizedTest
    @CsvSource({"429,AI_BUDGET_EXCEEDED", "503,AI_TRACKING_UNAVAILABLE"})
    @DisplayName("trả về mã HTTP và mã lỗi gốc khi quyền gọi AI bị chặn hoặc không thể kiểm tra")
    void test_handleAiAccess_005(int httpStatus, String code) throws Exception {
        failWith(new AiAccessException(code, httpStatus, "Không thể gọi AI"))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.message").value("Không thể gọi AI"))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 với lỗi theo từng trường khi nội dung hợp lệ về JSON nhưng thiếu dữ liệu")
    void test_handleValidationExceptions_006() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Tên là bắt buộc"))
                .andExpect(jsonPath("$.email").value("Email là bắt buộc"));
    }

    @Test
    @DisplayName("trả về HTTP 400 với thông báo thiếu file khi Spring báo lỗi multipart")
    void test_handleMultipartException_007() throws Exception {
        failWith(new MultipartException("Thông tin nội bộ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thiếu file đầu vào"));
    }

    @Test
    @DisplayName("trả về HTTP 400 với thông báo chung khi nội dung JSON không đọc được")
    void test_handleHttpMessageNotReadable_008() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"));
    }

    @Test
    @DisplayName("trả về HTTP 405 khi endpoint không hỗ trợ phương thức PUT")
    void test_handleMethodNotAllowed_009() throws Exception {
        mvc.perform(put("/test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Phương thức HTTP không được hỗ trợ"));
    }

    @Test
    @DisplayName("trả về HTTP 400 với thông báo riêng khi thiếu header Authorization")
    void test_handleMissingHeaderException_010() throws Exception {
        mvc.perform(get("/test/headers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thiếu header Authorization"));
    }

    @Test
    @DisplayName("trả về HTTP 400 kèm tên header khi thiếu X-Request-Id")
    void test_handleMissingHeaderException_011() throws Exception {
        mvc.perform(get("/test/headers").header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thiếu header: X-Request-Id"));
    }

    @Test
    @DisplayName("trả về HTTP 422 khi thiếu dữ liệu cần để xử lý yêu cầu")
    void test_handleUnprocessableEntity_012() throws Exception {
        failWith(new UnprocessableEntityException("Lỗi thử nghiệm"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 409 khi yêu cầu xung đột với trạng thái hiện tại")
    void test_handleConflict_013() throws Exception {
        failWith(new ConflictException("Lỗi thử nghiệm"))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 400 khi nghiệp vụ từ chối dữ liệu đầu vào")
    void test_handleBadRequest_014() throws Exception {
        failWith(new BadRequestException("Lỗi thử nghiệm"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 401 khi yêu cầu chưa được xác thực")
    void test_handleUnauthorized_015() throws Exception {
        failWith(new UnAuthorizedException("Lỗi thử nghiệm"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 403 khi người dùng thiếu quyền")
    void test_handleForbidden_016() throws Exception {
        failWith(new ForbiddenException("Lỗi thử nghiệm"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 404 khi tài nguyên không tồn tại")
    void test_handleNotfound_017() throws Exception {
        failWith(new NotFoundException("Lỗi thử nghiệm"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 500 khi ứng dụng báo lỗi nội bộ")
    void test_handleInternalServer_018() throws Exception {
        failWith(new InternalServerException("Lỗi thử nghiệm"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    @Test
    @DisplayName("trả về HTTP 500 khi sinh câu hỏi bằng AI thất bại")
    void test_handleAIGeneration_019() throws Exception {
        failWith(new AIGenerationException("Lỗi thử nghiệm"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.message").value("Lỗi thử nghiệm"));
    }

    private ResultActions failWith(RuntimeException error) throws Exception {
        doThrow(error).when(action).run();
        return mvc.perform(get("/test"));
    }

    @RestController
    static class TestController {
        private final Runnable action;

        TestController(Runnable action) {
            this.action = action;
        }

        @GetMapping("/test")
        ResponseEntity<Void> run() {
            action.run();
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/test")
        ResponseEntity<Void> validate(@Valid @RequestBody TestRequest request) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/test/headers")
        ResponseEntity<Void> requireHeaders(
                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                @RequestHeader("X-Request-Id") String requestId) {
            return ResponseEntity.noContent().build();
        }
    }

    record TestRequest(
            @NotBlank(message = "Tên là bắt buộc") String name,
            @NotBlank(message = "Email là bắt buộc") String email) {}
}
