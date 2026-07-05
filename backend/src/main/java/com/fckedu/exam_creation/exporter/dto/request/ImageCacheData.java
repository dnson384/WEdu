package com.fckedu.exam_creation.exporter.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageCacheData {
    private Map<String, String> questions;
    private Map<String, String> options;
}
