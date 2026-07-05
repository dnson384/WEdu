package com.fckedu.exam_creation.importer.dto.parsed;

import java.util.HashMap;
import java.util.Map;

public class VariablesDTO {
    private Map<String, String> math;
    private Map<String, String> image;

    public VariablesDTO() {
        this.math = new HashMap<>();
        this.image = new HashMap<>();
    }

    public VariablesDTO(Map<String, String> math,
                        Map<String, String> image) {
        this.math = math;
        this.image = image;
    }

    public Map<String, String> getMath() {
        return math;
    }

    public void setMath(Map<String, String> math) {
        this.math = math;
    }

    public Map<String, String> getImage() {
        return image;
    }

    public void setImage(Map<String, String> image) {
        this.image = image;
    }
}
