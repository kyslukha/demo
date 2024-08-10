package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
})
public class UuidGenerator implements RequestHandler<Object, String> {

    	private static final String BUCKET_NAME = "uuid-storage";
//    private static final String BUCKET_NAME = "cmtr-3ba132da-uuid-storage";


    public String handleRequest(Object request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("request " + request.toString());

        List<String> uuidList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            uuidList.add(UUID.randomUUID().toString());
        }

        String uuidData = String.join("\n", uuidList);
        logger.log("uuid list " + uuidData);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String fileName = "uuids_" + timestamp + ".txt";
        logger.log("filename " + fileName);

        uploadToS3(uuidData, fileName);

        return "File uploaded: " + fileName;
    }

    private void uploadToS3(String uuidData, String fileName) {
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        InputStream inputStream = new ByteArrayInputStream(uuidData.getBytes(StandardCharsets.UTF_8));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(uuidData.length());

        s3Client.putObject(BUCKET_NAME, fileName, inputStream, metadata);
    }
}
