package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
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
    private final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
    private static final String RESERVATION_TABLE = System.getenv("NAME_TABLE_RESERVATIONS");
    private static final String TABLE_TABLE = System.getenv("NAME_TABLE_TABLES");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // Parsing the request body
            Map requestBody = new ObjectMapper().readValue(request.getBody(), Map.class);

            Integer tableNumber = (Integer) requestBody.get("tableNumber");
            String clientName = (String) requestBody.get("clientName");
            String phoneNumber = (String) requestBody.get("phoneNumber");
            String date = (String) requestBody.get("date");
            String slotTimeStart = (String) requestBody.get("slotTimeStart");
            String slotTimeEnd = (String) requestBody.get("slotTimeEnd");

            if (tableNumber == null || clientName == null || phoneNumber == null || date == null ||
                    slotTimeStart == null || slotTimeEnd == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Missing or invalid parameters\"}");
            }

            if (!tableExists(tableNumber)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Table does not exist\"}");
            }

            if (conflictingReservationExists(tableNumber, date, slotTimeStart, slotTimeEnd)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Reservation overlaps with an existing reservation\"}");
            }

            String reservationId = UUID.randomUUID().toString();
            Item newReservation = new Item()
                    .withPrimaryKey("id", reservationId)
                    .withInt("tableNumber", tableNumber)
                    .withString("clientName", clientName)
                    .withString("phoneNumber", phoneNumber)
                    .withString("date", date)
                    .withString("slotTimeStart", slotTimeStart)
                    .withString("slotTimeEnd", slotTimeEnd);

            Table reservationTable = dynamoDB.getTable(RESERVATION_TABLE);
            reservationTable.putItem(newReservation);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{\"reservationId\": \"" + reservationId + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private boolean tableExists(int tableNumber) {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#num", "number");

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":tableNumber", new AttributeValue().withN(String.valueOf(tableNumber)));

        com.amazonaws.services.dynamodbv2.model.ScanRequest scanRequest = new ScanRequest()
                .withTableName(TABLE_TABLE)
                .withFilterExpression("#num = :tableNumber")
                .withExpressionAttributeNames(expressionAttributeNames)
                .withExpressionAttributeValues(expressionAttributeValues);

        ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
        return !scanResult.getItems().isEmpty();
    }

    private boolean conflictingReservationExists(Integer tableNumber, String date, String slotTimeStart, String slotTimeEnd) {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#tableNumber", "tableNumber");
        expressionAttributeNames.put("#date", "date");
        expressionAttributeNames.put("#slotTimeStart", "slotTimeStart");
        expressionAttributeNames.put("#slotTimeEnd", "slotTimeEnd");

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":tableNumber", new AttributeValue().withN(tableNumber.toString()));
        expressionAttributeValues.put(":date", new AttributeValue().withS(date));
        expressionAttributeValues.put(":slotTimeStart", new AttributeValue().withS(slotTimeStart));
        expressionAttributeValues.put(":slotTimeEnd", new AttributeValue().withS(slotTimeEnd));

        String filterExpression = "#tableNumber = :tableNumber AND #date = :date AND " +
                "((#slotTimeStart < :slotTimeEnd AND :slotTimeStart < #slotTimeEnd) " +
                "OR (#slotTimeStart > :slotTimeStart AND :slotTimeEnd > #slotTimeStart))";

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(RESERVATION_TABLE)
                .withFilterExpression(filterExpression)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withExpressionAttributeValues(expressionAttributeValues);

        QueryResult result = amazonDynamoDB.query(queryRequest);

        return !result.getItems().isEmpty();
    }
}
