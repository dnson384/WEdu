package com.wedu.exam_creation.draft.usecase.util.dto;

import com.wedu.exam_creation.common.dto.chapter.response.BankStatResponseDTO;
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
