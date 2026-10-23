package com.wedu.exam_creation.moderator.usecase;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.importer.service.ImporterService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ModeratorUsecase {
    private final ImporterService importerService;

    public ModeratorUsecase(ImporterService importerService) {
        this.importerService = importerService;
    }

    public List<String> parseDocument(CommonUserResponseAllDTO moderator, MultipartFile file, String subject) throws Exception {
        if (!moderator.getRole().equals("ROLE_MODERATOR")) {
            throw new ForbiddenException("Không có quyền truy cập");
        }

        return importerService.parseDocument(file, subject);
    }
}
