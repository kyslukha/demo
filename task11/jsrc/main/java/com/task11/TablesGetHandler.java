package com.task11;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
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

public class TablesGetHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient client = DynamoDbClient.builder().build();


    private final String TABLES = System.getenv("NAME_TABLE_TABLES");


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        ScanResponse scanResponse = client.scan(ScanRequest.builder().tableName(TABLES).build());

        List<Map<String, Object>> tables = new ArrayList<>();


        for (Map<String, AttributeValue> item : scanResponse.items()) {
            Map<String, Object> table = new HashMap<>();
            table.put("id", Integer.parseInt(item.get("id").n()));
            table.put("number", Integer.parseInt(item.get("number").n()));
            table.put("places", Integer.parseInt(item.get("places").n()));
            table.put("isVip", item.get("isVip").bool());
            if (item.containsKey("minOrder")) {
                table.put("minOrder", Integer.parseInt(item.get("minOrder").n()));
            }

            tables.add(table);
        }

        Map<String, List<Map<String, Object>>> responseBody = Map.of("tables", tables);

        try {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(new ObjectMapper().writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}


