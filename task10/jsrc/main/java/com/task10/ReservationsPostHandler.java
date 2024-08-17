package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReservationsPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient client = DynamoDbClient.builder().build();

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    private final String RESERVATIONS = System.getenv("NAME_TABLE_RESERVATIONS");





    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map bodyMap = new ObjectMapper().readValue(request.getBody(), Map.class);


            Object tableNumber = bodyMap.get("tableNumber");
            Object clientName = bodyMap.get("clientName");
            Object phoneNumber = bodyMap.get("phoneNumber");
            Object date = bodyMap.get("date");
            Object slotTimeStart = bodyMap.get("slotTimeStart");
            Object slotTimeEnd = bodyMap.get("slotTimeEnd");


            Integer tableNumberInt = (Integer) tableNumber;
            String clientNameStr = (String) clientName;
            String phoneNumberStr = (String) phoneNumber;
            String dateStr = (String) date;
            String slotTimeStartStr = (String) slotTimeStart;
            String slotTimeEndStr = (String) slotTimeEnd;

            if (!isValidInput(tableNumberInt, clientNameStr, phoneNumberStr, dateStr, slotTimeStartStr, slotTimeEndStr)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("There was an error in the request.");
            }

            if (hasConflictingReservation(tableNumberInt, dateStr, slotTimeStartStr, slotTimeEndStr)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("There was an error in the request.");
            }


            String reservationId = UUID.randomUUID().toString();

            Item item = new Item().withPrimaryKey("id", reservationId)
                    .withInt("tableNumber", tableNumberInt)
                    .withString("clientName", clientNameStr)
                    .withString("phoneNumber", phoneNumberStr)
                    .withString("date", dateStr)
                    .withString("slotTimeStart", slotTimeStartStr)
                    .withString("slotTimeEnd", slotTimeEndStr);


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

    private boolean hasConflictingReservation(Integer tableNumberInt, String dateStr, String slotTimeStartStr, String slotTimeEndStr) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":tableNumber", AttributeValue.builder().n(String.valueOf(tableNumberInt)).build());
        expressionValues.put(":date", AttributeValue.builder().s(dateStr).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(RESERVATIONS)
                .keyConditionExpression("tableNumber = :tableNumber and date = :date")
                .expressionAttributeValues(expressionValues)
                .build();

        QueryResponse queryResponse = client.query(queryRequest);

        LocalTime start = LocalTime.parse(slotTimeStartStr, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime end = LocalTime.parse(slotTimeEndStr, DateTimeFormatter.ofPattern("HH:mm"));

        for (Map<String, AttributeValue> item : queryResponse.items()) {
            LocalTime existingStart = LocalTime.parse(item.get("slotTimeStart").s(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime existingEnd = LocalTime.parse(item.get("slotTimeEnd").s(), DateTimeFormatter.ofPattern("HH:mm"));

            if (start.isBefore(existingEnd) && end.isAfter(existingStart)) {
                return true;
            }
        }
        return false;
    }


    private boolean isValidInput(Integer tableNumberInt, String clientNameStr, String phoneNumberStr, String dateStr, String slotTimeStartStr, String slotTimeEndStr) {
        if (tableNumberInt < 1) return false;

        if (clientNameStr == null || clientNameStr.isEmpty() || phoneNumberStr == null || phoneNumberStr.isEmpty()) return false;

        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return false;
        }
        try {
            LocalTime.parse(slotTimeStartStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime.parse(slotTimeEndStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}

