package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.amazonaws.util.json.Jackson.getObjectMapper;

@LambdaHandler(
        lambdaName = "uuid_generator",
        roleName = "uuid_generator-role",
        isPublishVersion = false,
//	aliasName = "${lambdas_alias_name}",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "eu-central-1"),
        @EnvironmentVariable(key = "notification_bucket", value = "uuid-storage")
//        @EnvironmentVariable(key = "notification_bucket", value = "cmtr-3ba132da-uuid-storage")
})
@RuleEventSource(
        targetRule = "uuid_trigger"
)

public class UuidGenerator implements RequestHandler<Object, String> {

        	private static final String BUCKET_NAME = "uuid-storage";
//        	private static final String BUCKET_NAME = "cmtr-3ba132da-uuid-storage";


    public String handleRequest(Object request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("request " + request.toString());

        List<String> uuidList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            uuidList.add(UUID.randomUUID().toString());
        }

        Map<String, List<String>> fileUUID = new HashMap<>();
        fileUUID.put("ids", uuidList);
        logger.log("fileUUID " + fileUUID.toString());

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String fileName = "uuids_" + timestamp + ".json";
        logger.log("filename " + fileName);

        try {
            String fileContentJson = getObjectMapper().writeValueAsString(fileUUID);
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            s3Client.putObject(BUCKET_NAME, fileName, fileContentJson);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return "File uploaded: " + fileName;
    }
}
