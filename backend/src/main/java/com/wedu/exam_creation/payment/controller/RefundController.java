package com.wedu.exam_creation.payment.controller;

import com.wedu.exam_creation.payment.dto.RefundDTO;
import com.wedu.exam_creation.payment.usecase.RefundUsecase;
import com.wedu.exam_creation.payment.usecase.AdminTransactionException;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RefundController {
    private final RefundUsecase refunds;

    @GetMapping("/payment/orders")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<?> orders(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam(defaultValue = "0") int page) {
        return response(refunds.orders(principal.getUser().getId(), page));
    }
    @PostMapping("/payment/orders/{id}/refunds")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<?> create(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody RefundDTO.Create body) {
        return response(refunds.create(principal.getUser().getId(), id, key, body));
    }
    @GetMapping("/payment/refunds")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<?> mine(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam(defaultValue = "0") int page) {
        return response(refunds.search(principal.getUser().getId(), "", "", "", "", page));
    }
    @GetMapping("/payment/refunds/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<?> mineDetail(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id) {
        return response(refunds.get(principal.getUser().getId(), id));
    }
    @PostMapping("/payment/refunds/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id,
            @RequestBody RefundDTO.Decision body) {
        return response(refunds.decide(principal.getUser().getId(), id, principal.getUser().getId(), "cancel", body));
    }
    @GetMapping("/admin/refunds")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "0") int page) {
        return response(refunds.search(null, keyword, status, from, to, page));
    }
    @GetMapping("/admin/refunds/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> detail(@PathVariable String id) { return response(refunds.get(null, id)); }

    @PostMapping("/admin/refunds/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> approve(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id,
            @RequestBody RefundDTO.Decision body) {
        return response(refunds.decide(null, id, principal.getUser().getId(), "approve", body));
    }
    @PostMapping("/admin/refunds/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> reject(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id,
            @RequestBody RefundDTO.Decision body) {
        return response(refunds.decide(null, id, principal.getUser().getId(), "reject", body));
    }
    @PostMapping("/admin/refunds/{id}/reconcile")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> reconcile(@PathVariable String id) { return response(refunds.reconcile(id)); }

    private ResponseEntity<?> response(Object body) { return ResponseEntity.ok().header("Cache-Control", "private, no-store").body(body); }
    @ExceptionHandler(AdminTransactionException.class)
    public ResponseEntity<?> failure(AdminTransactionException e) {
        return ResponseEntity.status(e.getStatus()).header("Cache-Control", "private, no-store")
                .body(Map.of("message", e.getMessage(), "code", e.getCode()));
    }
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).header("Cache-Control", "private, no-store")
                .body(Map.of("message", "Yêu cầu đang thay đổi. Vui lòng tải lại."));
    }
}
