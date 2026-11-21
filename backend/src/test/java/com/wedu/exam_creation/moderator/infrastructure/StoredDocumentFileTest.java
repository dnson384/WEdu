package com.wedu.exam_creation.moderator.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoredDocumentFileTest {
    private final byte[] bytes = {1, 2, 3};
    private final StoredDocumentFile document = new StoredDocumentFile("Đề thi.docx", bytes);

    @Test
    @DisplayName("trả về tên part file khi importer nhận tài liệu đã lưu")
    void test_getName_001() {
        assertThat(document.getName()).isEqualTo("file");
    }

    @Test
    @DisplayName("trả về tên tệp Unicode ban đầu khi importer đọc metadata")
    void test_getOriginalFilename_002() {
        assertThat(document.getOriginalFilename()).isEqualTo("Đề thi.docx");
    }

    @Test
    @DisplayName("trả về MIME DOCX khi importer đọc loại nội dung")
    void test_getContentType_003() {
        assertThat(document.getContentType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    @DisplayName("trả về true khi tài liệu không có byte và false khi có nội dung")
    void test_isEmpty_004() {
        assertThat(new StoredDocumentFile("empty.docx", new byte[0]).isEmpty()).isTrue();
        assertThat(document.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("trả về số byte thực tế khi importer đọc kích thước tài liệu")
    void test_getSize_005() {
        assertThat(document.getSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("trả về bản sao độc lập khi bên gọi lấy mảng byte của tài liệu")
    void test_getBytes_006() {
        byte[] firstRead = document.getBytes();
        firstRead[0] = 99;

        assertThat(document.getBytes()).containsExactly(bytes);
    }

    @Test
    @DisplayName("trả về luồng có thể đọc lại từ đầu khi mở tài liệu nhiều lần")
    void test_getInputStream_007() throws IOException {
        try (InputStream first = document.getInputStream();
                InputStream second = document.getInputStream()) {
            assertThat(first.readAllBytes()).containsExactly(bytes);
            assertThat(second.readAllBytes()).containsExactly(bytes);
        }
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và ghi đúng bytes khi đích ghi là tệp hợp lệ")
    void test_transferTo_008(@TempDir Path directory) throws IOException {
        Path target = directory.resolve("document.docx");
        document.transferTo(target.toFile());

        assertThat(Files.readAllBytes(target)).containsExactly(bytes);
    }

    @Test
    @DisplayName("trả về IOException khi thư mục cha của tệp đích không tồn tại")
    void test_transferTo_009(@TempDir Path directory) {
        Path target = directory.resolve("missing/document.docx");

        assertThatThrownBy(() -> document.transferTo(target.toFile()))
                .isInstanceOf(IOException.class);
    }
}
