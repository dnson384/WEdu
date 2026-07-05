package com.fckedu.exam_creation.importer.dto.parsed;

import java.util.List;

public class LessonDataImporterDTO {

    private String name;
    private List<BankStatDTO> bankStats;

    public LessonDataImporterDTO() {
    }

    public LessonDataImporterDTO(String name,
                                 List<BankStatDTO> bankStats) {
        this.name = name;
        this.bankStats = bankStats;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BankStatDTO> getBankStats() {
        return bankStats;
    }

    public void setBankStats(List<BankStatDTO> bankStats) {
        this.bankStats = bankStats;
    }
}
