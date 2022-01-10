package com.org.lambda;

public class GetPaymentInput {
    private String merchantId;
    private String paymentId;

    public GetPaymentInput(String merchantId, String paymentId) {
        this.merchantId = merchantId;
        this.paymentId = paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}
