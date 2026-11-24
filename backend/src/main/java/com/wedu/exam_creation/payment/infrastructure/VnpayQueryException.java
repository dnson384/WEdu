package com.wedu.exam_creation.payment.infrastructure;

import lombok.Getter;

@Getter
public class VnpayQueryException extends RuntimeException {
    private final String stage;
    private final String providerCode;
    private final Integer httpStatus;

    public VnpayQueryException(String stage, String providerCode, Integer httpStatus) {
        super("VNPay query failed: stage=" + stage + ", providerCode=" + providerCode + ", httpStatus=" + httpStatus);
        this.stage = stage;
        this.providerCode = providerCode;
        this.httpStatus = httpStatus;
    }
}
