package com.wedu.exam_creation.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetRoleRequestDTO {
    private String userId;
    private String role;
}
