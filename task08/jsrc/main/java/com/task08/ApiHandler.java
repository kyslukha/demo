package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
        layers = {"sdk-layer"}
)

@LambdaLayer(
        layerName = "sdk-layer",
        libraries = {"lib/gson-2.10.1.jar"},
        runtime = DeploymentRuntime.JAVA11
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, String> {

    @Override
    public String handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("request " + request.toString());
        logger.log("function name " + context.getFunctionName());

        Gson gson = new Gson();
        URL url = null;
        try {
            url = new URL("https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");

            int responseCode = httpURLConnection.getResponseCode();
            logger.log("response code " + responseCode);
            if (responseCode == 200) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                bufferedReader.lines().forEach(stringBuilder::append);
                bufferedReader.close();
                return String.valueOf(stringBuilder);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
