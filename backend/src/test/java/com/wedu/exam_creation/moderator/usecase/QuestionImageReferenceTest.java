package com.wedu.exam_creation.moderator.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class QuestionImageReferenceTest {
    @ParameterizedTest
    @ValueSource(strings = {"media/img.png", "media/questions/Đồ thị.jpg"})
    @DisplayName("trả về true khi ảnh có S3 key hợp lệ trong thư mục media")
    void test_isStoredKey_001(String value) {
        assertThat(QuestionImageReference.isStoredKey(value)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "Đồ thị cần vẽ",
                "https://example.test/img.png",
                "media/",
                "media/x//img.png",
                "media/x/../img.png",
                "media/./img.png",
                "media/x\\img.png",
                "media/https://example.test/img.png"
            })
    @DisplayName("trả về false khi giá trị ảnh là mô tả, URL hoặc key không hợp lệ")
    void test_isStoredKey_002(String value) {
        assertThat(QuestionImageReference.isStoredKey(value)).isFalse();
    }
}
