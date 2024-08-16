package com.task10;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class SigninHandler extends UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String userPoolId = System.getenv("COGNITO_ID");

    private final CognitoIdentityProviderClient client = CognitoIdentityProviderClient.create();

    protected SigninHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }

    @Override
    public APIGatewayProxyResponseEvent  handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode body = objectMapper.readTree(request.getBody());

            String email = body.get("email").asText();
            String password = body.get("password").asText();
            LambdaLogger logger = context.getLogger();
            logger.log("SINGIN email " + email);
            logger.log("SINGIN password " + password);

            String idToken = signIn(email, password).authenticationResult().idToken();

            logger.log("SIGIN token " + idToken);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{\"accessToken\":\"" + idToken + "\"}");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("There was an error in the request, problem token.");
        }
    }


}

