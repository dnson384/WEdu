package com.wedu.exam_creation.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {
    @NotBlank()
    private String plainToken;

    @NotBlank()
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8-32 ký tự")
    @Pattern(regexp = "^\\S+$",
            message = "Mật khẩu không được phép có khoảng trắng")
    private String newPassword;

    @NotBlank()
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8-32 ký tự")
    @Pattern(regexp = "^\\S+$",
            message = "Xác nhận mật khẩu không được phép có khoảng trắng")
    private String confirmNewPassword;

}
