package com.wedu.exam_creation.payment.controller;

import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
import com.wedu.exam_creation.payment.usecase.AdminTransactionUsecase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/transactions")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminTransactionController {
    private final AdminTransactionUsecase usecase;

    @GetMapping
    public ResponseEntity<AdminTransactionDTO.Page> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok()
                .header("Cache-Control", "private, no-store")
                .body(usecase.search(keyword, status, from, to, page));
    }
}
