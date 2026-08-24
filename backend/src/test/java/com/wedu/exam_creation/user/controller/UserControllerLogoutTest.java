package com.wedu.exam_creation.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.user.usecase.UserService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
import jakarta.servlet.http.Cookie;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerLogoutTest {

    private static final String VALID_AT = "mock-at";
    private static final String VALID_RT = "mock-rt";
    private static final String INVALID_RT = "abc.xyz.jqk";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUsecase userUsecase;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Case 1: Đăng xuất thành công với RT hợp lệ")
    void validRT() throws Exception {
        when(userUsecase.logout(VALID_AT, VALID_RT))
                .thenReturn(true);

        mockMvc.perform(post("/user/logout")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).logout(VALID_AT, VALID_RT);
    }

    @Test
    @DisplayName("Case 2: Đăng xuất với RT sai định dạng JWT")
    void malformedRT() throws Exception {
        String malformedRT = "chuoi-token-bay-ba";

        when(userUsecase.logout(VALID_AT, malformedRT))
                .thenThrow(new UnAuthorizedException("RT không hợp lệ"));

        mockMvc.perform(post("/user/logout")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", malformedRT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userUsecase, times(1)).logout(VALID_AT, malformedRT);
    }

    @Test
    @DisplayName("Case 3: RT không hợp lệ")
    void invalidRT() throws Exception {
        when(userUsecase.logout(VALID_AT, INVALID_RT))
                .thenThrow(new UnAuthorizedException("RT không hợp lệ"));

        mockMvc.perform(post("/user/logout")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", INVALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userUsecase, times(1)).logout(VALID_AT, INVALID_RT);
    }


    @Test
    @DisplayName("Case 4: RT không tồn tại")
    void nonExistedRT() throws Exception {
        String nonExistedRT = "non-existed-rt";

        when(userUsecase.logout(VALID_AT, nonExistedRT))
                .thenThrow(new InternalServerException("Xóa Refresh Token thất bại do không tìm thấy hoặc xóa dư. Đang thực hiện Rollback!"));

        mockMvc.perform(post("/user/logout")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", nonExistedRT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(userUsecase, times(1)).logout(VALID_AT, nonExistedRT);
    }

    @Test
    @DisplayName("Case 5: Gọi API logout sai HTTP Method (GET thay vì POST)")
    void wrongHttpMethod() throws Exception {
        mockMvc.perform(get("/user/logout")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT)))
                .andExpect(status().isMethodNotAllowed()); // Trả về 405 Method Not Allowed

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("Case 6: Không truyền cookie refreshToken")
    void missingCookie() throws Exception {
        mockMvc.perform(post("/user/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userUsecase);
    }
}
