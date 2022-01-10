package com.org.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.org.payments.MerchantPayment;

import java.util.HashMap;
import java.util.Map;

public class DynamoDBClient {
    private DynamoDBMapper mapper;

    public DynamoDBClient(DynamoDBMapper mapper) {
        this.mapper = mapper;
    }

    public DynamoDBMapper getMapper() {
        return this.mapper;
    }

    public void save(MerchantPayment payment) {
        mapper.save(payment);
    }

    public void saveMakePayment(MerchantPayment payment) {
        DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put("paymentId", new ExpectedAttributeValue(new AttributeValue(payment.getPaymentId())));
        expected.put("merchantId", new ExpectedAttributeValue(new AttributeValue(payment.getMerchantId())));

        saveExpression.setExpected(expected);
        saveExpression.setConditionalOperator(ConditionalOperator.AND);

        mapper.save(payment, saveExpression);
    }

    public MerchantPayment getMerchantPayment(String paymentId) {
        return mapper.load(MerchantPayment.class, paymentId);
    }
}
