package com.task10;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationsGetHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient client = DynamoDbClient.builder().build();

    private final String RESERVATIONS = System.getenv("NAME_TABLE_RESERVATIONS");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        ScanResponse scanResponse = client.scan(ScanRequest.builder().tableName(RESERVATIONS).build());
        List<Map<String, Object>> tables = new ArrayList<>();
        for (Map<String, AttributeValue> item : scanResponse.items()) {
            Map<String, Object> table = new HashMap<>();
            table.put("tableNumber", Integer.parseInt(item.get("tableNumber").n()));
            table.put("clientName", item.get("number").toString());
            table.put("phoneNumber", item.get("phoneNumber").toString());
            table.put("date", item.get("date").toString());
            table.put("slotTimeStart", item.get("slotTimeStart").toString());
            table.put("slotTimeEnd", item.get("slotTimeEnd").toString());
            tables.add(table);
        }

        Map<String, List<Map<String, Object>>> responseBody = Map.of("reservations", tables);

        try {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(new ObjectMapper().writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}

