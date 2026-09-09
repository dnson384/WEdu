package com.wedu.exam_creation.auth.controller;

import com.wedu.exam_creation.auth.usecase.AuthUsecase;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerLogoutTest {
    private static final String VALID_AT = "mock-at";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthUsecase authUsecase;

    @Test
    @DisplayName("200 - Đăng xuất thành công với RT hợp lệ")
    void should_logoutSuccessfully_when_refreshTokenIsValid() throws Exception {
        when(authUsecase.logout("Bearer " + VALID_AT))
                .thenReturn(true);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        verify(authUsecase, times(1)).logout(anyString());
    }

    @Test
    @DisplayName("404 - RT không tồn tại")
    void should_returnNotFound_when_refreshTokenDoesNotExist() throws Exception {
        String nonExistedRT = "non-existed-rt";

        when(authUsecase.logout("Bearer " + nonExistedRT))
                .thenThrow(new NotFoundException("Không tìm thấy RT"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + nonExistedRT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(authUsecase, times(1)).logout(anyString());
    }

    @Test
    @DisplayName("405 - Gọi API logout sai method")
    void should_returnMethodNotAllowed_when_httpMethodIsInvalid() throws Exception {
        mockMvc.perform(get("/auth/logout")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        verify(authUsecase, never()).logout(anyString());
    }
}
