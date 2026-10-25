package com.wedu.exam_creation.storage.service;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

@Service
public class S3Service {
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Duration URL_EXPIRATION = Duration.ofMinutes(60);
    private final Tika tika = new Tika();
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png");

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.bucketName}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public String uploadImageFile(MultipartFile file, String folderName, Integer maxSizeMb) throws IOException {
        validateFileSize(file, maxSizeMb);
        String detectedType;
        try (InputStream input = file.getInputStream()) {
            detectedType = tika.detect(input);
        }
        if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
            throw new BadRequestException("Lỗi: File không đúng định dạng");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;
        return uploadMultipartFile(file, folderName, fileName, detectedType);
    }

    public String updateDocumentFile(MultipartFile file, String folderName, Integer maxSizeMb,
                                     String email) throws IOException {
        validateFileSize(file, maxSizeMb);
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".docx")
                || !isWordDocument(file)) {
            throw new BadRequestException("Lỗi: Chỉ chấp nhận tài liệu Word (.docx)");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Lỗi: Thiếu email người tải lên");
        }
        return uploadMultipartFile(file, folderName,
                email + "-" + normalizeDocumentFileName(originalName), DOCX_CONTENT_TYPE);
    }

    private void validateFileSize(MultipartFile file, Integer maxSizeMb) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Lỗi: Đầu vào là file trống");
        }
        if (maxSizeMb == null || maxSizeMb <= 0) {
            throw new BadRequestException("Lỗi: Giới hạn dung lượng không hợp lệ");
        }
        if (file.getSize() > (long) maxSizeMb * 1024 * 1024) {
            throw new BadRequestException("Lỗi: File lớn hơn " + maxSizeMb + "MB");
        }
    }

    private boolean isWordDocument(MultipartFile file) throws IOException {
        // Tika core may report DOCX as ZIP; check the Word package entries.
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    hasContentTypes |= "[Content_Types].xml".equals(entry.getName());
                    hasDocument |= "word/document.xml".equals(entry.getName());
                }
                if (hasContentTypes && hasDocument) {
                    return true;
                }
            }
        } catch (ZipException e) {
            return false;
        }
        return false;
    }

    private String normalizeDocumentFileName(String originalName) {
        String baseName = originalName.substring(
                Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\')) + 1);
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        String asciiName = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        StringBuilder name = new StringBuilder();
        for (String word : asciiName.split("[^A-Za-z0-9]+")) {
            if (!word.isEmpty()) {
                name.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Lỗi: Tên tài liệu phải chứa chữ cái Latin hoặc chữ số");
        }
        return name + ".docx";
    }

    private String uploadMultipartFile(MultipartFile file, String folderName, String fileName,
                                       String contentType) throws IOException {
        String s3Key = folderName + "/" + fileName;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        try (InputStream input = file.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(input, file.getSize()));
        }

        return s3Key;
    }

    public String uploadLocalFile(File file, String folderName) {
        String fileName = UUID.randomUUID() + "_" + file.getName();
        String s3Key = folderName + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

        return s3Key;
    }

    public String generatePresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        return presign(getObjectRequest);
    }

    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
    }

    public byte[] readDocumentFile(String objectKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName).key(objectKey).build()).asByteArray();
    }

    public List<S3ObjectInfo> listQuestionFiles(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix + "/")
                .build();

        List<S3ObjectInfo> files = new ArrayList<>();
        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            response.contents().stream()
                    .filter(obj -> !obj.key().endsWith("/"))
                    .map(this::toS3ObjectInfo)
                    .forEach(files::add);
            request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();
        } while (Boolean.TRUE.equals(response.isTruncated()));
        return files;
    }

    private S3ObjectInfo toS3ObjectInfo(S3Object obj) {
        String fileName = obj.key().contains("/")
                ? obj.key().substring(obj.key().lastIndexOf('/') + 1)
                : obj.key();

        return new S3ObjectInfo(
                obj.key(),
                fileName,
                obj.size(),
                obj.lastModified(),
                generateDownloadUrl(obj.key(), fileName)
        );
    }

    private String generateDownloadUrl(String objectKey, String fileName) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .responseContentDisposition(ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8).build().toString())
                .build();
        return presign(request);
    }

    private String presign(GetObjectRequest request) {
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(URL_EXPIRATION)
                .getObjectRequest(request)
                .build()).url().toString();
    }
}
