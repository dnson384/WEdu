package com.wedu.exam_creation.chapter.controller;

import com.wedu.exam_creation.chapter.usecase.ChapterUsecase;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("chapter")
public class ChapterController {
    private final ChapterUsecase chapterUsecase;

    public ChapterController(ChapterUsecase chapterUsecase) {
        this.chapterUsecase = chapterUsecase;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ChapterResponseDTO>> getAll() {
        List<ChapterResponseDTO> result = chapterUsecase.getAll();
        return ResponseEntity.ok(result);
    }
}
