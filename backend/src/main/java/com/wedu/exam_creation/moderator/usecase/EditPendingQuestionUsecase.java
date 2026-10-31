package com.wedu.exam_creation.moderator.usecase;

import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.moderator.dto.EditPendingQuestionDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EditPendingQuestionUsecase {
    private static final Logger log = LoggerFactory.getLogger(EditPendingQuestionUsecase.class);
    private static final Pattern REFERENCE = Pattern.compile("<(math|img)_([0-9]+)>");
    private final QuestionService questionService;
    private final S3Service s3Service;

    public EditPendingQuestionUsecase(QuestionService questionService, S3Service s3Service) {
        this.questionService = questionService;
        this.s3Service = s3Service;
    }

    public void execute(String id, EditPendingQuestionDTO input, MultiValueMap<String, MultipartFile> files) {
        if (id == null || !id.matches("[a-fA-F0-9]{24}")) {
            throw new BadRequestException("ID câu hỏi không hợp lệ");
        }

        if (input == null || input.getQuestion() == null || input.getOptions() == null || input.getOptions().size() > 50) {
            throw new BadRequestException("Thiếu nội dung câu hỏi hoặc phương án");
        }

        PendingQuestionDTO original = questionService.getPendingForEdit(id);
        long reviewRevision = input.getReviewRevision() == null ? 0 : input.getReviewRevision();

        if (!Objects.equals(original.getUpdatedAt(), input.getUpdatedAt()) || original.getReviewRevision() != reviewRevision) {
            throw new ConflictException("Câu hỏi đã thay đổi. Vui lòng tải lại danh sách trước khi chỉnh sửa");
        }

        if (original.getLatestRegeneratedQuestion() != null) {
            original.setQuestion(original.getLatestRegeneratedQuestion().getQuestion());
            original.setOptions(original.getLatestRegeneratedQuestion().getOptions());
        }

        if (input.getOptions().size() != original.getOptions().size()) {
            throw new BadRequestException("Không được thay đổi số lượng phương án");
        }

        Map<String, MultipartFile> uploads = new LinkedHashMap<>();
        files.forEach((name, values) -> {
            if (name.equals("payload")) return;

            if (values.size() != 1 || values.get(0).isEmpty() || values.get(0).getSize() > 5L * 1024 * 1024) {
                throw new BadRequestException("Mỗi tham chiếu chỉ nhận một ảnh JPEG/PNG, tối đa 5 MB");
            }
            uploads.put(name, values.get(0));
        });
        if (uploads.size() > 10) {
            throw new BadRequestException("Chỉ tải tối đa 10 ảnh trong một lần lưu");
        }

        Map<String, Map<String, String>> imageParts = new LinkedHashMap<>();

        ContentDTO question = prepareContent(
                "question",
                input.getQuestion(),
                original.getQuestion(),
                uploads,
                imageParts);
        List<ContentDTO> options = new ArrayList<>();

        for (int index = 0; index < input.getOptions().size(); index++) {
            options.add(prepareContent(
                    "options." + index,
                    input.getOptions().get(index),
                    original.getOptions().get(index),
                    uploads,
                    imageParts)
            );
        }

        Set<String> usedFiles = new HashSet<>();
        imageParts.forEach((section, parts) -> parts.forEach((key, value) -> {
            if (value.isEmpty()) {
                usedFiles.add(section + "." + key);
            }
        }));
        if (!usedFiles.equals(uploads.keySet())) {
            throw new BadRequestException("Tệp ảnh không khớp tham chiếu trong nội dung");
        }

        List<String> created = new ArrayList<>();
        try {
            for (Map.Entry<String, Map<String, String>> section : imageParts.entrySet()) {
                for (Map.Entry<String, String> part : section.getValue().entrySet()) {
                    if (!part.getValue().isEmpty()) continue;

                    String s3Key = s3Service.uploadImageFile(
                            uploads.get(section.getKey() + "." + part.getKey()),
                            "media/questions/" + id, 5);

                    created.add(s3Key);
                    part.setValue(s3Key);
                }
            }

            questionService.updatePendingContent(id, question, options, original.getUpdatedAt(), reviewRevision);
        } catch (IOException e) {
            cleanup(created);
            throw new InternalServerException("Không thể tải ảnh. Câu hỏi chưa được cập nhật");
        } catch (ConflictException e) {
            cleanup(created);
            throw e;
        }
    }

    private ContentDTO prepareContent(String section, ContentDTO input, ContentDTO original,
                                      Map<String, MultipartFile> uploads, Map<String, Map<String, String>> imageParts) {
        if (input == null || input.getTemplate() == null || input.getTemplate().isBlank() || input.getTemplate().length() > 50000) {
            throw new BadRequestException("Nội dung không được rỗng hoặc vượt quá 50000 ký tự");
        }

        if (REFERENCE.matcher(input.getTemplate()).replaceAll("").matches("(?s).*<(?:math|img)_.*")) {
            throw new BadRequestException("Tham chiếu phải có dạng <math_0> hoặc <img_0>");
        }

        Map<String, String> math = new LinkedHashMap<>();
        Map<String, String> images = new LinkedHashMap<>();
        Matcher matcher = REFERENCE.matcher(input.getTemplate());
        while (matcher.find()) {
            String key = matcher.group(1) + "_" + matcher.group(2);
            if (matcher.group(1).equals("math")) {
                String latex = input.getVariables() == null ||
                        input.getVariables().getMath() == null ? null : input.getVariables().getMath().get(key);
                if (latex == null || latex.isBlank() || latex.length() > 10000) {
                    throw new BadRequestException("Thiếu hoặc quá dài công thức " + key);
                }

                math.put(key, latex);
            } else if (uploads.containsKey(section + "." + key)) {
                images.put(key, "");
            } else {
                String old = original == null ||
                        original.getVariables() == null ||
                        original.getVariables().getImage() == null ? null : original.getVariables().getImage().get(key);
                if (!QuestionImageReference.isStoredKey(old)) {
                    throw new BadRequestException("Cần tải ảnh cho tham chiếu " + section + "." + key);
                }

                images.put(key, old);
            }
        }
        imageParts.put(section, images);
        VariablesDTO variables = new VariablesDTO(math, images);
        return new ContentDTO(input.getTemplate(), variables);
    }

    private void cleanup(List<String> keys) {
        for (String key : keys) {
            try {
                s3Service.deleteFile(key);
            } catch (RuntimeException ignored) {
                log.warn("Could not remove an uncommitted question image");
            }
        }
    }
}
