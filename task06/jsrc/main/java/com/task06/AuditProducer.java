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
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
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
@EnvironmentVariable(key = "AUDIT_TABLE",
        value = "${target_table}"
)


public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamoDB = new DynamoDB(client);
    private final Table auditTable;

    private final String AUDIT_TABLE;

    public AuditProducer() {
        AUDIT_TABLE = System.getenv("AUDIT_TABLE");
//        tableName = "Audit";
        auditTable = dynamoDB.getTable(AUDIT_TABLE);
    }

    @Override
    public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("tableName" + AUDIT_TABLE);
        logger.log("context" + context.getFunctionName());
        logger.log("dynamodbEvent" + dynamodbEvent.toString());

        for (DynamodbEvent.DynamodbStreamRecord record : dynamodbEvent.getRecords()) {


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
            logger.log("event "+ record.getEventName());

            logger.log("newMap " + newMap);
            logger.log("oldMap " + oldMap);

            if ("INSERT".equals(eventName)) {
                handleCreatedEvent(primaryKey, newMap, modificationTime);
            } else if ("MODIFY".equals(eventName)) {
                handleStoredEvent(primaryKey, oldMap, newMap, modificationTime);
            }
        }
//        return "Successfully processed " + dynamodbEvent.getRecords().size() + " records." ;
        return null;

    }

    private void handleStoredEvent(String primaryKey, Map<String, Object> oldImage, Map<String, Object> newImage, String modificationTime) {
        boolean change = oldImage.get("key").toString().equals(newImage.get("key").toString());
        Item auditItem = new Item()
                .withPrimaryKey("id", primaryKey)
                .withString("event", "MODIFY")
                .withString("modificationTime", modificationTime)
                .withString("itemKey", newImage.get("key").toString())
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
                .withString("event", "INSERT")
                .withString("modificationTime", modificationTime)
                .withString("itemKey",  newImage.get("key").toString())
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
