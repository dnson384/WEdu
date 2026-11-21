package com.wedu.exam_creation.importer.infrastructure.pandoc.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IsArrayEqualTest {
    @Test
    @DisplayName("trả về true khi hai danh sách có cùng phần tử và số lần lặp nhưng khác thứ tự")
    void test_execute_001() {
        assertThat(IsArrayEqual.execute(List.of("B", "A", "B"), List.of("B", "B", "A"))).isTrue();
    }

    @Test
    @DisplayName("trả về false khi hai danh sách có độ dài khác nhau")
    void test_execute_002() {
        assertThat(IsArrayEqual.execute(List.of("A"), List.of("A", "A"))).isFalse();
    }

    @Test
    @DisplayName("trả về false khi số lần xuất hiện của phần tử khác nhau dù độ dài bằng nhau")
    void test_execute_003() {
        assertThat(IsArrayEqual.execute(List.of("A", "A", "B"), List.of("A", "B", "B"))).isFalse();
    }
}
