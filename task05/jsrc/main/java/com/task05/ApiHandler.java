package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler", roleName = "api_handler-role", isPublishVersion = false, logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED)
@DynamoDbTriggerEventSource(targetTable = "Events", batchSize = 10)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String DYNAMODB_TABLE_NAME = "Events" ;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private final com.amazonaws.services.dynamodbv2.document.DynamoDB dynamoDB = new com.amazonaws.services.dynamodbv2.document.DynamoDB(client);
    private final Table table = dynamoDB.getTable(DYNAMODB_TABLE_NAME);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
        try {
            Map<String, Object> responseBody = new HashMap<>();
            String id = UUID.randomUUID().toString();
            Integer principalId = (Integer) request.get("principalId");
            String createdAt = ISODateTimeFormat.dateTimeNoMillis().toString();
            Map<String, String> content = (Map<String, String>) request.get("content");
            Map<String,Map<String,String>> body = new HashMap<>();
            body.put("content", content);
            Item item = new Item().withPrimaryKey("id", id).withInt("principalId", principalId)
                    .withString("createdAt", createdAt).withMap("body", body);
            table.putItem(item);

            responseBody.put("statusCode", 201);
            responseBody.put("event", objectMapper.convertValue(item.asMap(), Map.class));
            return responseBody;

        } catch (Exception e) {
            context.getLogger().log("Error saving event: " + e.getMessage());
            return Map.of("statusCode", 500, "error", "Unable to save event");
        }
    }
}
