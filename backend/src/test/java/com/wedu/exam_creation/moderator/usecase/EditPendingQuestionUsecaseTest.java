package com.wedu.exam_creation.moderator.usecase;

import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.moderator.dto.EditPendingQuestionDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EditPendingQuestionUsecaseTest {
    private final String id = "507f1f77bcf86cd799439011";
    private final Instant version = Instant.parse("2026-10-29T00:00:00Z");
    private final QuestionService questions = mock(QuestionService.class);
    private final S3Service storage = mock(S3Service.class);
    private final EditPendingQuestionUsecase usecase = new EditPendingQuestionUsecase(questions, storage);
    private PendingQuestionDTO original;
    private EditPendingQuestionDTO input;
    private LinkedMultiValueMap<String, MultipartFile> files;

    @BeforeEach
    void setup() {
        original = new PendingQuestionDTO();
        original.setUpdatedAt(version);
        original.setQuestion(content("Xem <img_0>", Map.of(), Map.of("img_0", "Mô tả ảnh AI")));
        original.setOptions(List.of());
        when(questions.getPendingForEdit(id)).thenReturn(original);
        input = new EditPendingQuestionDTO();
        input.setUpdatedAt(version);
        input.setQuestion(content("Xem <img_0>", Map.of(), Map.of("img_0", "Mô tả đã sửa")));
        input.setOptions(List.of());
        files = new LinkedMultiValueMap<>();
    }

    @Test
    void replacesDescriptionWithUploadedKeyAndDeduplicatesReferences() throws Exception {
        input.setQuestion(content("<math_0> <math_0> <img_0> <img_0>", Map.of("math_0", "x^2", "math_8", "unused"), Map.of()));
        var image = image("question.img_0");
        files.add("question.img_0", image);
        when(storage.uploadImageFile(image, "media/questions/" + id, 5)).thenReturn("media/questions/new.png");
        usecase.execute(id, input, files);
        var captor = ArgumentCaptor.forClass(ContentDTO.class);
        verify(questions).updatePendingContent(eq(id), captor.capture(), eq(List.of()), eq(version));
        assertThat(captor.getValue().getVariables().getImage()).containsExactlyEntriesOf(Map.of("img_0", "media/questions/new.png"));
        assertThat(captor.getValue().getVariables().getMath()).containsExactlyEntriesOf(Map.of("math_0", "x^2"));
        verify(storage, times(1)).uploadImageFile(any(), anyString(), eq(5));
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void requiresAnActualImageForAnAiDescription() {
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(BadRequestException.class);
        verify(questions, never()).updatePendingContent(any(), any(), any(), any());
        verifyNoInteractions(storage);
    }

    @Test
    void preservesTrustedExistingImageInsteadOfClientUrl() {
        original.setQuestion(content("<img_0>", Map.of(), Map.of("img_0", "media/old.png")));
        input.setQuestion(content("<img_0>", Map.of(), Map.of("img_0", "https://attacker.test/image.png")));
        usecase.execute(id, input, files);
        var captor = ArgumentCaptor.forClass(ContentDTO.class);
        verify(questions).updatePendingContent(eq(id), captor.capture(), anyList(), eq(version));
        assertThat(captor.getValue().getVariables().getImage()).containsEntry("img_0", "media/old.png");
        verifyNoInteractions(storage);
    }

    @Test
    void removesUnusedVariablesAndDoesNotDeleteSharedOldMedia() {
        input.setQuestion(content("Không dùng ảnh", Map.of("math_0", "unused"), Map.of("img_0", "unused")));
        usecase.execute(id, input, files);
        var captor = ArgumentCaptor.forClass(ContentDTO.class);
        verify(questions).updatePendingContent(eq(id), captor.capture(), anyList(), eq(version));
        assertThat(captor.getValue().getVariables().getImage()).isEmpty();
        assertThat(captor.getValue().getVariables().getMath()).isEmpty();
        verifyNoInteractions(storage);
    }

    @Test
    void cleansNewUploadsWhenConditionalUpdateConflicts() throws Exception {
        files.add("question.img_0", image("question.img_0"));
        when(storage.uploadImageFile(any(), anyString(), eq(5))).thenReturn("media/new.png");
        doThrow(new ConflictException("changed")).when(questions).updatePendingContent(any(), any(), any(), any());
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(ConflictException.class);
        verify(storage).deleteFile("media/new.png");
    }

    @Test
    void cleansEarlierUploadsWhenSecondUploadFails() throws Exception {
        input.setQuestion(content("<img_0><img_1>", Map.of(), Map.of()));
        files.add("question.img_0", image("question.img_0"));
        files.add("question.img_1", image("question.img_1"));
        when(storage.uploadImageFile(any(), anyString(), eq(5))).thenReturn("media/new.png").thenThrow(new IOException());
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(InternalServerException.class);
        verify(storage).deleteFile("media/new.png");
        verify(questions, never()).updatePendingContent(any(), any(), any(), any());
    }

    @Test
    void retainsMediaWhenDatabaseOutcomeIsUnknown() throws Exception {
        files.add("question.img_0", image("question.img_0"));
        when(storage.uploadImageFile(any(), anyString(), eq(5))).thenReturn("media/new.png");
        doThrow(new IllegalStateException("ack lost")).when(questions).updatePendingContent(any(), any(), any(), any());
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(IllegalStateException.class);
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void rejectsStaleVersionBeforeUploading() {
        input.setUpdatedAt(version.minusSeconds(1));
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsMalformedReferencesAndMissingMath() {
        input.setQuestion(content("<math_x>", Map.of(), Map.of()));
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(BadRequestException.class);
        input.setQuestion(content("<math_0>", Map.of(), Map.of()));
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsExtraOrDuplicateImageParts() {
        input.setQuestion(content("Text", Map.of(), Map.of()));
        files.add("question.img_9", image("question.img_9"));
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(BadRequestException.class);
        files.add("question.img_9", image("question.img_9"));
        assertThatThrownBy(() -> usecase.execute(id, input, files)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void keepsImageNamespacesSeparateBetweenQuestionAndOption() throws Exception {
        original.setOptions(List.of(content("<img_0>", Map.of(), Map.of("img_0", "Option description"))));
        input.setOptions(List.of(content("<img_0>", Map.of(), Map.of())));
        files.add("question.img_0", image("question.img_0"));
        files.add("options.0.img_0", image("options.0.img_0"));
        when(storage.uploadImageFile(any(), anyString(), eq(5))).thenReturn("media/question.png", "media/option.png");
        usecase.execute(id, input, files);
        var question = ArgumentCaptor.forClass(ContentDTO.class);
        @SuppressWarnings("unchecked") var options = (ArgumentCaptor<List<ContentDTO>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(questions).updatePendingContent(eq(id), question.capture(), options.capture(), eq(version));
        assertThat(question.getValue().getVariables().getImage()).containsEntry("img_0", "media/question.png");
        assertThat(options.getValue().get(0).getVariables().getImage()).containsEntry("img_0", "media/option.png");
    }

    private ContentDTO content(String text, Map<String, String> math, Map<String, String> image) {
        return new ContentDTO(text, new VariablesDTO(math, image));
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(name, "image.png", "image/png", new byte[]{1, 2, 3});
    }
}
