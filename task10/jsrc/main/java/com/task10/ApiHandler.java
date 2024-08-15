package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;


import java.util.Map;

import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID;

@LambdaHandler(
    lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "REGION", value = "${region}"),
		@EnvironmentVariable(key = "COGNITO_ID", value = "${booking_userpool}", valueTransformer = USER_POOL_NAME_TO_USER_POOL_ID),
		@EnvironmentVariable(key = "CLIENT_ID", value = "${booking_userpool}", valueTransformer = USER_POOL_NAME_TO_CLIENT_ID),
		@EnvironmentVariable(key = "NAME_TABLE_TABLES", value = "${tables_table}"),
		@EnvironmentVariable(key = "NAME_TABLE_RESERVATIONS", value = "${reservations_table}")
})
@DependsOn(resourceType = ResourceType.COGNITO_USER_POOL, name = "${booking_userpool}")

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final Map<String, String> headersForCORS;

	public ApiHandler() {
		this.headersForCORS = initHeadersForCORS();
	}


	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		String resource = request.getResource();
		String httpMethod = request.getHttpMethod();
		APIGatewayProxyResponseEvent response = null;
		LambdaLogger logger = context.getLogger();
		logger.log("resource " + resource);
		logger.log("method " + httpMethod);
		logger.log("function " + context.getFunctionName());

		switch (resource) {
			case "/signup":
				if ("POST".equalsIgnoreCase(httpMethod)) {
					response = new SignupHandler().handleRequest(request, context).withHeaders(headersForCORS);
				}
				break;

			case "/signin":
				if ("POST".equalsIgnoreCase(httpMethod)) {
					response = new SigninHandler().handleRequest(request, context).withHeaders(headersForCORS);
				}
				break;

			case "/tables":
				if ("POST".equalsIgnoreCase(httpMethod)) {
					response = new TablesPostHandler().handleRequest(request, context).withHeaders(headersForCORS);
				} else if ("GET".equalsIgnoreCase(httpMethod)) {
					response = new TablesGetHandler().handleRequest(request, context).withHeaders(headersForCORS);
				}
				break;

			case "/tables/{tableId}":
				if ("GET".equalsIgnoreCase(httpMethod)) {
					response = new TablesGetByIdHandler().handleRequest(request, context).withHeaders(headersForCORS);
				}
				break;

			case "/reservations":
				if ("POST".equalsIgnoreCase(httpMethod)) {
					response = new ReservationsPostHandler().handleRequest(request, context).withHeaders(headersForCORS);
				} else if ("GET".equalsIgnoreCase(httpMethod)) {
					response = new ReservationsGetHandler().handleRequest(request, context).withHeaders(headersForCORS);
				}
				break;

			default:
				response = new APIGatewayProxyResponseEvent()
						.withStatusCode(400)
						.withBody("Invalid resource path or method.");
				break;
		}

		return response != null ? response : new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withBody("Invalid request.");
	}
	private Map<String, String> initHeadersForCORS() {
		return Map.of(
				"Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
				"Access-Control-Allow-Origin", "*",
				"Access-Control-Allow-Methods", "*",
				"Accept-Version", "*"
		);
	}
}
