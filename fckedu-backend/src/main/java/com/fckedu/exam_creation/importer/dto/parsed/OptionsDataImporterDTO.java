package com.fckedu.exam_creation.importer.dto.parsed;

public class OptionsDataImporterDTO {
    private String template;
    private VariablesDTO variables;

    public OptionsDataImporterDTO() {
        this.template = "";
        this.variables = new VariablesDTO();
    }

    public OptionsDataImporterDTO(String template, VariablesDTO variables) {
        this.template = template;
        this.variables = variables;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public VariablesDTO getVariables() {
        return variables;
    }

    public void setVariables(VariablesDTO variables) {
        this.variables = variables;
    }
}