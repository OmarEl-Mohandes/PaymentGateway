package com.org.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.dynamodb.DynamoDBClient;
import com.org.modules.DynamoDBMapperModule;
import com.org.payments.MerchantPayment;
import com.org.payments.PaymentStatus;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.org.modules.ResponseBuilder.error;
import static com.org.modules.ResponseBuilder.ok;

public class GetPaymentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
	private final DynamoDBClient dynamoDBClient;

	public GetPaymentHandler() {
		dynamoDBClient = new DynamoDBClient(DynamoDBMapperModule.provideDynamoDBMapper());
	}

	public GetPaymentHandler(DynamoDBClient client) {
		this.dynamoDBClient = client;
	}

  @Override
  public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();

			String merchantId = event.getQueryStringParameters().get("merchantId");
			String paymentId = event.getQueryStringParameters().get("paymentId");

			if (merchantId == null || paymentId == null) {
				return error("Missing merhantId/paymentId parameters", 400);
			}

			GetPaymentInput paymentInput = new GetPaymentInput(merchantId, paymentId);

			GetPaymentResponse getPaymentResponse = getPayment(paymentInput);

			objectMapper.setSerializationInclusion(NON_NULL);
			String jsonResponse = objectMapper.writeValueAsString(getPaymentResponse);

			return ok(jsonResponse);
	  	} catch (Exception e) {
		  	return error("Internal Error" , 500);
		}
  }


  GetPaymentResponse getPayment(GetPaymentInput paymentInput) {
		MerchantPayment payment = dynamoDBClient.getMerchantPayment(paymentInput.getPaymentId());
		GetPaymentResponse response = new GetPaymentResponse();
		String mask = "*".repeat(12);

		if (payment == null) {
			response.setFailCode(404);
			response.setFailReason("PaymentId is expired or not found");
		} else if (!payment.getMerchantId().equals(paymentInput.getMerchantId())) {
			response.setFailCode(401);
			response.setFailReason("This merchant doesn't have access to this payment");
		} else {
			response.setStatus(payment.getStatus());
			response.setCreationTimestampSeconds(payment.getCreationTimestampSeconds());
			if (!payment.getStatus().equals(PaymentStatus.Created.name())) {
				response.setAmount(payment.getAmount());
				response.setCurrency(payment.getCurrency());
				response.setCardName(payment.getCardName());
				response.setCardNumber(mask + payment.getCardNumber().substring(12));
				response.setAmount(payment.getAmount());
				response.setExpiryMonth(payment.getExpiryMonth());
				response.setExpiryYear(payment.getExpiryYear());
				response.setBillingAddress(payment.getBillingAddress());
			}
		}
		return response;
  }

}
