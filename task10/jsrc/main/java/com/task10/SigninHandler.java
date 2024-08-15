package com.task10;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;
import java.util.Map;

public class SigninHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String userPoolId = System.getenv("COGNITO_ID");
    private final String clientId = System.getenv("CLIENT_ID");

private final CognitoIdentityProviderClient client = CognitoIdentityProviderClient.create();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode body = objectMapper.readTree(request.getBody());

            String email = body.get("email").asText();
            String password = body.get("password").asText();
            LambdaLogger logger = context.getLogger();
            logger.log("welcome signin");
            logger.log("email " + email);
            logger.log("password " + password);
            logger.log("userPoolId " + userPoolId);

            ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .build();
            ListUsersResponse listUsersResponse = client.listUsers(listUsersRequest);
            logger.log(" list users");
            List<UserType> users = listUsersResponse.users();
            for (UserType user : users) {
                logger.log("user " + user.username());

            }



            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", email,
                            "PASSWORD", password
                    ))
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .build();
            logger.log("has authparam " + authRequest.hasAuthParameters());
            logger.log("uathparam " + authRequest.authParameters().toString());

            logger.log("create authRequest " + authRequest.toString());

            AdminInitiateAuthResponse authResponse = client.adminInitiateAuth(authRequest);

            logger.log("create authResponse " + authResponse.toString());
            String accessToken = client.adminInitiateAuth(authRequest).authenticationResult().accessToken();
            logger.log("create token " + accessToken);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{ \"accessToken\": \"" + accessToken + "\" }");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }
}

