package com.wedu.exam_creation.ai.controller;

import com.wedu.exam_creation.ai.dto.AiManagementDTO.BudgetDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.StatusDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.UsageReportDTO;
import com.wedu.exam_creation.ai.usecase.AiManagementUsecase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AiManagementController {
    private final AiManagementUsecase aiManagementUsecase;

    public AiManagementController(AiManagementUsecase aiManagementUsecase) {
        this.aiManagementUsecase = aiManagementUsecase;
    }

    @GetMapping("/usage")
    public ResponseEntity<UsageReportDTO> usage(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String model) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(aiManagementUsecase.report(from, to, model));
    }

    @GetMapping("/status")
    public ResponseEntity<StatusDTO> status() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(aiManagementUsecase.status());
    }

    @GetMapping("/budget")
    public ResponseEntity<BudgetDTO> budget() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(aiManagementUsecase.getBudget());
    }

    @PutMapping("/budget")
    public ResponseEntity<BudgetDTO> updateBudget(@RequestBody BudgetDTO input) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(aiManagementUsecase.updateBudget(input));
    }
}
