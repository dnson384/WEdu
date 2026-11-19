package com.wedu.exam_creation.payment;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.payment.controller.AdminTransactionController;
import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
import com.wedu.exam_creation.payment.usecase.AdminTransactionUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.*;
import java.util.List;
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
    void adminCanReadLocalListWithPrivateNoStore() throws Exception {
        authenticate("ROLE_ADMIN");
        when(usecase.search("", "", "", "", 0))
                .thenReturn(new AdminTransactionDTO.Page(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/admin/transactions").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    void otherRolesCannotList() throws Exception {
        for (String role : List.of("ROLE_TEACHER", "ROLE_MODERATOR")) {
            authenticate(role);
            mvc.perform(get("/admin/transactions").header("Authorization", "Bearer token"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(usecase);
    }

    @Test
    void anonymousCannotList() throws Exception {
        mvc.perform(get("/admin/transactions")).andExpect(status().is4xxClientError());
        verifyNoInteractions(usecase);
    }
}
