package com.fckedu.exam_creation.common.dto.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ATPayload {
    private String userId;
    private String email;
    private String role;
}
