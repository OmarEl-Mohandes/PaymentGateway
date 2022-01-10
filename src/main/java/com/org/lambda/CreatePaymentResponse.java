package com.org.lambda;

public class CreatePaymentResponse {

    private String paymentId;
    private String status;
    private Long creationTimestampSeconds;

    public CreatePaymentResponse(String paymentId, String status, Long creationTimestampSeconds) {
        this.paymentId = paymentId;
        this.status = status;
        this.creationTimestampSeconds = creationTimestampSeconds;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreationTimestampSeconds() {
        return creationTimestampSeconds;
    }

    public void setCreationTimestampSeconds(Long creationTimestampSeconds) {
        this.creationTimestampSeconds = creationTimestampSeconds;
    }
}
