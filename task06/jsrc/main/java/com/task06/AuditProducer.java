package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
        lambdaName = "audit_producer",
        roleName = "audit_producer-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 1)


@EnvironmentVariable(key = "TABLE_NAME", value = "${target_table}"
)


public class AuditProducer implements RequestHandler<DynamodbEvent, String> {
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDB = new DynamoDB(client);
    private final Table auditTable;

    private final String tableName;

    public AuditProducer() {
//        tableName = "cmtr-3ba132da-Configuration" ;
        tableName = System.getenv("TABLE_NAME");
        auditTable = dynamoDB.getTable(tableName);
    }

    @Override
    public String handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        for (DynamodbEvent.DynamodbStreamRecord record : dynamodbEvent.getRecords()) {
            LambdaLogger logger = context.getLogger();
            logger.log("tableName" + tableName);

            if (record == null || record.getDynamodb() == null) {
                continue;
            }

            String eventName = record.getEventName();
            Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
            Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
            Map<String, Object> newMap = convertAttributeMap(newImage);
            Map<String, Object> oldMap = convertAttributeMap(oldImage);

            String primaryKey = record.getDynamodb().getKeys().get("id").getS();
            String modificationTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

            logger.log("newImage " + newMap);
            logger.log("oldImage " + oldMap);

            if ("CREATE".equals(eventName)) {
                handleCreatedEvent(primaryKey, newMap, modificationTime);
            } else if ("STORE".equals(eventName)) {
                handleStoredEvent(primaryKey, oldMap, newMap, modificationTime);
            }
        }
        return "Successfully processed " + dynamodbEvent.getRecords().size() + " records." ;

    }

    private void handleStoredEvent(String primaryKey, Map<String, Object> oldImage, Map<String, Object> newImage, String modificationTime) {
        boolean change = oldImage.get("key").toString().equals(newImage.get("key").toString());
        Item auditItem = new Item()
                .withPrimaryKey("id", primaryKey)
                .withString("event", "STORE")
                .withString("modificationTime", modificationTime)
                .withString("newItemKey", newImage.get("key").toString())
                .withString("updatedAttribute", change ? "value" : "key")
                .withString(change ? "oldValue" : "oldKey", change ? oldImage.get("value").toString() : oldImage.get("key").toString())
                .withString(change ? "newValue" : "newKey", change ? newImage.get("value").toString() : newImage.get("key").toString());
        auditTable.putItem(auditItem);
    }

    private void handleCreatedEvent(String primaryKey, Map<String, Object> newImage, String modificationTime) {
        Map<String, Integer> newValue = new HashMap<>();
        newValue.put( newImage.get("key").toString(), Integer.valueOf( newImage.get("value").toString()));
        Item auditItem = new Item()
                .withPrimaryKey("id", primaryKey)
                .withString("event", "CREATE")
                .withString("modificationTime", modificationTime)
                .withString("newItemKey",  newImage.get("key").toString())
                .withMap("newValue", newValue);
        auditTable.putItem(auditItem);
    }

    private Map<String, Object> convertAttributeMap(Map<String, AttributeValue> attributeMap) {
        return attributeMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getS() != null ? entry.getValue().getS()
                                : entry.getValue().getN() != null ? entry.getValue().getN()
                                : entry.getValue().getM() != null ? convertAttributeMap(entry.getValue().getM())
                                : entry.getValue().getBOOL() != null ? entry.getValue().getBOOL()
                                : null
                ));
    }
}
