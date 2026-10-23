package com.wedu.exam_creation.auth.controller;

import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerLogoutTest {
    private static final String LOGOUT_URL = "/auth/logout";
    private static final String AUTHORIZATION = "Bearer mock-at";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthUsecase authUsecase;

    @Test
    @DisplayName("200 - Trả về true khi usecase đăng xuất thành công")
    void should_returnTrue_when_logoutSucceeds() throws Exception {
        when(authUsecase.logout(AUTHORIZATION)).thenReturn(true);

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(true));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("200 - Trả về false khi usecase trả về false")
    void should_returnFalse_when_usecaseReturnsFalse() throws Exception {
        when(authUsecase.logout(AUTHORIZATION)).thenReturn(false);

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("400 - Thiếu header Authorization")
    void should_returnBadRequest_when_authorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thiếu header Authorization"));

        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("404 - Usecase không tìm thấy refresh token")
    void should_returnNotFound_when_refreshTokenDoesNotExist() throws Exception {
        when(authUsecase.logout(AUTHORIZATION))
                .thenThrow(new NotFoundException("Không tìm thấy RT"));

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy RT"));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("500 - Usecase gặp lỗi khi xóa refresh token")
    void should_returnInternalServerError_when_usecaseThrowsInternalServerException() throws Exception {
        when(authUsecase.logout(AUTHORIZATION))
                .thenThrow(new InternalServerException("Dữ liệu không nhất quán, xóa dư"));

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Dữ liệu không nhất quán, xóa dư"));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("405 - Gọi logout bằng GET")
    void should_returnMethodNotAllowed_when_httpMethodIsUnsupported() throws Exception {
        mockMvc.perform(get(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Phương thức HTTP không được hỗ trợ"));

        verifyNoInteractions(authUsecase);
    }
}
