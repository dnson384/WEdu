package com.wedu.exam_creation.storage.service;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.service.SecurityService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class S3Service {
    private final Tika tika = new Tika();
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png");

    private final SecurityService securityService;


    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.bucketName}") String bucketName, SecurityService securityService
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.securityService = securityService;
    }

    public String uploadFile(MultipartFile file, String folderName, String accessToken) throws IOException {
        if (accessToken == null || !securityService.validateAccessToken(accessToken)) {
            throw new UnAuthorizedException("AT không hợp lệ");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("Lỗi: Đầu vào là file trống");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Lỗi: File lớn hơn 5MB");
        }

        String detectedType = tika.detect(file.getInputStream());
        if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
            throw new BadRequestException("Lỗi: File không đúng định dạng");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;
        String s3Key = folderName + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

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

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            System.out.println("Không có gì để xóa.");
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.println("Đã xóa thành công file: " + objectKey);

        } catch (Exception e) {
            System.err.println("Lỗi khi xóa file trên S3: " + e.getMessage());
            throw e;
        }
    }
}