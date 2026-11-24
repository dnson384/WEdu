package com.wedu.exam_creation.importer.infrastructure.pandoc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.importer.dto.parsed.ParsedDataOutput;
import com.wedu.exam_creation.storage.service.S3Service;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class PandocConverterTest {
    private static final String DOCUMENT =
            """
            {
              "meta": {
                "title":{"c":[{"t":"Str","c":"Electricity"}]},
                "subtitle":{"c":[{"t":"Str","c":"Circuits"}]}
              },
              "blocks":[
                {"t":"Header","c":[1,[],[{"t":"Str","c":"Calculation"}]]},
                {"t":"Header","c":[2,[],[{"t":"Str","c":"Basic"}]]},
                {"t":"Header","c":[3,[],[{"t":"Str","c":"Apply"}]]},
                {"t":"Header","c":[4,[],[{"t":"Str","c":"Choice"}]]},
                {"t":"Para","c":[{"t":"Str","c":"Find"},{"t":"Space"},{"t":"Math","c":[{}, "I"]}]},
                {"t":"Para","c":[{"t":"Image","c":[[],[],["media/circuit.png",""]]}]},
                {"t":"BulletList","c":[[{"t":"Plain","c":[{"t":"Str","c":"2A"}]}]]}
              ]
            }
            """;

    @TempDir private Path temporaryDirectory;

    private final S3Service storage = mock(S3Service.class);
    private final PandocConverter converter = new PandocConverter(storage);
    private final byte[] document = {1, 2, 3};

    @Test
    @DisplayName("trả về chương bài, thống kê và câu hỏi khi Pandoc cung cấp AST hợp lệ kèm ảnh")
    void test_parse_001() throws Exception {
        when(storage.uploadLocalFile(any(File.class), eq("media"))).thenReturn("media/stored.png");
        Process process = processWith(0, DOCUMENT);

        ParsedDataOutput output =
                parseWith(
                        builder ->
                                when(builder.start())
                                        .thenAnswer(
                                                call -> {
                                                    Path workspace = onlyWorkspace();
                                                    assertThat(
                                                                    Files.readAllBytes(
                                                                            workspace.resolve(
                                                                                    "input.docx")))
                                                            .containsExactly(document);
                                                    Path media =
                                                            Files.createDirectory(
                                                                    workspace.resolve("media"));
                                                    Files.write(
                                                            media.resolve("circuit.png"),
                                                            new byte[] {4});
                                                    return process;
                                                }));

        assertThat(output.getChapter().getName()).isEqualTo("Electricity");
        assertThat(output.getChapter().getLessons())
                .singleElement()
                .satisfies(
                        lesson -> {
                            assertThat(lesson.getName()).isEqualTo("Circuits");
                            assertThat(lesson.getBankStats())
                                    .singleElement()
                                    .satisfies(
                                            stat -> {
                                                assertThat(stat.getExerciseType())
                                                        .isEqualTo("Calculation");
                                                assertThat(stat.getDifficultyLevels())
                                                        .containsExactly("Basic");
                                                assertThat(stat.getLearningOutcomes())
                                                        .containsExactly("Apply");
                                                assertThat(stat.getQuestionType())
                                                        .isEqualTo("Choice");
                                                assertThat(stat.getCount()).isEqualTo(1);
                                            });
                        });
        assertThat(output.getQuestions())
                .singleElement()
                .satisfies(
                        question -> {
                            assertThat(question.getQuestion().getTemplate())
                                    .isEqualTo("Find <math_0> \n<img_0>\n ");
                            assertThat(question.getQuestion().getVariables().getMath())
                                    .containsEntry("math_0", "I");
                            assertThat(question.getQuestion().getVariables().getImage())
                                    .containsEntry("img_0", "media/stored.png");
                            assertThat(question.getOptions())
                                    .singleElement()
                                    .satisfies(
                                            option ->
                                                    assertThat(option.getTemplate())
                                                            .isEqualTo("2A"));
                        });
        verify(storage)
                .uploadLocalFile(
                        argThat(file -> file.getName().equals("circuit.png")), eq("media"));
        assertWorkspaceRemoved();
    }

    @Test
    @DisplayName("trả về lỗi chuyển đổi và dọn workspace khi Pandoc trả mã thoát khác không")
    void test_parse_002() throws Exception {
        Process process = processWith(2, "");

        assertThatThrownBy(() -> parseWith(builder -> when(builder.start()).thenReturn(process)))
                .hasMessage("Lỗi chuyển đổi file.");

        verifyNoInteractions(storage);
        assertWorkspaceRemoved();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"blocks\":[]}"})
    @DisplayName("trả về lỗi chuyển đổi và dọn workspace khi AST sai JSON hoặc không chứa câu hỏi")
    void test_parse_003(String stdout) throws Exception {
        Process process = processWith(0, stdout);

        assertThatThrownBy(() -> parseWith(builder -> when(builder.start()).thenReturn(process)))
                .hasMessage("Lỗi chuyển đổi file.");

        verifyNoInteractions(storage);
        assertWorkspaceRemoved();
    }

    @Test
    @DisplayName(
            "trả về lỗi chuyển đổi và dọn workspace khi tiến trình Pandoc không khởi động được")
    void test_parse_004() throws IOException {
        assertThatThrownBy(
                        () ->
                                parseWith(
                                        builder ->
                                                when(builder.start())
                                                        .thenThrow(
                                                                new IOException(
                                                                        "Pandoc unavailable"))))
                .hasMessage("Lỗi chuyển đổi file.");

        verifyNoInteractions(storage);
        assertWorkspaceRemoved();
    }

    @Test
    @DisplayName("trả về lỗi chuyển đổi và dọn workspace khi không tải ảnh lên S3 được")
    void test_parse_005() throws Exception {
        Process process = processWith(0, DOCUMENT);
        when(storage.uploadLocalFile(any(File.class), eq("media")))
                .thenThrow(new IllegalStateException("media storage failure"));

        assertThatThrownBy(
                        () ->
                                parseWith(
                                        builder ->
                                                when(builder.start())
                                                        .thenAnswer(
                                                                call -> {
                                                                    Path media =
                                                                            Files.createDirectory(
                                                                                    onlyWorkspace()
                                                                                            .resolve(
                                                                                                    "media"));
                                                                    Files.write(
                                                                            media.resolve(
                                                                                    "circuit.png"),
                                                                            new byte[] {4});
                                                                    return process;
                                                                })))
                .hasMessage("Lỗi chuyển đổi file.");

        verify(storage).uploadLocalFile(any(File.class), eq("media"));
        assertWorkspaceRemoved();
    }

    private Process processWith(int exitCode, String stdout) throws InterruptedException {
        Process process = mock(Process.class);
        when(process.getInputStream())
                .thenReturn(new ByteArrayInputStream(stdout.getBytes(StandardCharsets.US_ASCII)));
        when(process.waitFor()).thenReturn(exitCode);
        return process;
    }

    private ParsedDataOutput parseWith(ProcessSetup setup) throws Exception {
        String originalDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", temporaryDirectory.toString());
        try (MockedConstruction<ProcessBuilder> builders =
                mockConstruction(
                        ProcessBuilder.class, (builder, context) -> setup.configure(builder))) {
            return converter.parse(document);
        } finally {
            System.setProperty("user.dir", originalDirectory);
        }
    }

    private Path onlyWorkspace() throws IOException {
        try (Stream<Path> workspaces = Files.list(temporaryDirectory.resolve("static/uploads"))) {
            return workspaces.findFirst().orElseThrow();
        }
    }

    private void assertWorkspaceRemoved() throws IOException {
        try (Stream<Path> workspaces = Files.list(temporaryDirectory.resolve("static/uploads"))) {
            assertThat(workspaces).isEmpty();
        }
    }

    @FunctionalInterface
    private interface ProcessSetup {
        void configure(ProcessBuilder builder) throws IOException;
    }
}
