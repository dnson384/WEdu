package com.wedu.exam_creation.moderator.dto;

import java.util.List;

public record ApproveQuestionFileResponseDTO(List<String> questionIds, boolean fileDeleted, String message) {
}
