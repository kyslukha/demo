package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReservationsPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    private final String RESERVATIONS = System.getenv("NAME_TABLE_RESERVATIONS");


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode body = objectMapper.readTree(request.getBody());

            int tableNumber = body.get("tableNumber").asInt();
            String clientName = body.get("clientName").asText();
            Integer phoneNumber = Integer.valueOf(body.get("phoneNumber").asText());
            String date = body.get("date").asText();
            String slotTimeStart = body.get("slotTimeStart").asText();
            String slotTimeEnd = body.get("slotTimeEnd").asText();
            LambdaLogger logger = context.getLogger();
            logger.log("POST reserv data " + tableNumber + " " + clientName + " " + phoneNumber + " " + date + " " + slotTimeStart + " " + slotTimeEnd );

//            if (!isValidDate(date) || !isValidTime(slotTimeStart) || !isValidTime(slotTimeEnd)) {
//                logger.log("Invalid date or time format.");
//                return new APIGatewayProxyResponseEvent()
//                        .withStatusCode(400)
//                        .withBody("Invalid date or time format.");
//            }
            logger.log("Good validation//////////////");

            String reservationId = UUID.randomUUID().toString();
            logger.log("id////////////// " + reservationId);

            Table table = dynamoDB.getTable(RESERVATIONS);
            logger.log("table//////////// " + table.getTableName());

            Map<String, Object> item = new HashMap<>();
            item.put("reservationId", reservationId);
            item.put("tableNumber", tableNumber);
            item.put("clientName", clientName);
            item.put("phoneNumber", String.valueOf(phoneNumber));
            item.put("date", date);
            item.put("slotTimeStart", slotTimeStart);
            item.put("slotTimeEnd", slotTimeEnd);
            logger.log("create map//////////// " );


            table.putItem(new Item().withMap(reservationId, item));
            logger.log("put items  in map" );


            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{ \"reservationId\": \"" + reservationId + "\" }");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
//    private boolean isValidDate(String date) {
//        try {
//            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private boolean isValidTime(String time) {
//        try {
//            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

}

