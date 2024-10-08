package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@LambdaHandler(
        lambdaName = "processor",
        roleName = "processor-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
        tracingMode = TracingMode.Active
)

@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
//@EnvironmentVariable(key = "WEATHER_TABLE", value = "${target_table}")

public class Processor implements RequestHandler<DynamodbEvent, String> { private static final String TABLE_NAME_ENV = "WEATHER_TABLE";
    private final String tableName = "cmtr-3ba132da-Weather-test";
//    private final String tableName = "cmtr-3ba132da-Weather";




    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private final Table table;


    private final com.amazonaws.services.dynamodbv2.document.DynamoDB dynamoDB = new com.amazonaws.services.dynamodbv2.document.DynamoDB(client);

    public Processor() {

        table = dynamoDB.getTable(tableName);
    }

    @Override
    public String handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("events " + dynamodbEvent.getRecords());
        logger.log("function name " + context.getFunctionName());
        logger.log("table name " + tableName);




        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=50.4547&longitude=30.5238&hourly=temperature_2m&timezone=Europe%2FKyiv");

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");

            int responseCode = httpURLConnection.getResponseCode();
            logger.log("response code " + responseCode);
            if (responseCode == 200) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                bufferedReader.lines().forEach(response::append);
                bufferedReader.close();
                saveWeatherDataToDynamoDB(response);
                return String.valueOf(response);
            }
        } catch (IOException e) {

            throw new RuntimeException(e);
        }
        return null;
    }

    private void saveWeatherDataToDynamoDB(StringBuilder weatherData) {
        Item item = new Item().withPrimaryKey("id", UUID.randomUUID()
                .toString()).withJSON("forecast", String.valueOf(weatherData));
        table.putItem(item);
    }

}
