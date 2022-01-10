package com.org.lambda;

public class MakePaymentResponse {

    private String paymentStatus;
    private Integer failCode;
    private String failReason;

    public MakePaymentResponse(String paymentStatus, Integer failCode, String failReason) {
        this.paymentStatus = paymentStatus;
        this.failCode = failCode;
        this.failReason = failReason;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getFailCode() {
        return failCode;
    }

    public void setFailCode(Integer failCode) {
        this.failCode = failCode;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
