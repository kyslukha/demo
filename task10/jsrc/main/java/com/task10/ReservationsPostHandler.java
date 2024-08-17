package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
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
                        .withBody("There was an error in the request.");
            }

            if (conflictingReservationExists(tableNumber, date, slotTimeStart, slotTimeEnd)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("There was an error in the request.");
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

    private boolean conflictingReservationExists(Integer tableNumber, String date,
                                                 String slotTimeStart, String slotTimeEnd) {
        Table reservationsTable = dynamoDB.getTable(RESERVATIONS);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#tableNumber = :tableNumber and #date = :date")
                .withFilterExpression("#slotTimeStart < :slotTimeEnd AND #slotTimeEnd > :slotTimeStart")
                .withNameMap(new NameMap()
                        .with("#tableNumber", "tableNumber")
                        .with("#date", "date")
                        .with("#slotTimeStart", "slotTimeStart")
                        .with("#slotTimeEnd", "slotTimeEnd"))
                .withValueMap(new ValueMap()
                        .withInt(":tableNumber", tableNumber)
                        .withString(":date", date)
                        .withString(":slotTimeStart", slotTimeStart)
                        .withString(":slotTimeEnd", slotTimeEnd));

        ItemCollection<QueryOutcome> items = reservationsTable.query(spec);

        return items.iterator().hasNext();
    }

}

