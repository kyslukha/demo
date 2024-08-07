package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler", roleName = "api_handler-role", isPublishVersion = false, logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED)
@DynamoDbTriggerEventSource(targetTable = "Events", batchSize = 10)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String DYNAMODB_TABLE_NAME = "Events" ;
    private final com.amazonaws.services.dynamodbv2.document.DynamoDB dynamoDB;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiHandler(DynamoDB dynamoDB) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        this.dynamoDB = new com.amazonaws.services.dynamodbv2.document.DynamoDB(client);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            Map<String, Object> eventData = objectMapper.readValue(apiGatewayProxyRequestEvent.getBody(), Map.class);
            Table table = dynamoDB.getTable(DYNAMODB_TABLE_NAME);
            String id = (String) eventData.get("id");
            Integer principalId = (Integer) eventData.get("principalId");
            String createdAt = (String) eventData.get("createdAt");
            Map<String, Object> body = (Map<String, Object>) eventData.get("body");

            if (!isValidUUID(id)) {
                return createErrorResponse("Invalid UUID format for id");
            }
            if (principalId == null) {
                return createErrorResponse("principalId must be an integer");
            }
            if (!isValidISO8601Date(createdAt)) {
                return createErrorResponse("Invalid ISO 8601 date format for createdAt");
            }
            if (body == null || !(body instanceof Map)) {
                return createErrorResponse("body must be a map");
            }


            Item item = new Item().withPrimaryKey("id", id).withInt("principalId", principalId).withString("createdAt", createdAt).withMap("body", body);
            table.putItem(item);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("statusCode", 201);
            responseBody.put("event", eventData);
            return new APIGatewayProxyResponseEvent().withStatusCode(201).withBody(objectMapper.writeValueAsString(responseBody));

        } catch (Exception e) {
            context.getLogger().log("Error saving event: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("{\"message\": \"Error saving event\"}");
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(String message) {
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", message);
        try {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(objectMapper.writeValueAsString(responseBody));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("{\"message\": \"Internal server error\"}");
        }
    }

    private boolean isValidUUID(String id) {
        return UUID.fromString(id).toString().equals(id);
    }

    private boolean isValidISO8601Date(String dateStr) {
        try {
            ZonedDateTime.parse(dateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
