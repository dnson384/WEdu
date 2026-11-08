package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.controller.AdminTransactionController;
import com.wedu.exam_creation.payment.usecase.AdminTransactionUsecase;
import com.wedu.exam_creation.payment.usecase.AdminTransactionException;
import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
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
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTransactionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminTransactionControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminTransactionUsecase usecase;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean CustomUserDetailsService details;

    void authenticate(String role) {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId("admin"); user.setEmail("admin@example.com"); user.setRole(role); user.setIsActive(true);
        when(tokens.getPayloadFromAccessToken("token")).thenReturn(new ATPayload("parent", "admin", "admin@example.com", role));
        when(details.loadUserByUsername("admin@example.com")).thenReturn(new CustomUserDetails(user));
    }

    @Test void adminCanReadListAndLookupWithPrivateNoStore() throws Exception {
        authenticate("ROLE_ADMIN");
        when(usecase.search("", "", "", "", 0)).thenReturn(new AdminTransactionDTO.Page(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/admin/transactions").header("Authorization", "Bearer token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(header().string("Cache-Control", "private, no-store"));
        mvc.perform(post("/admin/transactions/" + "a".repeat(64) + "/lookup").header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
        verify(usecase).lookup("a".repeat(64));
    }

    @Test void otherRolesCannotListOrLookup() throws Exception {
        for (String role : List.of("ROLE_TEACHER", "ROLE_MODERATOR")) {
            authenticate(role);
            mvc.perform(get("/admin/transactions").header("Authorization", "Bearer token")).andExpect(status().isForbidden());
            mvc.perform(post("/admin/transactions/" + "a".repeat(64) + "/lookup").header("Authorization", "Bearer token")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(usecase);
    }

    @Test void anonymousCannotListOrLookup() throws Exception {
        mvc.perform(get("/admin/transactions")).andExpect(status().is4xxClientError());
        mvc.perform(post("/admin/transactions/" + "a".repeat(64) + "/lookup")).andExpect(status().is4xxClientError());
        verifyNoInteractions(usecase);
    }

    @Test void lookupErrorsPreserveSafeCode() throws Exception {
        authenticate("ROLE_ADMIN");
        when(usecase.lookup(anyString())).thenThrow(new AdminTransactionException(503, "TRANSACTION_LOOKUP_UNAVAILABLE", "Chưa thể tra cứu"));
        mvc.perform(post("/admin/transactions/" + "a".repeat(64) + "/lookup").header("Authorization", "Bearer token"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("TRANSACTION_LOOKUP_UNAVAILABLE"))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }
}
