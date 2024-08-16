package com.task10;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class TablesPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    private final String TABLES = System.getenv("NAME_TABLE_TABLES");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode body = objectMapper.readTree(request.getBody());

            int id = body.get("id").asInt();
            int number = body.get("number").asInt();
            int places = body.get("places").asInt();
            boolean isVip = body.get("isVip").asBoolean();
            int minOrder = body.has("minOrder") ? body.get("minOrder").asInt() : 0;

            Table table = dynamoDB.getTable(TABLES);

            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("number", number);
            item.put("places", places);
            item.put("isVip", isVip);
            if (minOrder > 0) {
                item.put("minOrder", minOrder);
            }
            table.putItem(new Item().withMap(String.valueOf(id), item));

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{ \"id\": " + id + " }");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}
