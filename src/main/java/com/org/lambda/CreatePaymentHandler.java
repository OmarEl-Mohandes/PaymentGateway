package com.org.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.dynamodb.DynamoDBClient;
import com.org.modules.DynamoDBMapperModule;
import com.org.payments.MerchantPayment;
import com.org.payments.PaymentStatus;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.org.modules.ResponseBuilder.error;
import static com.org.modules.ResponseBuilder.ok;

public class CreatePaymentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final DynamoDBClient dynamoDBClient;
    private final ObjectMapper objectMapper;

    public CreatePaymentHandler() {
        dynamoDBClient = new DynamoDBClient(DynamoDBMapperModule.provideDynamoDBMapper());
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(NON_NULL);
    }

    public CreatePaymentHandler(DynamoDBClient client) {
        this.dynamoDBClient = client;
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(NON_NULL);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            CreatePaymentInput paymentInput = objectMapper.readValue(event.getBody(), CreatePaymentInput.class);

            CreatePaymentResponse createPaymentResponse = createPayment(paymentInput);

            String jsonResponse = objectMapper.writeValueAsString(createPaymentResponse);

            return ok(jsonResponse);
        } catch (JsonProcessingException e) {
            return error("Input is missing or have extra fields, check docs", 400);
        }
    }

    CreatePaymentResponse createPayment(CreatePaymentInput paymentInput) {
        MerchantPayment payment = MerchantPayment.builder()
            .newPayment()
            .withStatus(PaymentStatus.Created.name())
            .withMerchantId(paymentInput.getMerchantId())
            .withCreationTimestampSeconds(Instant.now().getEpochSecond())
            .build();

        dynamoDBClient.save(payment);

        return new CreatePaymentResponse(payment.getPaymentId(), payment.getStatus(), payment.getCreationTimestampSeconds());
    }

}
