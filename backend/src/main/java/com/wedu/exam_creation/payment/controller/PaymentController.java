package com.wedu.exam_creation.payment.controller;

import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.payment.usecase.PaymentUsecase;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentUsecase paymentUsecase;

    @GetMapping("/plan")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<PaymentDTO.Plan> plan() {
        return response(paymentUsecase.plan());
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<PaymentDTO.Order> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader("Idempotency-Key") String key, HttpServletRequest request) {
        return response(paymentUsecase.create(principal.getUser(), key, request.getRemoteAddr()));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<PaymentDTO.Order> get(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String id) {
        return response(paymentUsecase.get(principal.getUser().getId(), id));
    }

    @PostMapping("/orders/{id}/verify")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<PaymentDTO.Order> verify(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String id) {
        return response(paymentUsecase.verify(principal.getUser().getId(), id));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> ipn(@RequestParam MultiValueMap<String, String> parameters) {
        if (parameters.values().stream().anyMatch(values -> values.size() != 1)) {
            return response(Map.of("RspCode", "97", "Message", "Invalid parameters"));
        }
        return response(paymentUsecase.ipn(parameters.toSingleValueMap()));
    }

    @PostMapping("/orders/{id}/return")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<PaymentDTO.Order> confirmReturn(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String id, @RequestBody PaymentDTO.ReturnConfirmation request) {
        return response(paymentUsecase.confirmReturn(principal.getUser().getId(), id, request));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(body);
    }
}
