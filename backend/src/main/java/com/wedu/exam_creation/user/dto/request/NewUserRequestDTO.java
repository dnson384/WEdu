package com.wedu.exam_creation.user.dto.request;

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
    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Email không được chứa ký tự Unicode")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8-32 ký tự")
    @Pattern(regexp = "^\\S+$",
            message = "Mật khẩu không được phép có khoảng trắng")
    private String plainPassword;


    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8-32 ký tự")
    @Pattern(regexp = "^\\S+$",
            message = "Mật khẩu không được phép có khoảng trắng")
    private String confirmPassword;

    @NotBlank(message = "Tên người dùng không được để trống")
    private String username;

    @Pattern(
            regexp = "^(LOCAL|GOOGLE)$",
            message = "Phương thức đăng nhập phải là LOCAL hoặc GOOGLE"
    )
    private String loginMethod;
}
