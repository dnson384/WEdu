package com.wedu.exam_creation.payment.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

@Component
@RequiredArgsConstructor
public class RefundJobs {
    private final RefundUsecase refunds;
    @Scheduled(fixedDelay = 60000)
    public void reconcile() { refunds.reconcileDue(); }
}
