package com.org.payments;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.org.lambda.MakePaymentInput;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@DynamoDBTable(tableName="MerchantPayment")
public class MerchantPayment {
    private String paymentId;
    private String merchantId;
    private String cardNumber;
    private Integer expiryYear;
    private Integer expiryMonth;
    private String currency;
    private Integer amount;
    private String status;
    private String cardName;
    private String billingAddress;
    private Long expiryTimestampSeconds;
    private Long creationTimestampSeconds;
    private Long version;

    @DynamoDBHashKey(attributeName="paymentId")
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

    @DynamoDBAttribute(attributeName="cardNumber")
    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @DynamoDBAttribute(attributeName="cardName")
    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    @DynamoDBAttribute(attributeName="expiryYear")
    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    @DynamoDBAttribute(attributeName="expiryMonth")
    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    @DynamoDBAttribute(attributeName="currency")
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @DynamoDBAttribute(attributeName="amount")
    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    @DynamoDBAttribute(attributeName="status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDBAttribute(attributeName="billingAddress")
    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    @DynamoDBAttribute(attributeName="expiryTimestampSeconds")
    public Long getExpiryTimestampSeconds() {
        return expiryTimestampSeconds;
    }

    public void setExpiryTimestampSeconds(Long expiryTimestampSeconds) {
        this.expiryTimestampSeconds = expiryTimestampSeconds;
    }

    @DynamoDBAttribute(attributeName="creationTimestampSeconds")
    public Long getCreationTimestampSeconds() {
        return creationTimestampSeconds;
    }

    public void setCreationTimestampSeconds(Long creationTimestampSeconds) {
        this.creationTimestampSeconds = creationTimestampSeconds;
    }

    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public static MerchantPayment.MerchantPaymentBuilder builder() {
        return new MerchantPayment.MerchantPaymentBuilder();
    }

    @Override
    public String toString() {
        return "MerchantPayment{" +
            "paymentId='" + paymentId + '\'' +
            ", merchantId='" + merchantId + '\'' +
            ", cardNumber='" + cardNumber + '\'' +
            ", expiryYear=" + expiryYear +
            ", expiryMonth=" + expiryMonth +
            ", currency='" + currency + '\'' +
            ", amount=" + amount +
            ", status='" + status + '\'' +
            ", cardName='" + cardName + '\'' +
            ", billingAddress='" + billingAddress + '\'' +
            ", expiryTimestampSeconds=" + expiryTimestampSeconds +
            ", creationTimestampSeconds=" + creationTimestampSeconds +
            '}';
    }

    public static class MerchantPaymentBuilder {
        private MerchantPayment payment;

        public MerchantPaymentBuilder() {
            this.payment = new MerchantPayment();
        }

        public MerchantPaymentBuilder withMakePaymentInput(MakePaymentInput paymentInput) {
            return withPaymentId(paymentInput.getPaymentId())
                .withMerchantId(paymentInput.getMerchantId())
                .withCardNumber(paymentInput.getCardNumber())
                .withCardName(paymentInput.getCardName())
                .withAmount(paymentInput.getAmount())
                .withCurrency(paymentInput.getCurrency())
                .withExpiryMonth(paymentInput.getExpiryMonth())
                .withExpiryYear(paymentInput.getExpiryYear())
                .withBillingAddress(paymentInput.getBillingAddress());
        }

        public MerchantPaymentBuilder newPayment() {
            this.payment = new MerchantPayment();
            String paymentId = UUID.randomUUID().toString();
            payment.setExpiryTimestampSeconds(Instant.now().plus(10, ChronoUnit.MINUTES).getEpochSecond());
            return withPaymentId(paymentId);
        }

        public MerchantPaymentBuilder withPaymentId(String paymentId) {
            payment.setPaymentId(paymentId);
            return this;
        }

        public MerchantPaymentBuilder withMerchantId(String merchantId) {
            payment.setMerchantId(merchantId);
            return this;
        }

        public MerchantPaymentBuilder withCardNumber(String cardNumber) {
            payment.setCardNumber(cardNumber);
            return this;
        }

        public MerchantPaymentBuilder withCardName(String cardName) {
            payment.setCardName(cardName);
            return this;
        }

        public MerchantPaymentBuilder withExpiryYear(int expiryYear) {
            payment.setExpiryYear(expiryYear);
            return this;
        }

        public MerchantPaymentBuilder withExpiryMonth(int expiryMonth) {
            payment.setExpiryMonth(expiryMonth);
            return this;
        }

        public MerchantPaymentBuilder withCurrency(String currency) {
            payment.setCurrency(currency);
            return this;
        }

        public MerchantPaymentBuilder withAmount(int amount) {
            payment.setAmount(amount);
            return this;
        }

        public MerchantPaymentBuilder withStatus(String status) {
            payment.setStatus(status);
            return this;
        }

        public MerchantPaymentBuilder withBillingAddress(String billingAddress) {
            payment.setBillingAddress(billingAddress);
            return this;
        }

        public MerchantPaymentBuilder withExpiryTimeSeconds(Long expiryTimestampSeconds) {
            payment.setExpiryTimestampSeconds(expiryTimestampSeconds);
            return this;
        }

        public MerchantPaymentBuilder withCreationTimestampSeconds(Long creationTimestampSeconds) {
            payment.setCreationTimestampSeconds(creationTimestampSeconds);
            return this;
        }

        public MerchantPaymentBuilder withVersion(Long version) {
            payment.setVersion(version);
            return this;
        }

        public MerchantPayment build() {
            return payment;
        }
    }
}