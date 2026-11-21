package com.wedu.exam_creation.importer.infrastructure.pandoc.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.importer.dto.pandoc.ExtractedHeaderResult;
import com.wedu.exam_creation.importer.dto.pandoc.HeaderContent;
import com.wedu.exam_creation.importer.dto.pandoc.PandocEleOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class HeaderExtractorTest {
    @Test
    @DisplayName(
            "trả về cấp heading và văn bản đã cắt khoảng trắng khi token có chữ, khoảng trắng và dữ liệu không hỗ trợ")
    void test_execute_001() {
        List<PandocEleOutput> tokens =
                new ObjectMapper()
                        .readValue(
                                """
                [{"t":"Space"},{"t":"Str","c":"Electric"},{"t":"Emph"},{"t":"Space"},
                 {"t":"Str"},{"t":"Str","c":"circuits"},{"t":"Space"}]
                """,
                                new TypeReference<List<PandocEleOutput>>() {});

        ExtractedHeaderResult result = HeaderExtractor.execute(new HeaderContent(3, tokens));

        assertThat(result.getLevel()).isEqualTo(3);
        assertThat(result.getText()).isEqualTo("Electric circuits");
    }

    @Test
    @DisplayName("trả về văn bản rỗng khi không có token heading")
    void test_execute_002() {
        ExtractedHeaderResult result = HeaderExtractor.execute(new HeaderContent(4, List.of()));

        assertThat(result.getLevel()).isEqualTo(4);
        assertThat(result.getText()).isEmpty();
    }
}
