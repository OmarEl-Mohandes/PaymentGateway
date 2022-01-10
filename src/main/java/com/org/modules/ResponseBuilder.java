package com.org.modules;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.json.simple.JSONObject;

public class ResponseBuilder {

    public static APIGatewayV2HTTPResponse ok(String response) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(200)
            .withBody(response)
            .withIsBase64Encoded(false)
            .build();
    }

    public static APIGatewayV2HTTPResponse error(String errorMessage, int statusCode) {
        JSONObject jsonResponse = new JSONObject();

        jsonResponse.put("errorMessage", errorMessage);

        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(statusCode)
            .withBody(jsonResponse.toJSONString())
            .withIsBase64Encoded(false)
            .build();
    }
}
