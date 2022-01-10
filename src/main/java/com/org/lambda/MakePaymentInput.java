package com.org.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MakePaymentInput {
    private String paymentId;
    private String merchantId;
    private String cardName;
    private String cardNumber;
    private Integer expiryYear;
    private Integer expiryMonth;
    private String currency;
    private Integer amount;
    private String billingAddress;
    private String cvv;

    @JsonCreator
    public MakePaymentInput(@JsonProperty(value = "paymentId", required = true) String paymentId,
        @JsonProperty(value = "merchantId", required = true) String merchantId,
        @JsonProperty(value = "cardName", required = true) String cardName,
        @JsonProperty(value = "cardNumber", required = true) String cardNumber,
        @JsonProperty(value = "expiryYear", required = true) Integer expiryYear,
        @JsonProperty(value = "expiryMonth", required = true) Integer expiryMonth,
        @JsonProperty(value = "currency", required = true) String currency,
        @JsonProperty(value = "amount", required = true) Integer amount,
        @JsonProperty(value = "billingAddress", required = true) String billingAddress,
        @JsonProperty(value = "cvv", required = true) String cvv) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.expiryYear = expiryYear;
        this.expiryMonth = expiryMonth;
        this.currency = currency;
        this.amount = amount;
        this.billingAddress = billingAddress;
        this.cvv = cvv;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
}
