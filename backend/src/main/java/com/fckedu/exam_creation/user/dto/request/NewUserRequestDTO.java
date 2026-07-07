package com.fckedu.exam_creation.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequestDTO {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8-32 ký tự")
    private String plainPassword;


    @NotBlank
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8-32 ký tự")
    private String confirmPassword;

    @NotBlank
    private String username;

    @Pattern(
            regexp = "^(LOCAL|GOOGLE)$",
            message = "Phương thức đăng nhập phải là LOCAL hoặc GOOGLE"
    )
    private String loginMethod;
}
