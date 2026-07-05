package com.fckedu.exam_creation.draft.usecase.util.dto;

import com.fckedu.exam_creation.common.dto.category.response.BankStatResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {
    BankStatResponseDTO bankStat;
    Integer remaining;
}
