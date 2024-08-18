package com.task11;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class TablesPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    private final String TABLES = System.getenv("NAME_TABLE_TABLES");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map bodyMap = new ObjectMapper().readValue(request.getBody(), Map.class);

            Object id = bodyMap.get("id");
            Integer idInt = (Integer) id;
            Object number = bodyMap.get("number");

            Integer numberInt = (Integer) number;

            Object places = bodyMap.get("places");

            Integer placesInt = (Integer) places;

            Object isVip = bodyMap.get("isVip");

            boolean isVipBool = isVip.equals("true");

            Object minOrder = bodyMap.get("minOrder");

            Integer minOrderInt = (Integer) minOrder;

            Item item = new Item().withPrimaryKey("id", idInt)
                    .withInt("number", numberInt)
                    .withInt("places", placesInt)
                    .withBoolean("isVip", isVipBool)
                    .withInt("minOrder", minOrderInt);

            Table table = dynamoDB.getTable(TABLES);

            table.putItem(item);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{ \"id\": " + bodyMap.get("id") + " }");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}
