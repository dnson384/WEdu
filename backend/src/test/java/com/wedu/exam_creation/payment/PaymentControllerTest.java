package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.controller.PaymentController;
import com.wedu.exam_creation.payment.usecase.PaymentUsecase;
import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.common.exception.ServiceUnavailableException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.service.*;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PaymentUsecase payments;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean CustomUserDetailsService details;

    void authenticate(String role, boolean active) {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId("teacher");
        user.setEmail("test@example.com");
        user.setRole(role);
        user.setIsActive(active);
        when(tokens.getPayloadFromAccessToken("token"))
                .thenReturn(new ATPayload("parent", "teacher", "test@example.com", role));
        when(details.loadUserByUsername("test@example.com")).thenReturn(new CustomUserDetails(user));
    }

    @Test void callbackIsPublicAndReturnsProtocolAcknowledgement() throws Exception {
        when(payments.ipn(any())).thenReturn(Map.of("RspCode", "97", "Message", "Invalid signature"));
        mvc.perform(get("/payment/vnpay/ipn").servletPath("/payment/vnpay/ipn").param("vnp_SecureHash", "bad"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test void duplicateCallbackParametersAreRejectedBeforeUsecase() throws Exception {
        mvc.perform(get("/payment/vnpay/ipn").servletPath("/payment/vnpay/ipn").param("vnp_Amount", "1", "2000000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"));
        verifyNoInteractions(payments);
    }

    @Test void teacherCanReadPlan() throws Exception {
        authenticate("ROLE_TEACHER", true);
        when(payments.plan()).thenReturn(new PaymentDTO.Plan(20000, "VND", 30, true));
        mvc.perform(get("/payment/plan").header("Authorization", "Bearer token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(20000));
    }

    @Test void moderatorAndAdminCannotCreateOrders() throws Exception {
        for (String role : new String[]{"ROLE_ADMIN", "ROLE_MODERATOR"}) {
            authenticate(role, true);
            mvc.perform(post("/payment/orders").header("Authorization", "Bearer token")
                            .header("Idempotency-Key", "test-idempotency-key"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(payments);
    }

    @Test void lockedTeacherCannotCreateOrders() throws Exception {
        authenticate("ROLE_TEACHER", false);
        mvc.perform(post("/payment/orders").header("Authorization", "Bearer token")
                        .header("Idempotency-Key", "test-idempotency-key"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(payments);
    }

    @Test void missingAuthenticationCannotReadOrder() throws Exception {
        mvc.perform(get("/payment/orders/" + "a".repeat(64))).andExpect(status().is4xxClientError());
        verifyNoInteractions(payments);
    }

    @Test void authenticatedTeacherCanSubmitReturnToOwnedOrderUsecase() throws Exception {
        authenticate("ROLE_TEACHER", true);
        when(payments.confirmReturn(eq("teacher"), eq("a".repeat(64)), any()))
                .thenReturn(new PaymentDTO.Order("a".repeat(64), 20000, "SUCCEEDED", null, null, null, false));
        mvc.perform(post("/payment/orders/" + "a".repeat(64) + "/return")
                        .header("Authorization", "Bearer token").contentType("application/json")
                        .content("{\"parameters\":[[\"vnp_TxnRef\",\"" + "a".repeat(64) + "\"]]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test void returnEndpointRequiresAuthenticationAndTeacherRole() throws Exception {
        String path = "/payment/orders/" + "a".repeat(64) + "/return";
        mvc.perform(post(path).contentType("application/json").content("{\"parameters\":[]}"))
                .andExpect(status().is4xxClientError());
        authenticate("ROLE_ADMIN", true);
        mvc.perform(post(path).header("Authorization", "Bearer token")
                        .contentType("application/json").content("{\"parameters\":[]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(payments);
    }

    @Test void unavailableVerificationReturns503AndRetryAfter() throws Exception {
        authenticate("ROLE_TEACHER", true);
        when(payments.verify("teacher", "a".repeat(64)))
                .thenThrow(new ServiceUnavailableException("Đơn đang chờ xác nhận"));
        mvc.perform(post("/payment/orders/" + "a".repeat(64) + "/verify").header("Authorization", "Bearer token"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.code").value("PAYMENT_VERIFICATION_UNAVAILABLE"));
    }
}
