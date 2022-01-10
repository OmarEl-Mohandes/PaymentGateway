package com.org.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatePaymentInput {

    private String merchantId;

    @JsonCreator
    public CreatePaymentInput(@JsonProperty(value = "merchantId", required = true) String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
}
