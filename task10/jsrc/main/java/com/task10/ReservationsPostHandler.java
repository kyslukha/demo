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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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

            LambdaLogger logger = context.getLogger();
            logger.log("WELCOME post RESERVATIONS");

            Object tableNumberObj = requestBody.get("tableNumber");
            Object clientNameObj = requestBody.get("clientName");
            Object phoneNumberObj = requestBody.get("phoneNumber");
            Object dateObj = requestBody.get("date");
            Object slotTimeStartObj = requestBody.get("slotTimeStart");
            Object slotTimeEndObj = requestBody.get("slotTimeEnd");
            logger.log("create objects of  " + tableNumberObj + " " +
                    clientNameObj + " " + phoneNumberObj + " " + dateObj + " " +
                    slotTimeStartObj + " " + slotTimeEndObj);

            Integer tableNumber = (Integer) tableNumberObj;
            String clientName = (String) clientNameObj;
            String phoneNumber = (String) phoneNumberObj;
            String date = (String) dateObj;
            String slotTimeStart = (String) slotTimeStartObj;
            String slotTimeEnd = (String) slotTimeEndObj;

            if (tableNumber == null || clientName == null || phoneNumber == null || date == null ||
                    slotTimeStart == null || slotTimeEnd == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Missing or invalid parameters\"}");
            }
            logger.log("begin validation table");

            if (!tableExists(tableNumber)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Table does not exist\"}");
            }

logger.log("begin conflict reservation method");

            if (conflictingReservationExists(tableNumber, date, slotTimeStart, slotTimeEnd, logger)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Reservation overlaps with an existing reservation\"}");
            }


            String reservationId = UUID.randomUUID().toString();
            logger.log("resevationId " + reservationId);
            Item newReservation = new Item()
                    .withPrimaryKey("id", reservationId)
                    .withInt("tableNumber", tableNumber)
                    .withString("clientName", clientName)
                    .withString("phoneNumber", phoneNumber)
                    .withString("date", date)
                    .withString("slotTimeStart", slotTimeStart)
                    .withString("slotTimeEnd", slotTimeEnd);
            logger.log("item ");

            Table reservationTable = dynamoDB.getTable(RESERVATION_TABLE);
            logger.log("table " + reservationTable.getTableName());
            reservationTable.putItem(newReservation);
            logger.log("putItem");

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

    private boolean conflictingReservationExists(Integer tableNumber, String date, String slotTimeStart, String slotTimeEnd, LambdaLogger logger) {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        logger.log("first map");
        expressionAttributeNames.put("#tableNumber", "tableNumber");
        expressionAttributeNames.put("#date", "date");
        expressionAttributeNames.put("#slotTimeStart", "slotTimeStart");
        expressionAttributeNames.put("#slotTimeEnd", "slotTimeEnd");
        logger.log("pull first map");

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        logger.log("second map");
        expressionAttributeValues.put(":tableNumber", new AttributeValue().withN(String.valueOf(tableNumber)));
        logger.log("put tableNumber");
        expressionAttributeValues.put(":date", new AttributeValue().withS(date));
        expressionAttributeValues.put(":slotTimeStart", new AttributeValue().withS(slotTimeStart));
        expressionAttributeValues.put(":slotTimeEnd", new AttributeValue().withS(slotTimeEnd));
        logger.log("pull second map");

        String filterExpression = "#tableNumber = :tableNumber AND #date = :date AND " +
                "((#slotTimeStart < :slotTimeEnd AND :slotTimeStart < #slotTimeEnd) " +
                "OR (#slotTimeStart > :slotTimeStart AND :slotTimeEnd > #slotTimeStart))";
        logger.log(" create filter " + filterExpression);

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(RESERVATION_TABLE)
                .withFilterExpression(filterExpression)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withExpressionAttributeValues(expressionAttributeValues);
        logger.log("request");

        QueryResult result = amazonDynamoDB.query(queryRequest);
        logger.log("result" + !result.getItems().isEmpty());

        return !result.getItems().isEmpty();
    }
}
