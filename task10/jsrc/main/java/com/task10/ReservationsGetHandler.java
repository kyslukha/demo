package com.task10;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.ArrayList;
import java.util.List;

public class ReservationsGetHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    private final String RESERVATIONS = System.getenv("NAME_TABLE_RESERVATIONS");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Table table = dynamoDB.getTable(RESERVATIONS);
            ScanSpec scanSpec = new ScanSpec();
            LambdaLogger logger = context.getLogger();
            logger.log("GETreserv   table " + table.getTableName());

            List<Item> items = table.scan(scanSpec).getLastLowLevelResult().getItems();
            logger.log("GETreserv   create list of item");
            List<Object> reservationsList = new ArrayList<>();


            for (Item item : items) {
                reservationsList.add(item.asMap());
                logger.log("GETreserv   add to list " + reservationsList.toString());
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{ \"reservations\": " + reservationsList.toString() + " }");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}

