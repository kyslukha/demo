package com.task10;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;

import java.util.Map;
import java.util.regex.Pattern;

public class UserHandler {
    private final String userPoolId = System.getenv("COGNITO_ID");
    private final String clientId = System.getenv("CLIENT_ID");
    private final CognitoIdentityProviderClient cognitoClient;

    protected UserHandler(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }


    protected void confirmSignUp(String email, String password) {

        AdminInitiateAuthResponse adminInitiateAuthResponse = signIn(email, password);


        if (!ChallengeNameType.NEW_PASSWORD_REQUIRED.name().equals(adminInitiateAuthResponse.challengeNameAsString())) {
            throw new RuntimeException("unexpected challenge: " + adminInitiateAuthResponse.challengeNameAsString());
        }


        Map<String, String> challengeResponses = Map.of(
                "USERNAME", email,
                "PASSWORD", password,
                "NEW_PASSWORD", password
        );

        AdminRespondToAuthChallengeResponse adminRespondToAuthChallengeResponse = cognitoClient.adminRespondToAuthChallenge(AdminRespondToAuthChallengeRequest.builder()
                .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                .challengeResponses(challengeResponses)
                .userPoolId(userPoolId)
                .clientId(clientId)
                .session(adminInitiateAuthResponse.session())
                .build());
    }

    protected AdminCreateUserResponse signUp(Map<String, String> user) {
        return cognitoClient.adminCreateUser(AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(user.get("email"))
                .temporaryPassword(user.get("password"))
                .userAttributes(
                        AttributeType.builder().name("given_name").value(user.get("firstName")).build(),
                        AttributeType.builder().name("family_name").value(user.get("lastName")).build(),
                        AttributeType.builder().name("email").value(user.get("email")).build(),
                        AttributeType.builder().name("email_verified").value("true").build()
                )
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .messageAction("SUPPRESS")
                .forceAliasCreation(Boolean.FALSE)
                .build());

    }

    protected AdminInitiateAuthResponse signIn(String nickName, String password) {
        Map<String, String> authParams = Map.of(
                "USERNAME", nickName,
                "PASSWORD", password
        );

        return cognitoClient.adminInitiateAuth(AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .authParameters(authParams)
                .userPoolId(userPoolId)
                .clientId(clientId)
                .build());
    }

    protected boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$%^*])[A-Za-z\\d$%^*]{12,}$";
        Pattern pat = Pattern.compile(passwordRegex);
        return pat.matcher(password).matches();
    }

    protected boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }

}
