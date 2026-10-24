package com.wedu.exam_creation.storage.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StorageQuestionFilesTest {
    private final S3Client client = mock(S3Client.class);

    @Test
    void listsAllPagesAndSignsDownloadsWithoutChangingAvatarUrls() {
        try (S3Presigner signer = S3Presigner.builder().region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-key", "test-secret"))).build()) {
            S3Service service = new S3Service(client, signer, "test-bucket");
            Instant modified = Instant.parse("2026-10-24T00:00:00Z");
            when(client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
                    ListObjectsV2Response.builder().isTruncated(true).nextContinuationToken("page-2")
                            .contents(S3Object.builder().key("question-files/").size(0L).build(),
                                    S3Object.builder().key("question-files/math/Đề thi 1.docx")
                                            .size(123L).lastModified(modified).build()).build(),
                    ListObjectsV2Response.builder().isTruncated(false)
                            .contents(S3Object.builder().key("question-files/second.pdf")
                                    .size(45L).lastModified(modified).build()).build());

            var files = service.listQuestionFiles("question-files");

            assertThat(files).hasSize(2);
            assertThat(files.get(0).getFileName()).isEqualTo("Đề thi 1.docx");
            assertThat(files.get(0).getKey()).isEqualTo("question-files/math/Đề thi 1.docx");
            assertThat(files.get(0).getSize()).isEqualTo(123);
            assertThat(files.get(0).getLastModified()).isEqualTo(modified);
            String url = files.get(0).getPresignedUrl();
            assertThat(url).contains("test-bucket", "X-Amz-Expires=3600", "response-content-disposition=");
            assertThat(java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8))
                    .contains("attachment;", "filename*=UTF-8''");
            assertThat(service.generatePresignedUrl("avatars/test.png"))
                    .doesNotContain("response-content-disposition");
            var requests = ArgumentCaptor.forClass(ListObjectsV2Request.class);
            verify(client, times(2)).listObjectsV2(requests.capture());
            assertThat(requests.getAllValues()).allSatisfy(request -> {
                assertThat(request.bucket()).isEqualTo("test-bucket");
                assertThat(request.prefix()).isEqualTo("question-files/");
            });
            assertThat(requests.getAllValues().get(1).continuationToken()).isEqualTo("page-2");
        }
    }

    @Test
    void returnsEmptyListForEmptyPrefix() {
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().isTruncated(false).build());
        assertThat(new S3Service(client, mock(S3Presigner.class), "test-bucket")
                .listQuestionFiles("question-files")).isEmpty();
    }

    @Test
    void propagatesSigningFailureRatherThanReturningPartialResults() {
        S3Presigner signer = mock(S3Presigner.class);
        when(client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
                ListObjectsV2Response.builder().contents(S3Object.builder()
                        .key("question-files/test.pdf").size(1L).build()).build());
        when(signer.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.create("signing failed"));
        assertThatThrownBy(() -> new S3Service(client, signer, "test-bucket").listQuestionFiles("question-files"))
                .isInstanceOf(software.amazon.awssdk.core.exception.SdkClientException.class);
    }
}
