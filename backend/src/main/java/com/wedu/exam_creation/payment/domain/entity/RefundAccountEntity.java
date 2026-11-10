package com.wedu.exam_creation.payment.domain.entity;

import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Serializes refund holds and AI starts without keeping a database transaction over HTTP. */
@Data
public class RefundAccountEntity {
    private String id;
    private Instant trackedSince;
    private Instant lastAiUsedAt;
    private List<String> activeAi = new ArrayList<>();
    private List<String> holds = new ArrayList<>();
    private long revision;
}
