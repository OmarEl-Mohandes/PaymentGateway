package com.org.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.org.dynamodb.DynamoDBClient;
import com.org.payments.MerchantPayment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreatePaymentHandlerTest {
    private static DynamoDB dynamoDB;
    private static DynamoDBMapper mapper;
    private static CreatePaymentHandler handler;

    @BeforeAll
    public static void setUpClass() {
        System.setProperty("sqlite4java.library.path", "native-libs");

        AmazonDynamoDBLocal amazonDynamoDBLocal = DynamoDBEmbedded.create();
        AmazonDynamoDB client = amazonDynamoDBLocal.amazonDynamoDB();
        dynamoDB = new DynamoDB(client);
        String tableName = "MerchantPaymentTestTable";

        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
            .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
            .build();
        mapper = new DynamoDBMapper(client, mapperConfig);

        CreateTableRequest request = mapper.generateCreateTableRequest(MerchantPayment.class);
        request.withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));
        dynamoDB.createTable(request);

        handler = new CreatePaymentHandler(new DynamoDBClient(mapper));
    }

    @Test
    public void test() {
        CreatePaymentInput input = new CreatePaymentInput();

        String merchantId = "TestMerchantId_1";
        input.setMerchantId(merchantId);
        CreatePaymentResponse payment = handler.createPayment(input);
        MerchantPayment storedMerchantPayment = mapper.load(MerchantPayment.builder().withPaymentId(payment.getPaymentId()).build());

        assertEquals(payment.getPaymentId(), storedMerchantPayment.getPaymentId());
        assertEquals(payment.getStatus(), storedMerchantPayment.getStatus());
        assertEquals(merchantId, storedMerchantPayment.getMerchantId());
    }



 }
