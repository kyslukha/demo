package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.util.regex.Pattern;

public class SignupHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String userPoolId = System.getenv("COGNITO_ID");

    private final CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();

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

            if (!isValidEmail(email) || !isValidPassword(password)) {
                logger.log("invalid email or password");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Invalid email or password format.");
            }

            logger.log("valid email and password");

            software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest adminCreateUserRequest
                    = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .messageAction("SUPPRESS")
                    .userAttributes(
                            AttributeType.builder().name("given_name").value(firstName).build(),
                            AttributeType.builder().name("family_name").value(lastName).build(),
                            AttributeType.builder().name("email").value(email).build()
                    )
                    .temporaryPassword(password)
                    .build();
            logger.log("create adminCreateUserRequest");

            AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(adminCreateUserRequest);

            logger.log("create user response");

            cognitoClient.adminSetUserPassword(builder -> builder
                    .userPoolId(userPoolId)
                    .username(email)
                    .password(password)
                    .permanent(true)
            );
            logger.log("password set");

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
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }
}

