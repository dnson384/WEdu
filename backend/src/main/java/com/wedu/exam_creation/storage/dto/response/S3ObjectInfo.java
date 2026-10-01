package com.wedu.exam_creation.storage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectInfo {
    private String key;
    private String fileName;
    private long size;
    private Instant lastModified;
    private String presignedUrl;
}
