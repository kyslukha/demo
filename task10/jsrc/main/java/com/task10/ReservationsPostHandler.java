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

import java.util.Map;
import java.util.UUID;

public class ReservationsPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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

}

