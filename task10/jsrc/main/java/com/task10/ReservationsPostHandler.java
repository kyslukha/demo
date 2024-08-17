package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReservationsPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
    private final String RESERVATIONS = System.getenv("NAME_TABLE_RESERVATIONS");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map bodyMap = new ObjectMapper().readValue(request.getBody(), Map.class);

            Integer tableNumber = (Integer) bodyMap.get("tableNumber");
            String clientName = (String) bodyMap.get("clientName");
            String phoneNumber = (String) bodyMap.get("phoneNumber");
            String date = (String) bodyMap.get("date");
            String slotTimeStart = (String) bodyMap.get("slotTimeStart");
            String slotTimeEnd = (String) bodyMap.get("slotTimeEnd");

            if (clientName == null || clientName.isEmpty() ||
                    phoneNumber == null || phoneNumber.isEmpty() ||
                    date == null || date.isEmpty() ||
                    slotTimeStart == null || slotTimeStart.isEmpty() ||
                    slotTimeEnd == null || slotTimeEnd.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Invalid data.");
            }

            if (conflictingReservationExists(tableNumber, date, slotTimeStart, slotTimeEnd)) {
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Conflicting reservation found.");
            }

            String reservationId = UUID.randomUUID().toString();
            Item item = new Item().withPrimaryKey("id", reservationId)
                    .withInt("tableNumber", tableNumber)
                    .withString("clientName", clientName)
                    .withString("phoneNumber", phoneNumber)
                    .withString("date", date)
                    .withString("slotTimeStart", slotTimeStart)
                    .withString("slotTimeEnd", slotTimeEnd);

            Table table = dynamoDB.getTable(RESERVATIONS);
            table.putItem(item);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{ \"reservationId\": \"" + reservationId + "\" }");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }

    private boolean conflictingReservationExists(Integer tableNumber, String date, String slotTimeStart, String slotTimeEnd) {
        try {
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#tableNumber", "tableNumber");
            expressionAttributeNames.put("#date", "date");

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":tableNumber", new AttributeValue().withN(tableNumber.toString()));
            expressionAttributeValues.put(":date", new AttributeValue().withS(date));
            expressionAttributeValues.put(":slotTimeStart", new AttributeValue().withS(slotTimeStart));
            expressionAttributeValues.put(":slotTimeEnd", new AttributeValue().withS(slotTimeEnd));

            String filterExpression = "slotTimeStart < :slotTimeEnd AND slotTimeEnd > :slotTimeStart";

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(RESERVATIONS)
                    .withKeyConditionExpression("#tableNumber = :tableNumber AND #date = :date")
                    .withFilterExpression(filterExpression)
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            QueryResult result = amazonDynamoDB.query(queryRequest);

            System.out.println("Query result: " + result.getItems());

            return !result.getItems().isEmpty();

        } catch (Exception e) {
            System.err.println("Error checking conflicting reservation: " + e.getMessage());
            return true;
        }
    }
}
