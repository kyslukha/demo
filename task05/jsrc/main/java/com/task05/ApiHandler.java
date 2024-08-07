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
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(
		targetTable = "Events",
		batchSize = 10
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private static final String DYNAMODB_TABLE_NAME = "Events";
	private final com.amazonaws.services.dynamodbv2.document.DynamoDB dynamoDB;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApiHandler(DynamoDB dynamoDB) {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		this.dynamoDB = new com.amazonaws.services.dynamodbv2.document.DynamoDB(client);
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
		try {

			Map<String, String> eventData = objectMapper.readValue(apiGatewayProxyRequestEvent.getBody(), Map.class);


			Table table = dynamoDB.getTable(DYNAMODB_TABLE_NAME);

			Item item = new Item()
					.withPrimaryKey("id", eventData.get("id").toString())
					.withString("principalId", eventData.get("principalId").toString())
					.withString("createdAt", eventData.get("createdAt").toString())
					.withString("body", eventData.get("body").toString());


			table.putItem(item);


			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("statusCode", "201");
			responseBody.put("event", eventData);


			return new APIGatewayProxyResponseEvent()
					.withStatusCode(201)
					.withBody(objectMapper.writeValueAsString(responseBody));

		} catch (Exception e) {
			context.getLogger().log("Error saving event: " + e.getMessage());
			return new APIGatewayProxyResponseEvent()
					.withStatusCode(500)
					.withBody("{\"message\": \"Error saving event\"}");
		}
	}
}
