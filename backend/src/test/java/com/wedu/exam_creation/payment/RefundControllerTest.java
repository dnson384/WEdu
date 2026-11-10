package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.controller.RefundController;
import com.wedu.exam_creation.payment.usecase.RefundUsecase;
import com.wedu.exam_creation.payment.usecase.AdminTransactionException;
import com.wedu.exam_creation.payment.dto.RefundDTO;
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
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RefundController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RefundControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RefundUsecase usecase;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean CustomUserDetailsService details;
    String id = "a".repeat(32);
    void authenticate(String role) {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId("user"); user.setEmail("user@example.com"); user.setRole(role); user.setIsActive(true);
        when(tokens.getPayloadFromAccessToken("token")).thenReturn(new ATPayload("parent", "user", "user@example.com", role));
        when(details.loadUserByUsername("user@example.com")).thenReturn(new CustomUserDetails(user));
    }
    @Test void adminCanReadAndDecideWithPrivateNoStore() throws Exception {
        authenticate("ROLE_ADMIN");
        when(usecase.search(null, "", "", "", "", 0)).thenReturn(new RefundDTO.Page(List.of(), 0, 10, 0, 0, true, "plus-refund-v1"));
        mvc.perform(get("/admin/refunds").header("Authorization", "Bearer token")).andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(10)).andExpect(header().string("Cache-Control", "private, no-store"));
        mvc.perform(post("/admin/refunds/" + id + "/approve").header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0}" )).andExpect(status().isOk());
        verify(usecase).decide(isNull(), eq(id), eq("user"), eq("approve"), any());
    }
    @Test void teacherAndModeratorCannotReadOrMutateAdminRefunds() throws Exception {
        for (String role : List.of("ROLE_TEACHER", "ROLE_MODERATOR")) {
            authenticate(role);
            mvc.perform(get("/admin/refunds").header("Authorization", "Bearer token")).andExpect(status().isForbidden());
            for (String action : List.of("approve", "reject", "reconcile"))
                mvc.perform(post("/admin/refunds/" + id + "/" + action).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0,\"reason\":\"test\"}")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(usecase);
    }
    @Test void teacherReadsAndCancelsOnlyUsingAuthenticatedOwner() throws Exception {
        authenticate("ROLE_TEACHER");
        mvc.perform(get("/payment/refunds/" + id).header("Authorization", "Bearer token")).andExpect(status().isOk());
        verify(usecase).get("user", id);
        mvc.perform(post("/payment/refunds/" + id + "/cancel").header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0,\"userId\":\"other\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/payment/refunds/" + id + "/cancel").header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0}")).andExpect(status().isOk());
        verify(usecase).decide(eq("user"), eq(id), eq("user"), eq("cancel"), any());
    }
    @Test void anonymousCannotAccessRefunds() throws Exception {
        mvc.perform(get("/admin/refunds")).andExpect(status().is4xxClientError());
        mvc.perform(get("/payment/refunds")).andExpect(status().is4xxClientError()); verifyNoInteractions(usecase);
    }
    @Test void missingIdempotencyKeyCannotCreateRequest() throws Exception {
        authenticate("ROLE_TEACHER");
        mvc.perform(post("/payment/orders/" + "b".repeat(64) + "/refunds").header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test\",\"policyVersion\":\"plus-refund-v1\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }
    @Test void missingOwnerRecordIsNotExposed() throws Exception {
        authenticate("ROLE_TEACHER"); when(usecase.get("user", id)).thenThrow(new AdminTransactionException(404, "REFUND_ERROR", "Không tìm thấy yêu cầu."));
        mvc.perform(get("/payment/refunds/" + id).header("Authorization", "Bearer token")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REFUND_ERROR")).andExpect(header().string("Cache-Control", "private, no-store"));
    }
}
