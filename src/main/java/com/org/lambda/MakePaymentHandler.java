package com.org.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.bank.BankSimulator;
import com.org.dynamodb.DynamoDBClient;
import com.org.modules.DynamoDBMapperModule;
import com.org.payments.MerchantPayment;
import com.org.payments.PaymentStatus;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.org.modules.ResponseBuilder.error;
import static com.org.modules.ResponseBuilder.ok;

public class MakePaymentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
	private final DynamoDBClient dynamoDBClient;
	private final ObjectMapper objectMapper;
	private final BankSimulator bankSimulator;
	private LambdaLogger logger;

	public MakePaymentHandler(final DynamoDBClient dynamoDBClient) {
		this.dynamoDBClient = dynamoDBClient;
		this.objectMapper = new ObjectMapper();
		this.bankSimulator = new BankSimulator();
	}

	public MakePaymentHandler() {
		dynamoDBClient = new DynamoDBClient(DynamoDBMapperModule.provideDynamoDBMapper());
		objectMapper = new ObjectMapper();
		bankSimulator = new BankSimulator();
	}

  @SuppressWarnings("unchecked")
  @Override
  public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		logger = context.getLogger();
		APIGatewayV2HTTPResponse response;

		try {
			MakePaymentInput paymentInput = objectMapper.readValue(event.getBody(), MakePaymentInput.class);

			logger.log("input: " + paymentInput);

			MakePaymentResponse paymentResponse = processMakePayment(paymentInput);

			objectMapper.setSerializationInclusion(NON_NULL);
			String jsonResponse = objectMapper.writeValueAsString(paymentResponse);

			// The reason why I choose to always return Ok response with failure codes, is because I assume there should be
			// another layer for the authorisation of the merchant client to use the merchantId.
			// So I assumed that these http status codes are reserved for that use case.
			response = ok(jsonResponse);
		} catch (JsonProcessingException e) {
			response = error("Json input is malformed, please check docs", 400);
		}

		return response;
	}

	MakePaymentResponse processMakePayment(MakePaymentInput paymentInput) {
		MerchantPayment existingPayment = dynamoDBClient.getMerchantPayment(paymentInput.getPaymentId());
		if (isPaymentExpiredOrNonExist(existingPayment)) {
			return new MakePaymentResponse(PaymentStatus.NotFound.name(), 404, "PaymentId is expired or not found");

		} else if (!existingPayment.getMerchantId().equals(paymentInput.getMerchantId())) {
			return new MakePaymentResponse(PaymentStatus.NotAuthorised.name(), 401, "This merchant doesn't have access to this payment");

		} else if (isPaymentSettled(existingPayment)) {
			return new MakePaymentResponse(existingPayment.getStatus(), null, null);

		} else {
			MerchantPayment paymentToBeSaved = MerchantPayment.builder()
				.withMakePaymentInput(paymentInput)
				.withCreationTimestampSeconds(existingPayment.getCreationTimestampSeconds())
				.withVersion(existingPayment.getVersion())
				.build();

			//TODO: If the bankSimulator.makePayment is idempotent (e.g on paymentId), add retries based on status codes.
			PaymentStatus paymentStatus = bankSimulator.makePayment(paymentToBeSaved);

			paymentToBeSaved.setStatus(paymentStatus.name());

			saveToDynamo(paymentToBeSaved);

			return new MakePaymentResponse(paymentStatus.name(), null , null);
		}
	}

	private boolean isPaymentSettled(MerchantPayment existingPayment) {
		return !existingPayment.getStatus().equals(PaymentStatus.Created.name());
	}

	private boolean isPaymentExpiredOrNonExist(MerchantPayment existingPayment) {
		return existingPayment == null ||
			(existingPayment.getExpiryTimestampSeconds() != null &&
				Instant.ofEpochSecond(existingPayment.getExpiryTimestampSeconds()).compareTo(Instant.now()) < 0);
	}

	private void saveToDynamo(MerchantPayment payment) {
		try {
			dynamoDBClient.saveMakePayment(payment);
		} catch (Exception e) {
			//TODO:
			// There will be an inconsistent state in this case between the bank status and our status. See README for other options.
			// - Add retries with a strategy (e.g exponential back offs) with timeout when saving to DDB (based on Exceptions).
			// - Emit a metric and alarm on it, Push to DLQ for async retries.
			logger.log("Failed saving to DDB for paymentId: " + payment.getPaymentId());
		}
	}

}