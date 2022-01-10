# Payment Gateway

This is a minimal project for a Payment Gateway, a stateless serverless application built on top of AWS.
It consists of AWS API Gateway, Lambda, DynamoDB NoSQL and built using [CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html). 

## Design 

![payment-gateway](https://user-images.githubusercontent.com/1239788/148842390-66093bfd-13cc-4396-9e95-85312914d9f4.jpeg)

### Workflow
I've used API Gateway to create three APIs, and each handled by a separate lambda function. I choose separate lambdas to maintain, provision, scale and deploy each use case independently.

To start a payment transaction you need to call `/create-payment` API to generate a new `payment-id` using your `merchant-id`. They will be stored in DDB 
with a creationTimestamp and status `Created`.
Once the `payment-id` is created, it's attached to the `merchant-id` and will expire in 10 minutes in case no payment 
has been made after. TTL is enabled on this table so items can get deleted if expired. 

Using the `payment-id` you can call `/make-payment` API with the required details to complete the payment. I choose to separate the two APIs to make the `/make-payment` API idempotent.

The API `/get-payment` can be called anytime to check the status of the payment.

### Data Store
I've used DynamoDB as a simple NoSQL store for payments. The hashkey for the item is `payment-id` so it can be retrieved quickly. I've used `version` to enable the optimistic locking 
on the table to make sure parallel retries on the same payments won't cause data inconsistency. 

We can create different GSIs (GlobalSecondaryIndex) 
depend on the use cases. For instance, we can create a GSI on `merhcantId` and sort key on `creationTimestamp` 
to get all the payments for merchant for a specific window.

### Bank Simulator
BankSimulator is a stubbed class to return different payment statuses based on the `amount` in the request. It's used only in `/make-payment` API.

### Areas Of Improvements 
This project is far from 'production ready' status, and I had to cut a lot of corners due to my available time. Nevertheless, here are some food for thought points:

- #### Security
    - The APIs don't have authentication/authorisation for the merchant clients. For instance, authenticating clients to use our APIs and authorising them 
      to use specific merchantIds.
    - The classification of data stored and passed around different components are **Critical**. It needs be encrypted at Rest and tokenizing the payment 
      details to pass a token around instead of the actual data.

- #### Scalability/Cost
   Although, AWS Lambda can handle high TPS (relatively), it's not recommended to use for latency nor cost sensitive use cases. Scaling up Lambdas 
   has proven to be more costly than e.g running ECS tasks on the long run.
   
- #### Availability and Consistency 
   - In `/make-payment`, the call to BankSimulator happens first then we store the result to DDB. However, when the call to DDB fails, we won't store the result. 
   That means these two calls are not atomically guaranteed. 
   If the payment is `Approved` we'll need to return it to the merchant and retry later to sync the correct status in our DDB. Another option is to rollback the 
   transaction to the bank and and return `Failed` to the client.
     
   - We need to introduce throttling based on merchantIds, however this might increase complexity to clients usage.
   
- #### Validation and Testing
    - Currently, the project is missing unit tests and integration tests. I've added some acceptance tests for some main scenarios, but it's far from done.
    - Introducing input format validation for each field.
    - Writing javadocs for each function API in my code, and proper API documentation.
   
- #### Monitoring and Observability
   A lot of metrics needs to be emitted specially in failure modes between the components. Big TODO around merchant alarms
   and dashboards.
 

## Usage

 - ### POST /create-payment
    
    **Purpose:** Use this API to create a new ```paymentId``` for the merchant.
    
    **Input:** POST Body contains `merchantId` in JSON format.

    **Output:** JSON output contains the following fields `paymentId`, `status`, `creationTimestampSeconds

    **Example Usage:**
    
    ```
    curl -X POST -d '{ "merchantId" : "test-merchant" }'  https://jn2zxhxbfg.execute-api.eu-west-2.amazonaws.com/prod/create-payment
    
    {"paymentId":"c560a405-f533-4939-ac46-a503c67c8bea","status":"Created","creationTimestampSeconds":1641845321}
    ```
    **Notes** After 10 minutes the `payment-id` will expire if not used, and will be deleted from the DDB. 
    

 - ### POST /make-payment

   **Purpose:** Use this API to complete the payment using the ```paymentId``` and all expected card information.

   **Input:** POST Body contains the following fields (all fields are required):
    ```
        {
            "paymentId" : "59c7a3dd-a035-41c6-97ee-d930cd340ce2",
            "merchantId" : "test-merchant",
            "cardNumber" : "1234432198761543",
            "expiryYear" : 2000,
            "expiryMonth" : 12,
            "currency" : "GBP",
            "amount" : 50,
            "cardName" : "OmarElmohandes",
            "billingAddress" : "77Merenda",
            "cvv" : "232"
        }
    ```

   **Output:** JSON output contains the following fields `status`, `failCode` (optional), `failReason` (optional)
    ```
        {
            "status":"Accepted",
            "failCode"   // Optional in case of error.
            "failReason" // Optional in case of error.
        }    
    ```

   **Example Usage:**

    ```
    curl -X POST https://jn2zxhxbfg.execute-api.eu-west-2.amazonaws.com/prod/make-payment -d '{"paymentId":"59c7a3dd-a035-41c6-97ee-d930cd340ce2","merchantId":"test-merchant","cardNumber":"1234432198761543","expiryYear":2000,"expiryMonth":12,"currency":"GBP","amount":50,"cardName":"OmarElmohandes","billingAddress":"77Merenda","cvv":"232"}'
    
    ```

    **Notes:** To simulate different payment statuses you can pass these values to `amount` in the request. 
    
   | Amount  | Status  | Comments |
   | --- | --- | ---|
   | 1 | Declined |
   | 2 | InsufficientFunds |
   | 24 | Pending | Make another request with the same amount to be Accepted
   | any other amount | Accepted |

 - ### GET /get-payment

   **Purpose:** The purpose of this API is to return the payment details for the ```paymentId``` and ```merchantId```.

   **Input:** `paymentId` and `merchantId`

   **Output:** JSON output contains the following fields:
    ```
       {
           "cardNumber":"************3423",
           "expiryYear":2000,
           "expiryMonth":12,
           "currency":"GBP",
           "amount":50,
           "status":"Accepted",
           "cardName":"OmarElmohandes",
           "billingAddress":"77Merenda",
           "creationTimestampSeconds":1641821593,
           "failCode"   // Optional in case of error.
           "failReason" // Optional in case of error.
       }
    ```
   
   **Example Usage:**

    ```
    curl -G -d "merchantId=test-merchant" -d "paymentId=59c7a3dd-a035-41c6-97ee-d930cd340ce2" https://jn2zxhxbfg.execute-api.eu-west-2.amazonaws.com/prod/get-payment    
    ```

## Build From Source
- Clone the package.
```
  $ git clone git@github.com:OmarEl-Mohandes/PaymentGateway.git
  $ cd PaymentGateway
```
- Build using mvn and will also run acceptance tests.
```
  $ mvn package
```
- Make sure you install `cdk` (`npm install -g aws-cdk`) and aws-cli and configured your AWS credentials using `aws configure`.

- Configure your env variables for AWS:
```
  $ export CDK_DEFAULT_ACCOUNT=YOUR_AWS_ACCOUNT_ID
```

- Build using CDK and deploy to your AWS account
```
  $ cdk synth
  $ cdk bootstrap
  $ cdk deploy
```

