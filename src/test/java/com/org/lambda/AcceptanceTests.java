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
import com.org.payments.PaymentStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AcceptanceTests {

    private static DynamoDB dynamoDB;
    private static DynamoDBMapper mapper;
    private static CreatePaymentHandler createPaymentHandler;
    private static GetPaymentHandler getPaymentHandler;
    private static MakePaymentHandler makePaymentHandler;
    private final String merchantId = "testMerchantId";

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

        createPaymentHandler = new CreatePaymentHandler(new DynamoDBClient(mapper));
        getPaymentHandler = new GetPaymentHandler(new DynamoDBClient(mapper));
        makePaymentHandler = new MakePaymentHandler(new DynamoDBClient(mapper));
    }

    @Test
    public void testCreatePaymentShouldStoreAndReturnPaymentId() {
        CreatePaymentInput input = new CreatePaymentInput(merchantId);

        CreatePaymentResponse payment = createPaymentHandler.createPayment(input);
        MerchantPayment storedMerchantPayment = mapper.load(MerchantPayment.class, payment.getPaymentId());

        assertEquals(payment.getPaymentId(), storedMerchantPayment.getPaymentId());
        assertEquals(PaymentStatus.Created.name(), storedMerchantPayment.getStatus());
        assertEquals(PaymentStatus.Created.name(), payment.getStatus());
        assertEquals(merchantId, storedMerchantPayment.getMerchantId());
    }

    @Test
    public void testCreatePaymentMultipleTimesShouldReturnDifferentPaymentIds() {
        Set<String> paymentIds = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            CreatePaymentInput input = new CreatePaymentInput(merchantId);
            CreatePaymentResponse payment = createPaymentHandler.createPayment(input);
            paymentIds.add(payment.getPaymentId());
        }

        assertEquals(10, paymentIds.size());
    }

    @Test
    public void testCreatePaymentThenGetPaymentShouldReturnSameData() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);

        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        GetPaymentInput getPaymentInput = new GetPaymentInput(merchantId, createPaymentResponse.getPaymentId());

        GetPaymentResponse getPaymentResponse = getPaymentHandler.getPayment(getPaymentInput);

        assertEquals(createPaymentResponse.getStatus(), getPaymentResponse.getStatus());
        assertEquals(createPaymentResponse.getCreationTimestampSeconds(), getPaymentResponse.getCreationTimestampSeconds());
    }

    @Test
    public void testGetPaymentWithWrongMerchantIdShouldReturnNotAuthorised() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);

        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        GetPaymentInput getPaymentInput = new GetPaymentInput("merchant2", createPaymentResponse.getPaymentId());

        GetPaymentResponse getPaymentResponse = getPaymentHandler.getPayment(getPaymentInput);

        assertEquals(401, getPaymentResponse.getFailCode());
        assertTrue(getPaymentResponse.getFailReason().length() > 0);
    }

    @Test
    public void testGetPaymentWithWrongPaymentIdShouldReturnNotFound() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);

        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        GetPaymentInput getPaymentInput = new GetPaymentInput(merchantId, "wrong-payment-id");

        GetPaymentResponse getPaymentResponse = getPaymentHandler.getPayment(getPaymentInput);

        assertEquals(404, getPaymentResponse.getFailCode());
        assertTrue(getPaymentResponse.getFailReason().length() > 0);
    }

    @Test
    public void testMakePaymentShouldStoreDataInDynamoDB() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);
        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        MakePaymentInput makePaymentInput = createMakePaymentInput();
        makePaymentInput.setPaymentId(createPaymentResponse.getPaymentId());

        MakePaymentResponse makePaymentResponse = makePaymentHandler.processMakePayment(makePaymentInput);

        MerchantPayment storedPayment = mapper.load(MerchantPayment.class, createPaymentResponse.getPaymentId());

        // Assert stored data in DDB.
        assertEquals(makePaymentInput.getPaymentId(), storedPayment.getPaymentId());
        assertEquals(makePaymentInput.getMerchantId(), storedPayment.getMerchantId());
        assertEquals(makePaymentInput.getCardName(), storedPayment.getCardName());
        assertEquals(makePaymentInput.getCardNumber(), storedPayment.getCardNumber());
        assertEquals(makePaymentInput.getExpiryYear(), storedPayment.getExpiryYear());
        assertEquals(makePaymentInput.getExpiryMonth(), storedPayment.getExpiryMonth());
        assertEquals(makePaymentInput.getCurrency(), storedPayment.getCurrency());
        assertEquals(makePaymentInput.getAmount(), storedPayment.getAmount());
        assertEquals(makePaymentInput.getBillingAddress(), storedPayment.getBillingAddress());

        // Assert makePayment response.
        assertEquals(storedPayment.getStatus(), makePaymentResponse.getPaymentStatus());
        assertNull(makePaymentResponse.getFailCode());
        assertNull(makePaymentResponse.getFailReason());
    }

    @Test
    public void testMakePaymentThenGetPaymentShouldReturnStoredDataAndMaskedCardNumber() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);
        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        MakePaymentInput makePaymentInput = createMakePaymentInput();
        makePaymentInput.setPaymentId(createPaymentResponse.getPaymentId());

        makePaymentHandler.processMakePayment(makePaymentInput);

        GetPaymentInput getPaymentInput = new GetPaymentInput(merchantId, createPaymentResponse.getPaymentId());
        GetPaymentResponse getPaymentResponse = getPaymentHandler.getPayment(getPaymentInput);

        MerchantPayment storedPayment = mapper.load(MerchantPayment.class, createPaymentResponse.getPaymentId());

        // Assert stored data vs get response.

        assertEquals(storedPayment.getCardName(), getPaymentResponse.getCardName());

        String expectedCardNumber = "*".repeat(12) + storedPayment.getCardNumber().substring(12);
        assertEquals(expectedCardNumber, getPaymentResponse.getCardNumber());

        assertEquals(storedPayment.getExpiryYear(), getPaymentResponse.getExpiryYear());
        assertEquals(storedPayment.getExpiryMonth(), getPaymentResponse.getExpiryMonth());
        assertEquals(storedPayment.getCurrency(), getPaymentResponse.getCurrency());
        assertEquals(storedPayment.getAmount(), getPaymentResponse.getAmount());
        assertEquals(storedPayment.getBillingAddress(), getPaymentResponse.getBillingAddress());
        assertEquals(storedPayment.getStatus(), getPaymentResponse.getStatus());
        assertEquals(storedPayment.getCreationTimestampSeconds(), getPaymentResponse.getCreationTimestampSeconds());
        assertNull(getPaymentResponse.getFailCode());
        assertNull(getPaymentResponse.getFailReason());
    }

    @Test
    public void testThatMakePaymentIsIdempotent() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);
        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        MakePaymentInput makePaymentInput = createMakePaymentInput();
        makePaymentInput.setPaymentId(createPaymentResponse.getPaymentId());

        for (int i = 0; i < 100; i++) {
            makePaymentHandler.processMakePayment(makePaymentInput);
        }

        MerchantPayment storedPayment = mapper.load(MerchantPayment.class, createPaymentResponse.getPaymentId());

        // version is incremented by DDB on every successful write.
        // so we expect it to be 2, due to CreatePayment and MakePayment.
        assertEquals(2, storedPayment.getVersion());
    }

    @Test
    public void testMakePaymentToBeDeclined() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);
        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        MakePaymentInput makePaymentInput = createMakePaymentInput();
        makePaymentInput.setPaymentId(createPaymentResponse.getPaymentId());
        makePaymentInput.setAmount(1);

        MakePaymentResponse makePaymentResponse = makePaymentHandler.processMakePayment(makePaymentInput);
        assertEquals(PaymentStatus.Declined.name(), makePaymentResponse.getPaymentStatus());
        assertNull(makePaymentResponse.getFailCode());
        assertNull(makePaymentResponse.getFailReason());
    }

    @Test
    public void testMakePaymentToBeInsufficientFunds() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);
        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        MakePaymentInput makePaymentInput = createMakePaymentInput();
        makePaymentInput.setPaymentId(createPaymentResponse.getPaymentId());
        makePaymentInput.setAmount(2);

        MakePaymentResponse makePaymentResponse = makePaymentHandler.processMakePayment(makePaymentInput);
        assertEquals(PaymentStatus.InsufficientFunds.name(), makePaymentResponse.getPaymentStatus());
        assertNull(makePaymentResponse.getFailCode());
        assertNull(makePaymentResponse.getFailReason());
    }


    @Test
    public void testMakePaymentToReturnPendingAndRetryThenSucceed() {
        CreatePaymentInput createPaymentInput = new CreatePaymentInput(merchantId);
        CreatePaymentResponse createPaymentResponse = createPaymentHandler.createPayment(createPaymentInput);

        MakePaymentInput makePaymentInput = createMakePaymentInput();
        makePaymentInput.setPaymentId(createPaymentResponse.getPaymentId());
        makePaymentInput.setAmount(24);

        MakePaymentResponse response1 = makePaymentHandler.processMakePayment(makePaymentInput);
        MakePaymentResponse response2 = makePaymentHandler.processMakePayment(makePaymentInput);
        assertEquals(PaymentStatus.Pending.name(), response1.getPaymentStatus());
        assertEquals(PaymentStatus.Accepted.name(), response2.getPaymentStatus());
        assertNull(response1.getFailCode());
        assertNull(response1.getFailReason());
    }

    private MakePaymentInput createMakePaymentInput() {
        return new MakePaymentInput("payment-id",
            merchantId,
            "testCardName",
            "223402020020200202",
            2023,
            11,
            "GBP",
            50,
            "23Hellenda",
            "020");
    }

}
