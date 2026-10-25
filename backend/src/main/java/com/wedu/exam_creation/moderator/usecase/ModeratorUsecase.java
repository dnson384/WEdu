package com.wedu.exam_creation.moderator.usecase;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import com.wedu.exam_creation.storage.service.S3Service;
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

    public ModeratorUsecase(ImporterService importerService, S3Service s3Service) {
        this.importerService = importerService;
        this.s3Service = s3Service;
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
        if (key == null || key.isBlank() || !key.startsWith("question-files/")
                || key.endsWith("/")
                || java.util.Arrays.stream(key.split("/", -1))
                        .anyMatch(segment -> segment.equals(".") || segment.equals(".."))) {
            throw new BadRequestException("Tệp tin câu hỏi không hợp lệ");
        }
        try {
            s3Service.deleteFile(key);
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw new InternalServerException("Không thể xóa tệp tin câu hỏi. Vui lòng thử lại");
        }
    }

    public List<String> parseDocument(CommonUserResponseAllDTO moderator, MultipartFile file, String subject) throws Exception {
        if (!moderator.getRole().equals("ROLE_MODERATOR")) {
            throw new ForbiddenException("Không có quyền truy cập");
        }

        return importerService.parseDocument(file, subject);
    }
}
