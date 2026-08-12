package com.fckedu.exam_creation.common.dto.chapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewLessonDataDTO {
    List<NewBankStatDTO> bankStats;
    private String name;
}
