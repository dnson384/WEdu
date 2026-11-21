package com.wedu.exam_creation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3ConfigTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(S3Config.class)
                    .withPropertyValues(
                            "aws.accessKey=test-key",
                            "aws.secretKey=test-value",
                            "aws.region=ap-southeast-1");

    @Test
    @DisplayName("trả về S3 client dùng đúng region khi khởi tạo từ cấu hình ứng dụng")
    void test_s3Client_001() {
        contextRunner.run(
                context -> {
                    S3Client client = context.getBean(S3Client.class);

                    assertThat(client.serviceClientConfiguration().region())
                            .isEqualTo(Region.AP_SOUTHEAST_1);
                });
    }

    @Test
    @DisplayName("trả về yêu cầu GET đã ký cho đúng bucket và region khi tạo liên kết đọc ảnh")
    void test_s3Presigner_002() {
        contextRunner.run(
                context -> {
                    S3Presigner presigner = context.getBean(S3Presigner.class);

                    PresignedGetObjectRequest request =
                            presigner.presignGetObject(
                                    presign ->
                                            presign.signatureDuration(Duration.ofMinutes(5))
                                                    .getObjectRequest(
                                                            object ->
                                                                    object.bucket(
                                                                                    "wedu-test-bucket")
                                                                            .key(
                                                                                    "media/test.png")));

                    assertThat(request.url().getHost())
                            .isEqualTo("wedu-test-bucket.s3.ap-southeast-1.amazonaws.com");
                    assertThat(request.url().getPath()).isEqualTo("/media/test.png");
                    assertThat(request.httpRequest().method().name()).isEqualTo("GET");
                });
    }
}
