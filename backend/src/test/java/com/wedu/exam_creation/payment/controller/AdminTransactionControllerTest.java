package com.wedu.exam_creation.payment.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
import com.wedu.exam_creation.payment.usecase.AdminTransactionUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
        user.setId("admin");
        user.setEmail("admin@example.com");
        user.setRole(role);
        user.setIsActive(true);
        when(tokens.getPayloadFromAccessToken("token"))
                .thenReturn(new ATPayload("parent", "admin", "admin@example.com", role));
        when(details.loadUserByUsername("admin@example.com"))
                .thenReturn(new CustomUserDetails(user));
    }

    @Test
    @DisplayName("trả về HTTP 200 và private no-store khi ADMIN tra cứu giao dịch")
    void test_search_001() throws Exception {
        authenticate("ROLE_ADMIN");
        when(usecase.search("", "", "", "", 0))
                .thenReturn(new AdminTransactionDTO.Page(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/admin/transactions").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    @DisplayName("trả về HTTP 403 khi vai trò khác ADMIN tra cứu giao dịch")
    void test_search_002() throws Exception {
        for (String role : List.of("ROLE_TEACHER", "ROLE_MODERATOR")) {
            authenticate(role);
            mvc.perform(get("/admin/transactions").header("Authorization", "Bearer token"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về lỗi xác thực khi tra cứu giao dịch mà thiếu token")
    void test_search_003() throws Exception {
        mvc.perform(get("/admin/transactions")).andExpect(status().is4xxClientError());
        verifyNoInteractions(usecase);
    }
}
