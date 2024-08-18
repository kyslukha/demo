package com.task11;


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
        LambdaLogger logger = context.getLogger();
        logger.log("WELCOME TO RESERVATIONS GET");
        List<Map<String, Object>> reservations = new ArrayList<>();
        for (Map<String, AttributeValue> item : scanResponse.items()) {
            Map<String, Object> table = new HashMap<>();
            table.put("tableNumber", Integer.parseInt(item.get("tableNumber").n()));
            table.put("clientName", (item.get("clientName").s()));
            table.put("phoneNumber", String.valueOf(item.get("phoneNumber").s()));
            table.put("date", String.valueOf(item.get("date").s()));
            table.put("slotTimeStart", String.valueOf(item.get("slotTimeStart").s()));
            table.put("slotTimeEnd", String.valueOf(item.get("slotTimeEnd").s()));
            logger.log("create list of reservation");
            reservations.add(table);
        }
        logger.log("create list of maps");

        Map<String, List<Map<String, Object>>> responseBody = Map.of("reservations", reservations);
        logger.log("create full map");

        try {
            return new APIGatewayProxyResponseEvent().withStatusCode(200)
                    .withBody(new ObjectMapper().writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}

