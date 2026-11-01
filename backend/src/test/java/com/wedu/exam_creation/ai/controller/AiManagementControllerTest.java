package com.wedu.exam_creation.ai.controller;

import com.wedu.exam_creation.ai.dto.AiManagementDTO.*;
import com.wedu.exam_creation.ai.usecase.AiManagementUsecase;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.exception.AiAccessException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiManagementController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AiManagementControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AiManagementUsecase usecase;
    @MockitoBean private CustomOAuth2UserService oauth;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;

    private void role(String role) {
        when(tokens.getPayloadFromAccessToken("test-token")).thenReturn(new ATPayload("jti", "account", "admin@example.com", "ROLE_" + role));
        when(users.loadUserByUsername("admin@example.com")).thenReturn(User.withUsername("admin").password("unused").roles(role).build());
    }

    @Test
    void adminCanReadAndWriteAndResponsesAreNotCached() throws Exception {
        role("ADMIN");
        when(usecase.report("2026-11-01", "2026-11-02", "deepseek-flash")).thenReturn(new UsageReportDTO());
        when(usecase.status()).thenReturn(new StatusDTO());
        when(usecase.getBudget()).thenReturn(new BudgetDTO());
        when(usecase.updateBudget(any())).thenReturn(new BudgetDTO());
        mvc.perform(get("/admin/ai/usage").param("from", "2026-11-01").param("to", "2026-11-02").param("model", "deepseek-flash").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/admin/ai/status").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mvc.perform(get("/admin/ai/budget").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mvc.perform(put("/admin/ai/budget").header("Authorization", "Bearer test-token")
                        .contentType("application/json").content("{\"revision\":0,\"monthlyBudgetUsd\":\"10\",\"monthlyTokenQuota\":1000}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR"})
    void otherRolesCannotReadOrChangeBudget(String role) throws Exception {
        role(role);
        for (String path : new String[]{"status", "budget", "usage?from=2026-11-01&to=2026-11-02"}) {
            mvc.perform(get("/admin/ai/" + path).header("Authorization", "Bearer test-token")).andExpect(status().isForbidden());
        }
        mvc.perform(put("/admin/ai/budget").header("Authorization", "Bearer test-token")
                        .contentType("application/json").content("{\"revision\":0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    void anonymousCannotAccessAdminStatus() throws Exception {
        // Existing JwtAuthenticationFilter returns 400 for a missing Authorization header.
        mvc.perform(get("/admin/ai/status")).andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    void staleRevisionReturnsConflict() throws Exception {
        role("ADMIN");
        when(usecase.updateBudget(any())).thenThrow(new ConflictException("reload"));
        mvc.perform(put("/admin/ai/budget").header("Authorization", "Bearer test-token")
                        .contentType("application/json").content("{\"revision\":0}"))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"revision\":0.5}", "{\"revision\":0,\"monthlyTokenQuota\":1.5}"})
    void fractionalRevisionAndQuotaAreRejectedBeforeUsecase(String body) throws Exception {
        role("ADMIN");
        mvc.perform(put("/admin/ai/budget").header("Authorization", "Bearer test-token")
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    void aiExceptionHandlerPreservesMachineReadableCode() throws Exception {
        role("ADMIN");
        when(usecase.status()).thenThrow(new AiAccessException("AI_BUDGET_EXCEEDED", 429, "limit"));
        mvc.perform(get("/admin/ai/status").header("Authorization", "Bearer test-token"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("AI_BUDGET_EXCEEDED"));
    }
}
