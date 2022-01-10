package com.org.infra;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.Method;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.HashMap;

public class PaymentGatewayCdkStack extends Stack {

    public PaymentGatewayCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public PaymentGatewayCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        String tableName = "MerchantPayment";
        //Create a DynamoDB Table
        TableProps tableProps = TableProps.builder()
            .partitionKey(Attribute.builder()
                .name("paymentId")
                .type(AttributeType.STRING)
                .build())
            .readCapacity(10)
            .writeCapacity(10)
            .removalPolicy(RemovalPolicy.DESTROY)
            .timeToLiveAttribute("expiryTimestampSeconds")
            .tableName(tableName)
            .build();
        Table merchantPaymentTable = new Table(this, tableName, tableProps);

        //Global Secondary Index
        merchantPaymentTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
            .indexName("MerchantGSI")
            .projectionType(ProjectionType.ALL)
            .partitionKey(Attribute.builder()
                .name("merchantId")
                .type(AttributeType.STRING)
                .build())
            .sortKey(Attribute.builder()
                .name("creationTimestampSeconds")
                .type(AttributeType.NUMBER)
                .build())
            .build());

        //Lambda Environment Variables to pass to the Lambdas
        HashMap<String, String> env = new HashMap<String, String>();
        env.put("merchantPaymentTableName", merchantPaymentTable.getTableName());

        //Lambda setup
        Function createPaymentLambda = Function.Builder.create(this, "CreatePaymentHandler")
            .runtime(Runtime.JAVA_11)
            .functionName("CreatePaymentHandler")
            .timeout(Duration.minutes(1))
            .memorySize(500)
            .environment(env)
            .code(Code.fromAsset("target/PaymentGateway-0.1.jar"))
            .handler("com.org.lambda.CreatePaymentHandler::handleRequest")
            .build();
        merchantPaymentTable.grantFullAccess(createPaymentLambda);

        Function makePaymentLambda = Function.Builder.create(this, "MakePaymentHandler")
            .runtime(Runtime.JAVA_11)
            .functionName("MakePaymentHandler")
            .timeout(Duration.minutes(1))
            .memorySize(500)
            .environment(env)
            .code(Code.fromAsset("target/PaymentGateway-0.1.jar"))
            .handler("com.org.lambda.MakePaymentHandler::handleRequest")
            .build();
        merchantPaymentTable.grantFullAccess(makePaymentLambda);

        Function getPaymentLambda = Function.Builder.create(this, "GetPaymentHandler")
            .runtime(Runtime.JAVA_11)
            .functionName("GetPaymentHandler")
            .timeout(Duration.minutes(1))
            .memorySize(500)
            .environment(env)
            .code(Code.fromAsset("target/PaymentGateway-0.1.jar"))
            .handler("com.org.lambda.GetPaymentHandler::handleRequest")
            .build();
        merchantPaymentTable.grantFullAccess(getPaymentLambda);

        // Allow lambdas to be called from API Gateway.
        RestApi api = RestApi.Builder.create(this, "Java CDK")
            .restApiName("Java CDK").
                description("Java CDK")
            .build();

        LambdaIntegration createPaymentIntegration = LambdaIntegration.Builder.create(createPaymentLambda)
            .requestTemplates(new HashMap<String, String>() {{
                put("application/json", "{ \"statusCode\": \"200\" }");
            }}).build();
        LambdaIntegration makePaymentIntegration = LambdaIntegration.Builder.create(makePaymentLambda)
            .requestTemplates(new HashMap<String, String>() {{
                put("application/json", "{ \"statusCode\": \"200\" }");
            }}).build();
        LambdaIntegration getPaymentIntegration = LambdaIntegration.Builder.create(getPaymentLambda)
            .requestTemplates(new HashMap<String, String>() {{
                put("application/json", "{ \"statusCode\": \"200\" }");
            }}).build();

        Resource createPaymentResource = api.getRoot().addResource("create-payment");
        Resource makePaymentResource = api.getRoot().addResource("make-payment");
        Resource getPaymentResource = api.getRoot().addResource("get-payment");
        Method createPaymentMethod = createPaymentResource.addMethod("POST", createPaymentIntegration);
        Method makePaymentMethod = makePaymentResource.addMethod("POST", makePaymentIntegration);
        Method getPaymentMethod = getPaymentResource.addMethod("GET", getPaymentIntegration);

        CfnOutput.Builder.create(this, "RegionOutput")
            .description("")
            .value("Region:" + this.getRegion())
            .build();

        CfnOutput.Builder.create(this, "DynamoDBPaymentTable")
            .description("")
            .value("DynamoDBTable:" + merchantPaymentTable.getTableName())
            .build();

        String urlPrefix = api.getUrl().substring(0, api.getUrl().length() - 1);

        CfnOutput.Builder.create(this, "CreatePaymentLambda")
            .description("")
            .value("CreatePayment Lambda:" + urlPrefix + createPaymentMethod.getResource().getPath())
            .build();

        CfnOutput.Builder.create(this, "MakePaymentLambda")
            .description("")
            .value("MakePayment Lambda:" + urlPrefix + makePaymentMethod.getResource().getPath())
            .build();

        CfnOutput.Builder.create(this, "GetPaymentLambda")
            .description("")
            .value("GetPayment Lambda:" + urlPrefix + getPaymentMethod.getResource().getPath())
            .build();
    }
}