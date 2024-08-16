package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.util.Map;
import java.util.regex.Pattern;

public class SignupHandler extends UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String userPoolId = System.getenv("COGNITO_ID");
    private final String clientId = System.getenv("CLIENT_ID");



    public SignupHandler (CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode body = objectMapper.readTree(request.getBody());

            String firstName = body.get("firstName").asText();
            String lastName = body.get("lastName").asText();
            String email = body.get("email").asText();
            String password = body.get("password").asText();
            LambdaLogger logger = context.getLogger();
            logger.log("email" + email);
            logger.log("password " + password);

            if (!isValidEmail(email)) {
                logger.log("invalid email or password");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Invalid email format.");
            }
            logger.log("valid email");
            if (!isValidPassword(password)) {
                logger.log("invalid email or password");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Invalid password format.");
            }

            logger.log("valid password");

            Map<String, String> user = Map.of("email", email,
                    "password", password,
                    "firstName", firstName,
                    "lastName", lastName);


            String userId = signUp(user)
                    .user().attributes().stream()
                    .filter(attr -> attr.name().equals("sub"))
                    .map(AttributeType::value)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Sub not found."));

            logger.log("userId " + userId);

            confirmSignUp(email,password, logger);
//            logger.log("token " + token)
            logger.log("END");
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Sign-up process is successful");

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.");
        }
    }



    private boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$%^*])[A-Za-z\\d$%^*]{12,}$";
        Pattern pat = Pattern.compile(passwordRegex);
        return pat.matcher(password).matches();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%±]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$";
        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }
}

