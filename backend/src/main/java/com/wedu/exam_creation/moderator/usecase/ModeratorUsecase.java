package com.wedu.exam_creation.moderator.usecase;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.moderator.dto.ApproveQuestionFileResponseDTO;
import com.wedu.exam_creation.moderator.dto.RejectQuestionFileDTO;
import com.wedu.exam_creation.moderator.infrastructure.StoredDocumentFile;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import com.wedu.exam_creation.storage.service.S3Service;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ModeratorUsecase {
    private static final Logger log = LoggerFactory.getLogger(ModeratorUsecase.class);
    private final ImporterService importerService;
    private final S3Service s3Service;
    private final MailService mailService;

    public ModeratorUsecase(ImporterService importerService, S3Service s3Service, MailService mailService) {
        this.importerService = importerService;
        this.s3Service = s3Service;
        this.mailService = mailService;
    }

    public ApproveQuestionFileResponseDTO approveQuestionFile(CommonUserResponseAllDTO moderator, String key, MultipartFile editedFile) throws Exception {
        String[] parts = reviewKeyParts(key);

        byte[] original = getQuestionFileContent(key);

        MultipartFile approvedFile = editedFile == null
                ? new StoredDocumentFile(parts[2], original) : editedFile;
        s3Service.validateDocumentFile(approvedFile, 50);

        List<String> questionIds;

        try {
            questionIds = parseDocument(moderator, approvedFile, parts[1]);
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException("Không thể nhập câu hỏi từ tài liệu. Tệp gốc được giữ lại");
        }

        try {
            deleteQuestionFile(key);
            return new ApproveQuestionFileResponseDTO(questionIds, true, "Đã duyệt và nhập câu hỏi thành công");
        } catch (InternalServerException e) {
            return new ApproveQuestionFileResponseDTO(questionIds, false,
                    "Đã nhập câu hỏi nhưng chưa xóa được tệp gốc. Chỉ thử lại thao tác xóa, không duyệt lại");
        }
    }

    public void rejectQuestionFile(RejectQuestionFileDTO request) {
        if (request == null || request.reason() == null || request.reason().trim().isEmpty()
                || request.reason().trim().length() > 2000) {
            throw new BadRequestException("Lý do từ chối phải có từ 1 đến 2000 ký tự");
        }

        String[] parts = reviewKeyParts(request.key());
        String fileName = parts[2];

        int separator = fileName.lastIndexOf('-');
        if (separator <= 0 || separator >= fileName.length() - 6) {
            throw new BadRequestException("Không xác định được email người gửi từ tên tệp");
        }

        String email = fileName.substring(0, separator);

        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();

            if (!email.equals(address.getAddress()) || !email.contains("@")
                    || email.chars().anyMatch(Character::isWhitespace)) {
                throw new AddressException();
            }
        } catch (AddressException e) {
            throw new BadRequestException("Không xác định được email người gửi từ tên tệp");
        }

        // Confirm existence before deleting; no notification for an already absent object.
        getQuestionFileContent(request.key());
        deleteQuestionFile(request.key());

        try {
            mailService.sendQuestionFileRejectionEmail(email, fileName, request.reason().trim());
        } catch (RuntimeException e) {
            log.warn("Không gửi được email từ chối tệp câu hỏi sau khi xóa S3; loại lỗi: {}",
                    e.getClass().getSimpleName());
        }
    }

    private String[] reviewKeyParts(String key) {
        validateQuestionFileKey(key);
        String[] parts = key.split("/", -1);

        if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()
                || !parts[2].toLowerCase(java.util.Locale.ROOT).endsWith(".docx")) {
            throw new BadRequestException("Tệp tin câu hỏi không hợp lệ");
        }

        return parts;
    }

    public List<S3ObjectInfo> listQuestionFiles() {
        try {
            return s3Service.listQuestionFiles("question-files");
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.error("Lỗi xảy ra khi lấy danh sách tệp tin câu hỏi từ S3", e);
            throw new InternalServerException("Không thể lấy danh sách tệp tin câu hỏi");
        }
    }

    public void deleteQuestionFile(String key) {
        validateQuestionFileKey(key);
        try {
            s3Service.deleteFile(key);
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw new InternalServerException("Không thể xóa tệp tin câu hỏi. Vui lòng thử lại");
        }
    }

    private void validateQuestionFileKey(String key) {
        if (key == null || key.isBlank() || !key.startsWith("question-files/")
                || key.endsWith("/")
                || java.util.Arrays.stream(key.split("/", -1))
                .anyMatch(segment -> segment.equals(".") || segment.equals(".."))) {
            throw new BadRequestException("Tệp tin câu hỏi không hợp lệ");
        }
    }

    public byte[] getQuestionFileContent(String key) {
        validateQuestionFileKey(key);
        if (!key.toLowerCase(java.util.Locale.ROOT).endsWith(".docx")) {
            throw new BadRequestException("Chỉ hỗ trợ xem trước tệp DOCX");
        }
        try {
            return s3Service.readDocumentFile(key);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 404) {
                throw new com.wedu.exam_creation.common.exception.NotFoundException("Tệp tin không còn tồn tại");
            }
            throw new InternalServerException("Không thể tải nội dung tệp tin câu hỏi");
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw new InternalServerException("Không thể tải nội dung tệp tin câu hỏi");
        }
    }

    public List<String> parseDocument(CommonUserResponseAllDTO moderator, MultipartFile file, String subject) throws Exception {
        if (!moderator.getRole().equals("ROLE_MODERATOR")) {
            throw new ForbiddenException("Không có quyền truy cập");
        }

        return importerService.parseDocument(file, subject);
    }
}
