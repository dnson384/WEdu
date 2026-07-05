package com.fckedu.exam_creation.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordPayloadRequestDTO {
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}
