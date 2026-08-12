package com.fckedu.exam_creation.chapter.controller;

import com.fckedu.exam_creation.chapter.usecase.ChapterUsecase;
import com.fckedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("category")
public class CategoryController {
    private final ChapterUsecase chapterUsecase;

    public CategoryController(ChapterUsecase chapterUsecase) {
        this.chapterUsecase = chapterUsecase;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ChapterResponseDTO>> getAll() {
        List<ChapterResponseDTO> result = chapterUsecase.getAll();
        return ResponseEntity.ok(result);
    }
}
